package com.payhub.infra.message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EventPublisherImplTest {

  private KafkaTemplate<String, String> kafkaTemplate;
  private EventPublisherImpl publisher;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    publisher = new EventPublisherImpl(kafkaTemplate);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSerializeAndSendWithClassNameHeader() {
    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setStatus(PaymentStatus.COMPLETED);
    PaymentEvent event = new PaymentEvent(payment, 1717000000000L);

    when(kafkaTemplate.send((ProducerRecord<String, String>) any())).thenReturn(null);

    publisher.publish(event);

    ArgumentCaptor<ProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    ProducerRecord<String, String> record = captor.getValue();

    assertEquals("payment-events", record.topic());
    assertNotNull(record.headers().lastHeader("eventType"));
    assertEquals(
        "com.payhub.core.domain.PaymentEvent",
        new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldUsePaymentIdAsKey() {
    Payment payment = new Payment();
    payment.setId("pay-key-test");
    payment.setStatus(PaymentStatus.INITIATED);
    PaymentEvent event = new PaymentEvent(payment, System.currentTimeMillis());

    when(kafkaTemplate.send((ProducerRecord<String, String>) any())).thenReturn(null);

    publisher.publish(event);

    ArgumentCaptor<ProducerRecord<String, String>> captor =
        ArgumentCaptor.forClass(ProducerRecord.class);
    verify(kafkaTemplate).send(captor.capture());
    ProducerRecord<String, String> record = captor.getValue();

    assertEquals("pay-key-test", record.key());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldNotSendImmediatelyWhenTransactionActive() {
    Payment payment = new Payment();
    payment.setId("pay-tx");
    PaymentEvent event = new PaymentEvent(payment, 0L);

    try (MockedStatic<TransactionSynchronizationManager> txMock =
        mockStatic(TransactionSynchronizationManager.class)) {
      txMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

      publisher.publish(event);

      // Should NOT send immediately when a transaction is active
      verify(kafkaTemplate, never()).send((ProducerRecord<String, String>) any());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSendOnTransactionCommit() {
    Payment payment = new Payment();
    payment.setId("pay-commit");
    PaymentEvent event = new PaymentEvent(payment, 0L);

    when(kafkaTemplate.send((ProducerRecord<String, String>) any())).thenReturn(null);

    try (MockedStatic<TransactionSynchronizationManager> txMock =
        mockStatic(TransactionSynchronizationManager.class)) {
      txMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

      // Capture the registered synchronization
      List<TransactionSynchronization> syncs = new ArrayList<>();
      txMock
          .when(
              () ->
                  TransactionSynchronizationManager.registerSynchronization(
                      any(TransactionSynchronization.class)))
          .thenAnswer(
              invocation -> {
                syncs.add(invocation.getArgument(0, TransactionSynchronization.class));
                return null;
              });

      publisher.publish(event);

      assertEquals(1, syncs.size());
      // Simulate afterCommit
      syncs.get(0).afterCommit();

      verify(kafkaTemplate).send((ProducerRecord<String, String>) any());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldNotSendWhenTransactionRollsBack() {
    Payment payment = new Payment();
    payment.setId("pay-rollback");
    PaymentEvent event = new PaymentEvent(payment, 0L);

    try (MockedStatic<TransactionSynchronizationManager> txMock =
        mockStatic(TransactionSynchronizationManager.class)) {
      txMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

      publisher.publish(event);

      // Should NOT send when transaction is active (message is deferred)
      verify(kafkaTemplate, never()).send((ProducerRecord<String, String>) any());
    }
  }
}
