package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.dto.CheckPaymentStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckPaymentStatusControlTest {

  private CheckPaymentStatusControl control;

  @BeforeEach
  void setUp() {
    control = new CheckPaymentStatusControl();
  }

  @Test
  void shouldThrowWhenPaymentIdIsNull() {
    CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenPaymentIdIsBlank() {
    CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
    request.setPaymentId("  ");

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }
}
