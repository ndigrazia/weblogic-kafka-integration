package com.telefonica.weblogic_kafka_integration.weblogic.sender;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telefonica.schemas.EventSchema;
import com.telefonica.weblogic_kafka_integration.weblogic.config.JMSApplicationConfig;

public class JMSSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSSender.class.getName());

    private static final String ENRICHMENT_MODE = "EnrichmentEventContext";
    private static final String DELAYED_MODE = "EventDeliveryMode";

    private QueueConnectionFactory queueConnectionFactory;
    private QueueSession queueSession;
    private QueueConnection queueConnection;
    private QueueSender queueSender;
    private Queue queue;
    private TextMessage message;

    public void init(Context context, String cf, String queueName) 
        throws NamingException, JMSException {
        queueConnectionFactory = (QueueConnectionFactory) context.lookup(cf);
        queue = (Queue) context.lookup(queueName);
        queueConnection = queueConnectionFactory.createQueueConnection();
        queueSession = queueConnection.createQueueSession(false,
            Session.AUTO_ACKNOWLEDGE);
        queueSender = queueSession.createSender(queue);
        message = queueSession.createTextMessage();
        queueConnection.start();
    }

    public TextMessage send(String msg, boolean withHeaders) throws JMSException {
        if (withHeaders) {
            message.setStringProperty(ENRICHMENT_MODE, "EventType");
            message.setStringProperty(DELAYED_MODE, "Delayed");
        } 
           
        message.setText(msg);

        queueSender.send(message);

        return message;
    }

    public void close() throws JMSException {
        queueSender.close();
        queueSession.close();
        queueConnection.close();
    }

    private static TextMessage sendToServer(JMSSender sender, String msg,
        boolean withHeaders) throws IOException, JMSException {
        return sender.send(msg, withHeaders);
    }

    private static InitialContext getInitialContext(String server) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, JMSApplicationConfig.INITIAL_CONTEXT);
        env.put(Context.PROVIDER_URL, server);

        return new InitialContext(env);
    }

    public static void main(String args[]) {
        if (args.length < 3) {
            System.out.println("Usage: java JMSSender <PROVIDER_URL> " + 
                "<JMS_FACTORY> <QUEUE> <WITH_HEADERS> <MESSAGE>");
            return;
        }

        String server = args[0];
        String jmsFactory = args[1];
        String queueName = args[2];
        boolean withHeaders = Boolean.parseBoolean(args[3]);
        
        final String payload;

        if(args.length >= 5)
            payload = args[4];
        else {
            try {
                payload = new ObjectMapper().writeValueAsString(
                    createASampleEvent());
            } catch (JsonProcessingException e) {
                LOGGER.error("PARSING ERROR:", e);
                return;
            }  
        }

        JMSSender sender = null;

        try {
            sender = new JMSSender();
        
            sender.init(getInitialContext(server), jmsFactory, queueName);

            TextMessage message = sendToServer(sender, payload, withHeaders);

            Map<String, String> headers = null;
            if(withHeaders) {
                headers = new HashMap<String, String>();

                Enumeration<?> props = message.getPropertyNames();

                while (props.hasMoreElements()) {
                    String key = (String) props.nextElement();
                    String value = message.getObjectProperty(key).toString();
                    headers.put(key, value);
                }
            }
                     
            LOGGER.info("\nMessage Successfully Sent to the JMS queue!!\n MESSAGE: " +
                 message.getBody(String.class) +  "\n HEADERS: " +(headers == null ? "empty" : headers) + "\n");
        } catch (Exception e) {
            LOGGER.error("\nError sending message to the JMS queue!!");
        } finally {
            if(sender != null)
                try {
                    sender.close();
                } catch (JMSException e) {
                    LOGGER.error("CLOSE CONNECTION ERROR: ", e);
                    return;
                }
        }
    }

    private static EventSchema createASampleEvent() {
        final String now = LocalDateTime.now().toString();
        final String uuid = UUID.randomUUID().toString();

        final String payload = "{\n" +
            "    \"creation_date\": \"" + now +"\",\n" +
            "    \"payload\": {\n" +
            "        \"notification_event_id\": \""+ uuid +"\"\n" +
            "    },\n" +
            "    \"user_id\": \"string\"\n" +
            "}";
        
        return new EventSchema(uuid, now, EventSchema.Type.ADD, 
            EventSchema.SubType.USER, "0", payload, "ESB");
    }

}
