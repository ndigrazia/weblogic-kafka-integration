package com.telefonica.weblogic_kafka_integration.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.telefonica.schemas.EventSchema;

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

    @Value("${spring.kafka.security.protocol:#{null}}") 
    private String securityProtocol;

    @Value("${spring.kafka.ssl.protocol:#{null}}") 
    private String sslProtocol;

    @Value("${spring.kafka.ssl.key-password:#{null}}") 
    private String sslKeyPassword;

    @Value("${spring.kafka.ssl.key-store-type:#{null}}")
    private String keyStoreType;

    @Value("${spring.kafka.ssl.key-store-file:#{null}}")
    private String keyStoreLocation;

    @Value("${spring.kafka.ssl.key-store-password:#{null}}")
    private String keyStorePassword;

    @Value("${spring.kafka.ssl.trust-store-type:#{null}}")
    private String trustStoreType;

    @Value("${spring.kafka.ssl.trust-store-file:#{null}}")
    private String trustStoreLocation;

    @Value("${spring.kafka.ssl.trust-store-password:#{null}}")
    private String trustStorePassword;
   
    @Bean
    public ProducerFactory<String, EventSchema> kafkaFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig);

        if(securityProtocol!=null) {
            configProps.put(SslConfigs.SSL_PROTOCOL_CONFIG, sslProtocol);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
            configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
            configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStorePassword);
            configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, trustStoreType);
            configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keyStoreType);
            configProps.put("security.protocol", securityProtocol);
        }

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, EventSchema> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaFactory());
    }

}