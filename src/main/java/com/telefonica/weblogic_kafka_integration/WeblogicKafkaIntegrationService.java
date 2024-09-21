package com.telefonica.weblogic_kafka_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication(exclude = JmxAutoConfiguration.class)
@EnableJms
public class WeblogicKafkaIntegrationService {

	public static void main(String[] args) {
		SpringApplication.run(WeblogicKafkaIntegrationService.class, args);
	}

}
