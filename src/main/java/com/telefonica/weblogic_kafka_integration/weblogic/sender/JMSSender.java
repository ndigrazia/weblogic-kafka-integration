package com.telefonica.weblogic_kafka_integration.weblogic.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telefonica.weblogic_kafka_integration.model.Event;
import com.telefonica.weblogic_kafka_integration.weblogic.config.JMSApplicationConfig;

public class JMSSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSSender.class.getName());

    private QueueConnectionFactory queueConnectionFactory;
    private QueueSession queueSession;
    private QueueConnection queueConnection;
    private QueueSender queueSender;
    private Queue queue;
    private TextMessage message;

    public void init(Context context, String cf, String queueName) throws NamingException, JMSException {
        queueConnectionFactory = (QueueConnectionFactory) context.lookup(cf);
        queue = (Queue) context.lookup(queueName);
        queueConnection = queueConnectionFactory.createQueueConnection();
        queueSession = queueConnection.createQueueSession(false,Session.AUTO_ACKNOWLEDGE);
        queueSender = queueSession.createSender(queue);
        message = queueSession.createTextMessage();
        queueConnection.start();
    }

    public void send(String msg) throws JMSException {
        message.setText(msg);
        queueSender.send(message);
    }

    public void close() throws JMSException {
        queueSender.close();
        queueSession.close();
        queueConnection.close();
    }

    private static void sendToServer(JMSSender sender, String msg) throws IOException, JMSException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        sender.send(msg);
        bufferedReader.close();
    }

    private static InitialContext getInitialContext(String server) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JMSApplicationConfig.INITIAL_CONTEXT);
        env.put(Context.PROVIDER_URL, server);
        return new InitialContext(env);
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java JMSSender <PROVIDER_URL> <JMS_FACTORY> <QUEUE> <MESSAGE>");
            return;
        }

        String server = args[0];
        String jmsFactory = args[1];
        String queueName = args[2];

        String data = "{\n" +
            "    \"creation_date\": \"" +LocalDateTime.now().toString() +"\",\n" +
            "    \"payload\": {\n" +
            "        \"notification_event_id\": \""+ UUID.randomUUID().toString() +"\"\n" +
            "    },\n" +
            "    \"user_id\": \"string\"\n" +
            "}";
        
        Event event = new Event(UUID.randomUUID().toString(), LocalDateTime.now().toString(), 
            Event.Type.ADD, Event.SubType.USER, "0", data, "ESB");
      
        String message = new ObjectMapper().writeValueAsString(event);   

        if(args.length >= 4)
             message = args[3];

        InitialContext initialContext = getInitialContext(server);
        
        JMSSender sender = new JMSSender();
        sender.init(initialContext, jmsFactory, queueName);
        sendToServer(sender, message);

        LOGGER.info("\nMessage Successfully Sent to the JMS queue!!");
        LOGGER.info("Message: " + message);

        sender.close();
    }

}
