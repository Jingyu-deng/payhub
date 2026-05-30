package com.payhub.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentEventTest {

  @Test
  void shouldSerializeAndDeserializeViaJsonUtils() {
    Payment payment = new Payment();
    payment.setId("pay-123");
    payment.setOrderId("order-456");
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency(Currency.CNY);
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setNotifyUrl("https://partner.example.com/webhook");

    PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

    String json = com.payhub.core.utils.JsonUtils.toJson(event);
    PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

    assertEquals(PaymentStatus.COMPLETED, restored.getType());
    assertEquals("pay-123", restored.getPayment().getId());
    assertEquals("order-456", restored.getPayment().getOrderId());
    assertEquals("https://partner.example.com/webhook", restored.getPayment().getNotifyUrl());
    assertEquals(1717000000000L, restored.getTimestamp());
  }

  @Test
  void shouldSerializeAndDeserializeMinimalEvent() {
    Payment payment = new Payment();
    payment.setId("pay-min");
    payment.setOrderId("order-min");
    payment.setStatus(PaymentStatus.INITIATED);

    PaymentEvent event =
        new PaymentEvent(PaymentStatus.INITIATED, payment, System.currentTimeMillis());

    String json = com.payhub.core.utils.JsonUtils.toJson(event);
    PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

    assertEquals(PaymentStatus.INITIATED, restored.getType());
    assertEquals("pay-min", restored.getPayment().getId());
    assertNull(restored.getPayment().getNotifyUrl());
    assertTrue(restored.getTimestamp() > 0);
  }
}
