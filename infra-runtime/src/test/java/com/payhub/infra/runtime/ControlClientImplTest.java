package com.payhub.infra.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class ControlClientImplTest {

  private ApplicationContext applicationContext;
  private ControlClientImpl controlClient;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    applicationContext = mock(ApplicationContext.class);
    controlClient =
        new ControlClientImpl(
            mock(AdapterClient.class),
            mock(DatabaseClient.class),
            mock(IdempotencyClient.class),
            mock(EventPublisher.class),
            mock(SchedulerClient.class),
            mock(HttpClient.class),
            applicationContext);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldReturnEventControlsMatchingEventType() {
    EventControl completedControl = mock(EventControl.class);
    when(completedControl.getHandledEventType()).thenReturn(PaymentStatus.COMPLETED);

    EventControl initiatedControl = mock(EventControl.class);
    when(initiatedControl.getHandledEventType()).thenReturn(PaymentStatus.INITIATED);

    Map<String, EventControl> beans = Map.of("c1", completedControl, "c2", initiatedControl);
    when(applicationContext.getBeansOfType(EventControl.class)).thenReturn((Map) beans);

    List<EventControl<PaymentEvent>> result =
        controlClient.getEventControls(PaymentStatus.COMPLETED);

    assertEquals(1, result.size());
    assertEquals(completedControl, result.get(0));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldReturnEmptyListWhenNoMatch() {
    EventControl control = mock(EventControl.class);
    when(control.getHandledEventType()).thenReturn(PaymentStatus.COMPLETED);

    when(applicationContext.getBeansOfType(EventControl.class))
        .thenReturn((Map) Map.of("c1", control));

    List<EventControl<PaymentEvent>> result =
        controlClient.getEventControls(PaymentStatus.INITIATED);

    assertTrue(result.isEmpty());
  }
}
