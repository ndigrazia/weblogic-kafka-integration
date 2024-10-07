package com.telefonica.weblogic_kafka_integration.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.telefonica.weblogic_kafka_integration.schemas.EventSchema;

@Configuration
public class KafkaConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value = "${max.request.size.config}")
    private String maxRequestSizeConfig;
    
    @Value(value = "${spring.kafka.producer.key-serializer}")
    private String keySerializerClass;
    
    @Value(value = "${spring.kafka.producer.value-serializer}")
    private String valueSerializerClass;

    @Bean
    public ProducerFactory<String, EventSchema> kafkaFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig);

        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, EventSchema> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaFactory());
    }

}