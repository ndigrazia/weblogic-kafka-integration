package com.telefonica.weblogic_kafka_integration.weblogic;

import java.time.LocalDateTime;
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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.support.MessageBuilder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telefonica.weblogic_kafka_integration.kafka.core.MessageDeliveryMode;
import com.telefonica.schemas.EventSchema;

@Service
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());

    private static final String KEY_FILE_NAME = "user_id";
 
    @Autowired
    private KafkaTemplate<String, EventSchema> kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Value(value = "${kafka.topic.name}")
    private String topicName;  

    @Value(value = "${delivery.mode}")
    String deliveryMode;

    @Value(value = "${accepted.headers}")
    List<String> acceptedHeaders;
    
    @Value(value = "${filtered.headers}")
    boolean filteredHeaders;

    @Value("${spring.kafka.partition:#{null}}") 
    private Integer partition;
    
    @Value("${spring.kafka.key.enabled:#{false}}") 
    private boolean keyEnabled;

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

    private String getKey(EventSchema payload) {
        return keyEnabled?payload.getData().get(KEY_FILE_NAME).asText():null;
    }

    private void processMessage(Message msg) throws JMSException {
        final EventSchema payload = parsePayload(msg);
        
        final Map<String, Object> headers = new HashMap<>();
        addHeaders(headers, msg);
    
        logMessage("MESSAGE RECEIVED", payload, headers);

        //If 'partition' is defined and valid, set the partition header
        if(partition!=null)
            headers.put(KafkaHeaders.PARTITION_ID, partition);
        
        sendMessage(payload, headers);
    }
        
    private void sendMessage(EventSchema payload, Map<String, ?> headers) 
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

    private void sendMessageSync(String topic, EventSchema payload, Map<String, ?> headers) 
        throws JMSException {
        try {
            kafkaTemplate.send(createKafkaMessage(topic, payload, headers)).get();
            logMessage("MESSAGE SENT TO KAFKA", payload, headers);
         } catch (InterruptedException | ExecutionException e) {
            JMSException jmsException = 
                new JMSException("ERROR SENDING MESSAGE TO KAFA.");
            jmsException.initCause(e);
            throw jmsException;
        }
    }     

    private org.springframework.messaging.Message<EventSchema> createKafkaMessage(String topic, 
        EventSchema payload, Map<String, ?> headers) {
        final org.springframework.messaging.Message<EventSchema> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, getKey(payload))
                .copyHeaders(headers)
                .build();

        return message;
    }

    void addHeaders(Map<String, Object> headers, Message msg) throws JMSException {
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

    private EventSchema parsePayload(Message msg) throws JMSException {
        try {
           return mapper.readValue(msg.getBody(String.class),
            EventSchema.class);
        } catch (JsonProcessingException ex) {
            JMSException jmsException = 
                new JMSException("ERROR PARSING JSON MESSAGE.");
            jmsException.initCause(ex);
            throw jmsException;
        }
    }

    private void sendMessageAsync(String topic, EventSchema payload, Map<String, ?> headers) {
        ListenableFuture<SendResult<String, EventSchema>> future = 
            kafkaTemplate.send(createKafkaMessage(topic, payload, headers));

        future.addCallback(new ListenableFutureCallback<SendResult<String, EventSchema>>() {
            @Override
            public void onSuccess(SendResult<String, EventSchema> result) {
                logMessage("MESSAGE SENT TO KAFKA", payload, headers);
            }

            @Override
            public void onFailure(Throwable ex) {
                logErrorMsg("ERROR SENDING MESSAGE TO KAFKA", ex.getMessage());
            }
        });
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

    private void logMessage(String message, EventSchema payload, Map<String, ?> headers) {
        try {
            final StringBuffer sb = new StringBuffer();

            sb.append("BODY: " +  mapper.writeValueAsString(payload) + "\n");
            sb.append("HEADERS: " + headers + "\n");
    
            logInfoMsg(message, sb.toString());
        } catch (JsonProcessingException e) {
            logErrorMsg("JSON PARSE ERROR", 
                "Conversion of JSON Body to String failed. The message did not log!");
        }           
    }

    private void handleError(Throwable e) {
        logErrorMsg("ERROR PROCESSING MESSAGE", e.getMessage());        
    }

}
