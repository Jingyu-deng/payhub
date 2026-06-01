package com.payhub.core.controls.base;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.event.BaseEvent;
import org.junit.jupiter.api.Test;

class EventControlTest {

  @Test
  void shouldExtendControlInjectorAndDeclareHandledEventType() {
    EventControl<PaymentEvent> control =
        new EventControl<>() {
          @Override
          public Class<? extends BaseEvent> getHandledEventType() {
            return PaymentEvent.class;
          }

          @Override
          public Void execute(PaymentEvent input) {
            return null;
          }
        };

    assertEquals(PaymentEvent.class, control.getHandledEventType());
    assertInstanceOf(ControlInjector.class, control);
    assertInstanceOf(Control.class, control);
    assertNull(control.execute(new PaymentEvent()));
  }
}
