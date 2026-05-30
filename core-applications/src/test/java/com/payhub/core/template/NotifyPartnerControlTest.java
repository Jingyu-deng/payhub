package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotifyPartnerControlTest {

  private HttpClient httpClient;
  private NotifyPartnerControl control;

  @BeforeEach
  void setUp() {
    httpClient = mock(HttpClient.class);
    control = new NotifyPartnerControl();
    control.setHttpClient(httpClient);
  }

  @Test
  void shouldHandleCompletedEvents() {
    assertEquals(PaymentStatus.COMPLETED, control.getHandledEventType());
  }

  @Test
  void shouldPostToNotifyUrlWhenCompleted() {
    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.COMPLETED);

    PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

    HttpClient.Response response = new HttpClient.Response(200, "OK");
    when(httpClient.post(eq("https://partner.example.com/webhook"), anyMap(), anyString()))
        .thenReturn(response);

    control.execute(event);

    verify(httpClient)
        .post(
            eq("https://partner.example.com/webhook"),
            eq(Map.of("Content-Type", "application/json")),
            anyString());
  }

  @Test
  void shouldSkipWhenNotifyUrlIsNull() {
    Payment payment = new Payment();
    payment.setId("pay-2");
    payment.setNotifyUrl(null);

    PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

    control.execute(event);

    verify(httpClient, never()).post(anyString(), anyMap(), anyString());
  }

  @Test
  void shouldSkipWhenNotifyUrlIsBlank() {
    Payment payment = new Payment();
    payment.setId("pay-3");
    payment.setNotifyUrl("   ");

    PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

    control.execute(event);

    verify(httpClient, never()).post(anyString(), anyMap(), anyString());
  }
}
