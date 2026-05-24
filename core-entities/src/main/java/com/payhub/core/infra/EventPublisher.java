package com.payhub.core.infra;

import com.payhub.core.domain.PaymentEvent;

/**
 * Infrastructure interface for publishing domain events. Implementations live in the infra module
 * (Kafka, RabbitMQ, etc.).
 */
public interface EventPublisher {

  void publish(PaymentEvent event);

  default String topicName() {
    return "payment-events";
  }
}
