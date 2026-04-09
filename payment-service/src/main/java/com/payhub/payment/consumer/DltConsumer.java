package com.payhub.payment.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DltConsumer {

  @KafkaListener(
      topics = "order-events.DLT",
      groupId = "payment-group-dlt",
      containerFactory = "dltContainerFactory")
  public void handleDltMessage(String message) {
    log.error("Dead letter message: {}", message);
    // Optionally store in a database for manual inspection
  }
}
