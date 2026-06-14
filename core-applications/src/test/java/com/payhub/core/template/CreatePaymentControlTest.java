package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.dto.PaymentInitiateRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePaymentControlTest {

  private CreatePaymentControl control;

  @BeforeEach
  void setUp() {
    control = new CreatePaymentControl();
  }

  @Test
  void shouldThrowWhenOrderIdIsNull() {
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setAmount(new BigDecimal("100.00"));

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenOrderIdIsBlank() {
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("   ");
    request.setAmount(new BigDecimal("100.00"));

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenAmountIsNull() {
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-1");

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenAmountIsZero() {
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-1");
    request.setAmount(BigDecimal.ZERO);

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenAmountIsNegative() {
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-1");
    request.setAmount(new BigDecimal("-1.00"));

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }
}
