package com.telefonica.weblogic_kafka_integration.weblogic.config;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;

@Configuration
public class JMSApplicationConfig {

    public static final String INITIAL_CONTEXT = "weblogic.jndi.WLInitialContextFactory";

    // Url to access to the queue o topic
    @Value("${jms.provider.url}")
    private String providerUrl;
    
    // Name of the queue o topic to extract the message
    @Value("${jms.queue.name}")
    private String destinationName;
    
    // Number of consumers in the application
    @Value("${jms.concurrent.consumers}")
    private String concurrentConsumers;
    
    @Bean
    public DefaultJmsListenerContainerFactory factory(ConnectionFactory connectionFactory, 
        DestinationResolver destination) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(destination);
        factory.setSessionTransacted(true);
        factory.setConcurrency(concurrentConsumers);
        return factory;
    }

    @Bean
    public JndiObjectFactoryBean connectionFactory(JndiTemplate provider){  
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiTemplate(provider);
        factory.setJndiName("javax.jms.QueueConnectionFactory");
        factory.setProxyInterface(ConnectionFactory.class);
        return factory;
    }

    @Bean
    public JndiTemplate provider() {
        Properties env = new Properties();
        env.put("java.naming.factory.initial", INITIAL_CONTEXT);
        env.put("java.naming.provider.url", providerUrl);
        return new JndiTemplate(env);
    }
    
    @Bean
    public JndiDestinationResolver jmsDestinationResolver(JndiTemplate provider){
        JndiDestinationResolver destResolver = new JndiDestinationResolver();
        destResolver.setJndiTemplate(provider);
        return destResolver;
    }
    
    @Bean
    public JndiObjectFactoryBean destination(JndiTemplate provider) {
        JndiObjectFactoryBean dest = new JndiObjectFactoryBean();
        dest.setJndiTemplate(provider);
        dest.setJndiName(destinationName);
        return dest;
    }
    
}
