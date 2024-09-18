package com.telefonica.weblogic_kafka_integration.weblogic;

import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telefonica.weblogic_kafka_integration.model.Event;

@Component
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());
 
    @Autowired
    private KafkaTemplate<String, Event> kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Value(value = "${message.topic.name}")
    private String topicName;  

    @Value(value = "${message.async}")
    private String async;

    @Value(value = "${message.headers}")
    private List<String> acceptedHeaders;
    
    @JmsListener(containerFactory = "factory", destination = "${jms.destination.name}")
    public void listenToMessages(Message msg) throws JMSException {
        Event payload = payload(msg);
        Map<String, Object> headers = headers(msg);

        logMessage(msg.getBody(String.class), headers);

        if (async.equalsIgnoreCase("true")) {
            sendAsync(topicName, payload, headers);
            return;
        }
    
        sendSync(topicName, payload, headers);
    }

    private void logMessage(String body,  Map<String, Object> headers) throws JMSException {
        StringBuffer sb = new StringBuffer();

        sb.append("BODY: " + body + "\n");
        sb.append("HEADERS: " + headers + "\n");
    
        logCustomMsg("MESSAGE RECEIVED", sb.toString());
    }

    private void sendSync(String topic, Event payload, Map<String, Object> headers) throws JMSException {
        try {
            SendResult<String, Event> result = kafkaTemplate.send(
                    createKafkaMessage(topic, payload, headers)).get();
            
            logCustomMsg("MESSAGE SENT TO KAFKA", 
                    asJSONString(result.getProducerRecord().value()));
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) {
            logCustomErrorMsg("ERROR SENDING MESSAGE TO KAFKA", 
                e.getMessage());
            JMSException jmsException = 
                new JMSException("Error sending message to kafka.");
            jmsException.initCause(e);
            throw jmsException;
        }
    }     

    private org.springframework.messaging.Message<Event> createKafkaMessage(String topic, 
        Event payload, Map<String, Object> headers) throws JMSException {
        org.springframework.messaging.Message<Event> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getEvent_id())
                .copyHeaders(headers)
                .build();

        return message;
    }

    private Map<String, Object> headers(Message msg) throws JMSException {
        Map<String, Object> map = new HashMap<>();

        Enumeration<?> propertyNames = msg.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            if (acceptedHeaders.contains(key)) {
                Object value = msg.getObjectProperty(key);
                map.put(key, value);
            }
        }

        return map;
    }

    private Event payload(Message msg) throws JMSException {
        try {
           return mapper.readValue(msg.getBody(String.class),
    Event.class);
        } catch (JsonProcessingException ex) {
            logCustomErrorMsg("ERROR PARSING JSON MESSAGE", 
                ex.getMessage());
            JMSException jmsException = 
                new JMSException("Error parsing json message.");
            jmsException.initCause(ex);
            throw jmsException;
        }
    }

    private void sendAsync(String topic, Event payload, Map<String, Object> headers) throws JMSException {
        ListenableFuture<SendResult<String, Event>> future = 
            kafkaTemplate.send(createKafkaMessage(topic, payload, headers));

        future.addCallback(new ListenableFutureCallback<SendResult<String, Event>>() {
            @Override
            public void onSuccess(SendResult<String, Event> result) {
                try {
                    logCustomMsg("MESSAGE SENT TO KAFKA", 
                        asJSONString(result.getProducerRecord().value()));
                } catch (JsonProcessingException e) {
                    logCustomErrorMsg("ERROR PARSING MESSAGE", 
                        e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable ex) {
                logCustomErrorMsg("ERROR SENDING MESSAGE TO KAFKA", 
                    ex.getMessage());
            }
        });
    }

    private String customMsg(String header, String msg) {
        return header +" AT: " + LocalDateTime.now() 
            + ". MSG: " + msg;
    } 
    
    private void logCustomMsg(String header, String msg) {
        LOGGER.info(customMsg(header, msg));
    }

    private void logCustomErrorMsg(String header, String msg) {
        LOGGER.error(customMsg(header, msg));
    }

    private String  asJSONString(Event e) throws JsonProcessingException {
        return mapper.writeValueAsString(e);
    }
    
}
