package com.payhub.infra.message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.event.BaseEvent;
import com.payhub.core.exception.PartnerNotificationException;
import com.payhub.core.infra.ControlClient;
import com.payhub.infra.message.exception.MissingHeaderException;
import com.payhub.infra.message.exception.UnknownEventTypeException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventListenerImplTest {

  private static final String TOPIC = "payment-events";

  private ControlClient controlClient;
  private EventListenerImpl listener;

  @BeforeEach
  void setUp() {
    controlClient = mock(ControlClient.class);
    listener = new EventListenerImpl(controlClient);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> ConsumerRecord<K, V> recordWithHeader(
      String topic, V value, String headerKey, String headerValue) {
    ConsumerRecord<K, V> record = new ConsumerRecord<>(topic, 0, 0L, null, value);
    RecordHeaders headers = new RecordHeaders();
    headers.add(new RecordHeader(headerKey, headerValue.getBytes(StandardCharsets.UTF_8)));
    ConsumerRecord<K, V> spy = spy(record);
    when(spy.headers()).thenReturn(headers);
    return spy;
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeserializeAndDispatchWhenControlMatches() {
    String json =
        "{\"payment\":{\"id\":\"pay-1\",\"orderId\":\"ord-1\",\"status\":\"COMPLETED\"},\"timestamp\":1717000000000}";
    ConsumerRecord<String, String> record =
        recordWithHeader(TOPIC, json, "eventType", "com.payhub.core.domain.PaymentEvent");

    EventControl<BaseEvent> mockControl = mock(EventControl.class);
    when(mockControl.getHandledEventType()).thenReturn((Class) BaseEvent.class);
    when(controlClient.getEventControls()).thenReturn(List.of(mockControl));

    listener.onMessage(record);

    verify(mockControl).execute(any(BaseEvent.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSkipWhenControlDoesNotMatch() {
    String json =
        "{\"payment\":{\"id\":\"pay-2\",\"orderId\":\"ord-2\",\"status\":\"INITIATED\"},\"timestamp\":1717000000000}";
    ConsumerRecord<String, String> record =
        recordWithHeader(TOPIC, json, "eventType", "com.payhub.core.domain.PaymentEvent");

    EventControl<BaseEvent> mockControl = mock(EventControl.class);
    when(mockControl.getHandledEventType()).thenReturn((Class) String.class);
    when(controlClient.getEventControls()).thenReturn(List.of(mockControl));

    listener.onMessage(record);

    verify(mockControl, never()).execute(any());
  }

  @Test
  void shouldHandleNoControls() {
    String json = "{\"payment\":{\"id\":\"pay-3\"},\"timestamp\":1}";
    ConsumerRecord<String, String> record =
        recordWithHeader(TOPIC, json, "eventType", "com.payhub.core.domain.PaymentEvent");

    when(controlClient.getEventControls()).thenReturn(List.of());

    listener.onMessage(record);

    verify(controlClient).getEventControls();
  }

  @Test
  void shouldThrowMissingHeaderExceptionWhenHeaderAbsent() {
    String json = "{\"payment\":{\"id\":\"pay-4\"},\"timestamp\":1}";
    ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 0, 0L, null, json);

    assertThrows(MissingHeaderException.class, () -> listener.onMessage(record));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldThrowUnknownEventTypeExceptionForBadClassName() {
    String json = "{\"payment\":{\"id\":\"pay-5\"},\"timestamp\":1}";
    ConsumerRecord<String, String> record =
        recordWithHeader(TOPIC, json, "eventType", "com.bad.Class");

    assertThrows(UnknownEventTypeException.class, () -> listener.onMessage(record));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldPropagateExceptionFromControlForRetry() {
    String json = "{\"payment\":{\"id\":\"pay-6\"},\"timestamp\":1}";
    ConsumerRecord<String, String> record =
        recordWithHeader(TOPIC, json, "eventType", "com.payhub.core.domain.PaymentEvent");

    EventControl<BaseEvent> mockControl = mock(EventControl.class);
    when(mockControl.getHandledEventType()).thenReturn((Class) BaseEvent.class);
    when(controlClient.getEventControls()).thenReturn(List.of(mockControl));
    doThrow(new PartnerNotificationException("https://partner.example.com/webhook", 503))
        .when(mockControl)
        .execute(any(BaseEvent.class));

    assertThrows(PartnerNotificationException.class, () -> listener.onMessage(record));
  }
}
