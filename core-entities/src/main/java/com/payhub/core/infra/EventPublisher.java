package com.payhub.core.infra;

import com.payhub.core.event.BaseEvent;

/**
 * Infrastructure interface for publishing domain events. Implementations live in the infra module
 * (Kafka, RabbitMQ, etc.).
 */
public interface EventPublisher {

  void publish(BaseEvent event);
}
