package com.payhub.infra.message;

import com.payhub.core.event.BaseEvent;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class EventPublisherImpl implements EventPublisher {

  private static final String TOPIC = "payment-events";
  private static final String EVENT_TYPE_HEADER = "eventType";

  @SuppressWarnings("unchecked")
  private final KafkaTemplate<String, String> kafkaTemplate;

  @SuppressWarnings("unchecked")
  public EventPublisherImpl(KafkaTemplate<?, ?> kafkaTemplate) {
    this.kafkaTemplate = (KafkaTemplate<String, String>) kafkaTemplate;
  }

  @Override
  public void publish(BaseEvent event) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doPublish(event);
            }

            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) {
                log.info(
                    "Event skipped — transaction rolled back: type={}",
                    event.getClass().getSimpleName());
              }
            }
          });
    } else {
      doPublish(event);
    }
  }

  private void doPublish(BaseEvent event) {
    String json = JsonUtils.toJson(event);
    String eventClassName = event.getClass().getName();

    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, null, event.key(), json);
    record
        .headers()
        .add(new RecordHeader(EVENT_TYPE_HEADER, eventClassName.getBytes(StandardCharsets.UTF_8)));

    kafkaTemplate.send(record);
    log.info("Event published: type={}, key={}", event.getClass().getSimpleName(), event.key());
  }
}
