package com.telefonica.weblogic_kafka_integration.weblogic;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.telefonica.schemas.event.jms.JmsCloudEvent;

import com.telefonica.weblogic_kafka_integration.kafka.core.MessageDeliveryMode;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;

@Service
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());
 
    @Autowired
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper().registerModule(JsonFormat.getCloudEventJacksonModule());

    @Value(value = "${kafka.topic.name}")
    private String topicName;  

    @Value(value = "${delivery.mode}")
    String deliveryMode;

    @Value(value = "${accepted.headers}")
    List<String> acceptedHeaders;
    
    @Value(value = "${filtered.headers}")
    boolean filteredHeaders;

    @Value("${spring.kafka.partition:#{null}}") 
    private String partition;

    @PostConstruct
    public void init() {
        if(MessageDeliveryMode.valueOf(deliveryMode).equals(MessageDeliveryMode.NONE))
            LOGGER.info("There is no Delivery Mode. Notifications will not be sent!");
        
        if(!filteredHeaders)
            LOGGER.info("All headers will be sent");
    }

    @JmsListener(containerFactory = "factory", destination = "${jms.queue.name}")
    public void listenToMessages(Message msg) throws JMSException {
        try {
            processMessage(msg);
        } catch (Exception e) {
            handleError(e);
            throw e;
        }
    }

    private void processMessage(Message msg) throws JMSException {
        final JmsCloudEvent payload = parsePayload(msg);
        
        final Map<String, String> headers = new HashMap<>();
        addHeaders(headers, msg);
        
        logMessage("MESSAGE RECEIVED", payload, headers);

        sendMessage(payload, headers);
    }
        
    private void sendMessage(JmsCloudEvent payload, Map<String, String> headers) 
        throws JMSException {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Delibery Mode: " + deliveryMode); 
        
        switch (MessageDeliveryMode.valueOf(deliveryMode)) {
            case NONE: 
                break;
            case SYNC:
                sendMessageSync(topicName, payload, headers);
                break;
            case ASYNC:
                sendMessageAsync(topicName, payload, headers);
                break;
            default:
                LOGGER.info("Delivery Mode is not valid. Use NONE, SYNC, ASYNC.");
                break;
        }
    }

    private void sendMessageSync(String topic, JmsCloudEvent payload, Map<String, String> headers) 
        throws JMSException {
        try {
            CloudEvent ce = buildCloudEvent(payload, headers);
            kafkaTemplate.send(topic, getPartition(), null, ce).get();
            logMessage("MESSAGE SENT TO KAFKA", ce, null);
         } catch (InterruptedException | ExecutionException | JsonProcessingException  e) {
            JMSException jmsException = 
                new JMSException("ERROR SENDING MESSAGE TO KAFA.");
            jmsException.initCause(e);
            throw jmsException;
        }
    }     

    private Integer getPartition() {
        return partition==null?null:Integer.parseInt(partition);
    }

    private CloudEvent buildCloudEvent(JmsCloudEvent payload, Map<String, String> headers) 
        throws JsonProcessingException {

        final CloudEventBuilder ceb = CloudEventBuilder.v1()
                .withId(payload.getId())
                .withSource(URI.create(payload.getSource()))
                .withDataContentType(payload.getDataContentType())  
                .withTime(OffsetDateTime.parse(payload.getTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .withType(payload.getType())
                .withData(mapper.writeValueAsBytes(payload.getData()));
      
        headers.forEach(ceb::withExtension);

        return ceb.build();
    }

    void addHeaders(Map<String, String> headers, Message msg) throws JMSException {
        Enumeration<?> propertyNames = msg.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            
            // Filtrar los encabezados si es necesario
            if (!filteredHeaders || acceptedHeaders.contains(key)) {
                Object value = msg.getObjectProperty(key);

                // Añadir el valor al mapa si no es nulo
                if (value != null) {
                    headers.put(key, value.toString());
                }
            }       
        }
    }

    private JmsCloudEvent parsePayload(Message msg) throws JMSException {
        try {
           return mapper.readValue(msg.getBody(String.class),
            JmsCloudEvent.class);
        } catch (JsonProcessingException ex) {
            JMSException jmsException = 
                new JMSException("ERROR PARSING JSON MESSAGE.");
            jmsException.initCause(ex);
            throw jmsException;
        }
    }

    private void sendMessageAsync(String topic, JmsCloudEvent payload, Map<String, String> headers)
        throws JMSException {
            try {
                CloudEvent ce = buildCloudEvent(payload, headers);
                kafkaTemplate.send(topic, getPartition(), null, ce)
                    .addCallback(new ListenableFutureCallback<SendResult<String, CloudEvent>>() {
                        @Override
                        public void onSuccess(SendResult<String, CloudEvent> result) {
                            logMessage("MESSAGE SENT TO KAFKA", ce, null);
                        }
            
                        @Override
                        public void onFailure(Throwable ex) {
                            logErrorMsg("ERROR SENDING MESSAGE TO KAFKA", ex.getMessage());
                        }
                    });
            } catch (JsonProcessingException e) {
                JMSException jmsException = 
                new JMSException("ERROR SENDING MESSAGE TO KAFA.");
                jmsException.initCause(e);
                throw jmsException;
            }
    }

    private String customMsg(String header, String msg) {
        return header +" AT: " + LocalDateTime.now() 
            + ". MSG: " + msg;
    } 
    
    private void logInfoMsg(String header, String msg) {
        LOGGER.info(customMsg(header, msg));
    }

    private void logErrorMsg(String header, String msg) {
        LOGGER.error(customMsg(header, msg));
    }

    private void logMessage(String header, Object event, Map<String, String> headers) {
        try {
            StringBuffer sb = new StringBuffer();
            
            sb.append("BODY: " + mapper.writeValueAsString(event) + "\n");

            if(event instanceof JmsCloudEvent) {
                sb.append("HEADERS: " + headers + "\n");
            }
           
            logInfoMsg(header, sb.toString());
        } catch (JsonProcessingException e) {
            logErrorMsg("JSON PARSE ERROR",  
                "Conversion of JSON Body to String failed. The message did not log!");
        }
    }

    private void handleError(Throwable e) {
        logErrorMsg("ERROR PROCESSING MESSAGE", e.getMessage());        
    }

}
