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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JMSListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSListener.class.getName());
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${message.topic.name}")
    private String topicName;  
    
    @JmsListener(containerFactory = "factory", destination = "${jms.destination.name}")
    public void listenToMessages(Message msg) throws JMSException {
        logCustomMsg("MESSAGE RECEIVED", msg.getBody(String.class));
        
        kafkaTemplate.send(topicName, msg.getBody(String.class));

        logCustomMsg("MESSAGE SENT TO KAFKA", msg.getBody(String.class));
    }

    private void logCustomMsg(String header, String msg) throws JMSException {
        String text = header +" AT: " + LocalDateTime.now() 
            + ". MSG: " + msg;
       LOGGER.info(text);
    }
    
}
