package com.telefonica.weblogic_kafka_integration.weblogic;

import java.time.LocalDateTime;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
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
    
    @JmsListener(containerFactory = "factory", destination = "${jms.destination.name}")
    public void listenToMessages(Message msg) throws JMSException {
       try {
            String json = msg.getBody(String.class);

            logCustomMsg("MESSAGE RECEIVED", json, false);

            Event event = mapper.readValue(json, Event.class);
        
            if (async.equalsIgnoreCase("true")) {
                sendAsync(topicName, event);
                return;
            }
        
            sendSync(topicName, event);
        } catch (JsonProcessingException ex) {
           logCustomMsg("ERROR PARSING JSON MESSAGE", 
               ex.getMessage(), true);
           JMSException jmsException = 
               new JMSException("Error parsing json message.");
           jmsException.initCause(ex);
           throw jmsException;
       }
    }

    private void sendSync(String topic, Event event) throws JMSException {
        try {
            SendResult<String, Event> result = kafkaTemplate.send(topic, event).get();
            logCustomMsg("MESSAGE SENT TO KAFKA", 
                asJSONString(result.getProducerRecord().value()), false);
        } catch (Exception ex) {
            logCustomMsg("ERROR SENDING MESSAGE TO KAFKA", 
                ex.getMessage(), true);
            JMSException jmsException = 
                new JMSException("Error sending message to kafka.");
            jmsException.initCause(ex);
            throw jmsException;
        }
    }     

    private void sendAsync(String topic, Event event) {
        ListenableFuture<SendResult<String, Event>> future = kafkaTemplate.send(topic, event);

        future.addCallback(new ListenableFutureCallback<SendResult<String, Event>>() {
            @Override
            public void onSuccess(SendResult<String, Event> result) {
                try {
                    logCustomMsg("MESSAGE SENT TO KAFKA", 
                        asJSONString(result.getProducerRecord().value()), 
                            false);
                } catch (JsonProcessingException e) {
                    logCustomMsg("ERROR PARSING MESSAGE", 
                        e.getMessage(), true);
                }
            }

            @Override
            public void onFailure(Throwable ex) {
                logCustomMsg("ERROR SENDING MESSAGE TO KAFKA", 
                    ex.getMessage(), true);
            }
        });
    }

    private void logCustomMsg(String header, String msg, boolean error) {
        String text = header +" AT: " + LocalDateTime.now() 
            + ". MSG: " + msg;
        
        if (error){ 
            LOGGER.error(text);
            return;
        } 

        LOGGER.info(text);
    }
    
    private String  asJSONString(Event e) throws JsonProcessingException {
        return mapper.writeValueAsString(e);
    }
    
}
