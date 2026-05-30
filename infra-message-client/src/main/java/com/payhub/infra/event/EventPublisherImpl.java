package com.payhub.infra.event;

import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.infra.EncryptionClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventPublisherImpl implements EventPublisher {

  private static final String TOPIC = "payment-events";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final EncryptionClient encryptionClient;

  public EventPublisherImpl(
      KafkaTemplate<String, String> kafkaTemplate, EncryptionClient encryptionClient) {
    this.kafkaTemplate = kafkaTemplate;
    this.encryptionClient = encryptionClient;
  }

  @Override
  public void publish(PaymentEvent event) {
    String json = JsonUtils.toJson(event);
    String encrypted = encryptionClient.encrypt(json);
    String key = event.getPayment().getId();
    kafkaTemplate.send(TOPIC, key, encrypted);
    log.info("Event published: type={}, paymentId={}", event.getType(), event.getPayment().getId());
  }
}
