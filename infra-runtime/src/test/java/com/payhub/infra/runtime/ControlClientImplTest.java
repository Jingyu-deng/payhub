package com.payhub.infra.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.event.BaseEvent;
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
  void shouldReturnAllEventControls() {
    EventControl control1 = mock(EventControl.class);
    EventControl control2 = mock(EventControl.class);

    Map<String, EventControl> beans = Map.of("c1", control1, "c2", control2);
    when(applicationContext.getBeansOfType(EventControl.class)).thenReturn((Map) beans);

    List<EventControl<BaseEvent>> result = controlClient.getEventControls();

    assertEquals(2, result.size());
    assertTrue(result.contains(control1));
    assertTrue(result.contains(control2));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void shouldReturnEmptyListWhenNoControlsRegistered() {
    when(applicationContext.getBeansOfType(EventControl.class)).thenReturn(Map.of());

    List<EventControl<BaseEvent>> result = controlClient.getEventControls();

    assertTrue(result.isEmpty());
  }
}
