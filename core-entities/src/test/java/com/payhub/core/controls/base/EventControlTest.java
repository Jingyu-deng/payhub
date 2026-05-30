package com.payhub.core.controls.base;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

class EventControlTest {

  @Test
  void shouldExtendControlInjectorAndDeclareHandledEventType() {
    EventControl<String> control =
        new EventControl<>() {
          @Override
          public PaymentStatus getHandledEventType() {
            return PaymentStatus.COMPLETED;
          }

          @Override
          public Void execute(String input) {
            return null;
          }
        };

    assertEquals(PaymentStatus.COMPLETED, control.getHandledEventType());
    assertInstanceOf(ControlInjector.class, control);
    assertInstanceOf(Control.class, control);
    assertNull(control.execute("test"));
  }
}
