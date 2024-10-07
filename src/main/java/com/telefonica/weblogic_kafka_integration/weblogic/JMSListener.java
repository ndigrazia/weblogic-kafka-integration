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
import com.telefonica.weblogic_kafka_integration.schemas.EventSchema;

@Service
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());
 
    @Autowired
    private KafkaTemplate<String, EventSchema> kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Value(value = "${kafka.topic.name}")
    private String topicName;  

    @Value(value = "${delivery.mode}")
    private String deliveryMode;

    @Value(value = "${message.headers}")
    private List<String> acceptedHeaders;

    @PostConstruct
    public void init() {
        if(MessageDeliveryMode.valueOf(deliveryMode).equals(MessageDeliveryMode.NONE))
            LOGGER.info("There is no Delivery Mode. Notifications will not be sent!");
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

    public void processMessage(Message msg) throws JMSException {
        final EventSchema payload = parsePayload(msg);
        final Map<String, String> headers = extractHeaders(msg);
    
        logMessage("MESSAGE RECEIVED", payload, headers);
                
        sendMessage(payload, headers);
    }

    private void sendMessage(EventSchema payload, Map<String, String> headers) 
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

    private void sendMessageSync(String topic, EventSchema payload, Map<String, String> headers) 
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
        EventSchema payload, Map<String, String> headers) {
        final org.springframework.messaging.Message<EventSchema> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getEventId())
                .copyHeaders(headers)
                .build();

        return message;
    }

    private Map<String, String> extractHeaders(Message msg) throws JMSException {
        Map<String, String> map = new HashMap<String, String>();

        Enumeration<?> propertyNames = msg.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            if (acceptedHeaders.contains(key)) {
                Object value = msg.getObjectProperty(key);
                map.put(key, value.toString());
            }
        }

        return map;
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

    private void sendMessageAsync(String topic, EventSchema payload, Map<String, String> headers) {
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

    private void logMessage(String message, EventSchema payload, Map<String, String> headers) {
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
