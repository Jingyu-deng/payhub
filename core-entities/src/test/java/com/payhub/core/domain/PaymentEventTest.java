package com.payhub.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.event.BaseEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentEventTest {

  @Test
  void shouldImplementEventType() {
    PaymentEvent event = new PaymentEvent();
    assertInstanceOf(BaseEvent.class, event);
  }

  @Test
  void shouldSerializeAndDeserializeViaJsonUtils() {
    Payment payment = new Payment();
    payment.setId("pay-123");
    payment.setOrderId("order-456");
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency(Currency.CNY);
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setNotifyUrl("https://partner.example.com/webhook");

    PaymentEvent event = new PaymentEvent(payment, 1717000000000L);

    String json = com.payhub.core.utils.JsonUtils.toJson(event);
    PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

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

    PaymentEvent event = new PaymentEvent(payment, System.currentTimeMillis());

    String json = com.payhub.core.utils.JsonUtils.toJson(event);
    PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

    assertEquals("pay-min", restored.getPayment().getId());
    assertNull(restored.getPayment().getNotifyUrl());
    assertTrue(restored.getTimestamp() > 0);
  }

  @Test
  void shouldReturnPaymentIdAsKey() {
    Payment payment = new Payment();
    payment.setId("pay-key");
    PaymentEvent event = new PaymentEvent(payment, 0L);

    assertEquals("pay-key", event.key());
  }

  @Test
  void shouldReturnNullKeyWhenPaymentIsNull() {
    PaymentEvent event = new PaymentEvent(null, 0L);

    assertNull(event.key());
  }
}
