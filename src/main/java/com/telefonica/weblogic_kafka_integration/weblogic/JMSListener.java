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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telefonica.schemas.EventSchema;

@Service
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());
 
    @Autowired
    private KafkaTemplate<String, EventSchema> kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Value(value = "${kafka.topic.name}")
    private String topicName;  

    @Value(value = "${async.mode}")
    private Boolean isAsyncMode;

    @Value(value = "${message.headers}")
    private List<String> acceptedHeaders;
    
    @JmsListener(containerFactory = "factory", destination = "${jms.queue.name}")
    public void listenToMessages(Message msg) throws JMSException {
        EventSchema payload = payload(msg);
        Map<String, String> headers = headers(msg);

        logMessage("MESSAGE RECEIVED", msg.getBody(String.class), headers);

        if (isAsyncMode) {
            sendAsync(topicName, payload, headers);
            return;
        }
    
        sendSync(topicName, payload, headers);
    }

    private void logMessage(String header, String body, Map<String, String> headers) {
        StringBuffer sb = new StringBuffer();

        sb.append("BODY: " + body + "\n");
        sb.append("HEADERS: " + headers + "\n");
    
        logCustomMsg(header, sb.toString());
    }

    private void sendSync(String topic, EventSchema payload, Map<String, String> headers) 
        throws JMSException {
        try {
            kafkaTemplate.send(createKafkaMessage(topic, payload, headers)).get();
            logMessage("MESSAGE SENT TO KAFKA", asJSONString(payload), headers);
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) {
            logCustomErrorMsg("ERROR SENDING MESSAGE TO KAFKA", 
                e.getMessage());
            JMSException jmsException = 
                new JMSException("Error sending message to kafka.");
            jmsException.initCause(e);
            throw jmsException;
        }
    }     

    private org.springframework.messaging.Message<EventSchema> createKafkaMessage(String topic, 
        EventSchema payload, Map<String, String> headers) {
        org.springframework.messaging.Message<EventSchema> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getEvent_id())
                .copyHeaders(headers)
                .build();

        return message;
    }

    private Map<String, String> headers(Message msg) throws JMSException {
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

    private EventSchema payload(Message msg) throws JMSException {
        try {
           return mapper.readValue(msg.getBody(String.class),
            EventSchema.class);
        } catch (JsonProcessingException ex) {
            logCustomErrorMsg("ERROR PARSING JSON MESSAGE", 
                ex.getMessage());
            JMSException jmsException = 
                new JMSException("Error parsing json message.");
            jmsException.initCause(ex);
            throw jmsException;
        }
    }

    private void sendAsync(String topic, EventSchema payload, Map<String, String> headers) 
        throws JMSException {
        ListenableFuture<SendResult<String, EventSchema>> future = 
            kafkaTemplate.send(createKafkaMessage(topic, payload, headers));

        future.addCallback(new ListenableFutureCallback<SendResult<String, EventSchema>>() {
            @Override
            public void onSuccess(SendResult<String, EventSchema> result) {
                try {
                    logMessage("MESSAGE SENT TO KAFKA", asJSONString(payload), headers);
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

    private String  asJSONString(EventSchema e) throws JsonProcessingException {
        return mapper.writeValueAsString(e);
    }
    
}
