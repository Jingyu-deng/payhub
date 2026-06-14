package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.enums.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessPaymentControlTest {

  private ProcessPaymentControl control;

  @BeforeEach
  void setUp() {
    control = new ProcessPaymentControl();
  }

  @Test
  void shouldThrowWhenPaymentIdIsNull() {
    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setOrderId("order-1");
    request.setGatewayName(PaymentGateway.WECHAT_PAY);

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenPaymentIdIsBlank() {
    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("  ");
    request.setOrderId("order-1");
    request.setGatewayName(PaymentGateway.WECHAT_PAY);

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenOrderIdIsNull() {
    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-1");
    request.setGatewayName(PaymentGateway.WECHAT_PAY);

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenOrderIdIsBlank() {
    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-1");
    request.setOrderId("  ");
    request.setGatewayName(PaymentGateway.WECHAT_PAY);

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }

  @Test
  void shouldThrowWhenGatewayNameIsNull() {
    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-1");
    request.setOrderId("order-1");

    assertThrows(IllegalArgumentException.class, () -> control.execute(request));
  }
}
