package com.payhub.infra.message;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.event.BaseEvent;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.utils.JsonUtils;
import com.payhub.infra.message.exception.MissingHeaderException;
import com.payhub.infra.message.exception.UnknownEventTypeException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventListenerImpl {

  private static final String EVENT_TYPE_HEADER = "eventType";
  private static final ConcurrentMap<String, Class<? extends BaseEvent>> CLASS_CACHE =
      new ConcurrentHashMap<>();

  private final ControlClient controlClient;

  public EventListenerImpl(ControlClient controlClient) {
    this.controlClient = controlClient;
  }

  @KafkaListener(topics = "payment-events", groupId = "payhub-consumer")
  public void onMessage(ConsumerRecord<String, String> record) {
    String className = readEventTypeHeader(record);
    BaseEvent event = deserialize(record.value(), className);
    log.info("Event received: type={}", event.getClass().getSimpleName());

    for (EventControl<BaseEvent> control : controlClient.getEventControls()) {
      if (control.getHandledEventType().isAssignableFrom(event.getClass())) {
        control.execute(event);
      }
    }
  }

  private String readEventTypeHeader(ConsumerRecord<String, String> record) {
    Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
    if (header == null) {
      throw new MissingHeaderException(EVENT_TYPE_HEADER);
    }
    return new String(header.value(), StandardCharsets.UTF_8);
  }

  @SuppressWarnings("unchecked")
  private BaseEvent deserialize(String json, String className) {
    Class<? extends BaseEvent> eventClass = CLASS_CACHE.get(className);
    if (eventClass == null) {
      eventClass = resolveClass(className);
      CLASS_CACHE.putIfAbsent(className, eventClass);
    }
    return JsonUtils.fromJson(json, eventClass);
  }

  @SuppressWarnings("unchecked")
  private Class<? extends BaseEvent> resolveClass(String className) {
    try {
      Class<?> clazz = Class.forName(className);
      if (!BaseEvent.class.isAssignableFrom(clazz)) {
        throw new UnknownEventTypeException(className);
      }
      return (Class<? extends BaseEvent>) clazz;
    } catch (ClassNotFoundException e) {
      throw new UnknownEventTypeException(className, e);
    }
  }
}
