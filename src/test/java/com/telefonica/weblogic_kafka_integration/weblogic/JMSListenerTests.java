package com.telefonica.weblogic_kafka_integration.weblogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.concurrent.ListenableFuture;

import com.telefonica.weblogic_kafka_integration.kafka.core.MessageDeliveryMode;
import com.telefonica.schemas.EventSchema;
import com.telefonica.weblogic_kafka_integration.weblogic.util.Util;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SpringBootTest
public class JMSListenerTests {

    @InjectMocks
    private JMSListener jmsListener;

    @Mock
    private Message jmsMessage;

    @Mock
    private KafkaTemplate<String, EventSchema> kafkaTemplate;

    @Value(value = "${accepted.headers}") 
    private List<String> acceptedHeaders;

    @Mock
    private ListenableFuture<SendResult<String, EventSchema>> listenableFuture;
     
	@BeforeEach
    public void setup() throws JMSException {
        jmsListener.acceptedHeaders = acceptedHeaders;

        when(jmsMessage.getObjectProperty("EnrichmentEventContext")).thenReturn("EventType");
        when(jmsMessage.getObjectProperty("EventDeliveryMode")).thenReturn("Delayed");
        when(jmsMessage.getPropertyNames()).thenReturn(new EnumerationMock());
    }

    @Test
    public void testListenToMessagesSync() throws JMSException, InterruptedException, ExecutionException {
        when(jmsMessage.getBody(String.class)).thenReturn(Util.createASampleEvent());
    	when(kafkaTemplate.send(Mockito.any(org.springframework.messaging.Message.class)))
                .thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(mock(SendResult.class));

        jmsListener.deliveryMode = MessageDeliveryMode.SYNC.name();

        jmsListener.listenToMessages(jmsMessage);

        verify(kafkaTemplate, times(1)).send(
                Mockito.any(org.springframework.messaging.Message.class));
    }

    @Test
    public void testListenToMessagesNone() throws JMSException, InterruptedException, ExecutionException {
        when(jmsMessage.getBody(String.class)).thenReturn(Util.createASampleEvent());
    	
        jmsListener.deliveryMode = MessageDeliveryMode.NONE.name();

        jmsListener.listenToMessages(jmsMessage);

        verify(kafkaTemplate, times(0)).send(
                Mockito.any(org.springframework.messaging.Message.class));
    }

    @Test
    public void testListenToMessagesSyncWithException() throws JMSException, InterruptedException, ExecutionException {
        when(jmsMessage.getBody(String.class)).thenReturn(Util.createASampleEvent());
    	when(kafkaTemplate.send(Mockito.any(org.springframework.messaging.Message.class)))
                .thenReturn(listenableFuture);
        when(listenableFuture.get()).thenThrow(new InterruptedException("Error sending message"));

        jmsListener.deliveryMode = MessageDeliveryMode.SYNC.name();

        assertThrows(JMSException.class, () -> jmsListener.listenToMessages(jmsMessage));

        verify(kafkaTemplate, times(1)).send(
                Mockito.any(org.springframework.messaging.Message.class));
    }

    @Test
    public void testListenToMessagesAsync() throws JMSException, InterruptedException, ExecutionException {
        when(jmsMessage.getBody(String.class)).thenReturn(Util.createASampleEvent());
    	when(kafkaTemplate.send(Mockito.any(org.springframework.messaging.Message.class)))
                .thenReturn(listenableFuture);

        jmsListener.deliveryMode = MessageDeliveryMode.ASYNC.name();

        jmsListener.listenToMessages(jmsMessage);

        verify(kafkaTemplate, times(1)).send(
                Mockito.any(org.springframework.messaging.Message.class));
    }

   
    @Test
    public void testGetAcceptedHeaders() throws JMSException {
        jmsListener.filteredHeaders=true;
        final Map<String, Object> headers = new HashMap<>();
        jmsListener.addHeaders(headers, jmsMessage);
        assertEquals(2, headers.size());
        assertEquals("EventType", headers.get("EnrichmentEventContext"));
        assertEquals("Delayed", headers.get("EventDeliveryMode"));
    }

    @Test
    public void testGetAllHeadersWithError() throws JMSException {
        jmsListener.filteredHeaders=false;
        when(jmsMessage.getObjectProperty("Header1")).thenReturn("Value1");

        final Map<String, Object> headers = new HashMap<>();
        jmsListener.addHeaders(headers, jmsMessage);

        assertNotEquals(2, headers.size());
    }

    @Test
    public void testGetAllHeaders() throws JMSException {
        jmsListener.filteredHeaders=false;
       
        when(jmsMessage.getObjectProperty("Header1")).thenReturn("Value1");
       
        final Map<String, Object> headers = new HashMap<>();
        jmsListener.addHeaders(headers, jmsMessage);

        assertEquals(3, headers.size());
        assertEquals("EventType", headers.get("EnrichmentEventContext"));
        assertEquals("Delayed", headers.get("EventDeliveryMode"));
        assertEquals("Value1", headers.get("Header1"));
    }

    private static class EnumerationMock implements java.util.Enumeration<String> {
        private final String[] elements = { "EnrichmentEventContext", "EventDeliveryMode",
             "Header1"};

        private int index = 0;

        @Override
        public boolean hasMoreElements() {
            return index < elements.length;
        }

        @Override
        public String nextElement() {
            return elements[index++];
        }
    }

}
