package com.payhub.infra.event;

import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.EncryptionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class EventPublisherImplTest {

  private KafkaTemplate<String, String> kafkaTemplate;
  private EncryptionClient encryptionClient;
  private EventPublisherImpl publisher;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    encryptionClient = mock(EncryptionClient.class);
    publisher = new EventPublisherImpl(kafkaTemplate, encryptionClient);
  }

  @Test
  void shouldEncryptThenSend() {
    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setStatus(PaymentStatus.COMPLETED);
    PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

    when(encryptionClient.encrypt(anyString())).thenReturn("encrypted-payload");
    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

    publisher.publish(event);

    verify(encryptionClient).encrypt(anyString());
    verify(kafkaTemplate).send("payment-events", "pay-1", "encrypted-payload");
  }
}
