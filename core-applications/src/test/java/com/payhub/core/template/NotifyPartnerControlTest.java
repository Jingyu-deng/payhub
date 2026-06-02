package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.PartnerNotificationException;
import com.payhub.core.http.DynamicHttpApi;
import com.payhub.core.infra.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotifyPartnerControlTest {

  private HttpClient httpClient;
  private DynamicHttpApi mockApi;
  private NotifyPartnerControl control;

  @BeforeEach
  void setUp() {
    httpClient = mock(HttpClient.class);
    mockApi = mock(DynamicHttpApi.class);
    control = new NotifyPartnerControl();
    control.setHttpClient(httpClient);
  }

  @Test
  void shouldHandlePaymentEvents() {
    assertEquals(PaymentEvent.class, control.getHandledEventType());
  }

  @Test
  void shouldPostToNotifyUrlWhenCompleted() {
    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.COMPLETED);

    PaymentEvent event = new PaymentEvent(payment, 1717000000000L);

    HttpClient.Response response = new HttpClient.Response(200, "OK");
    when(httpClient.createHttpApi(DynamicHttpApi.class, "https://partner.example.com/webhook"))
        .thenReturn(mockApi);
    when(mockApi.post(anyString())).thenReturn(response);

    control.execute(event);

    verify(mockApi).post(anyString());
  }

  @Test
  void shouldSkipWhenNotifyUrlIsNull() {
    Payment payment = new Payment();
    payment.setId("pay-2");
    payment.setNotifyUrl(null);

    PaymentEvent event = new PaymentEvent(payment, 1717000000000L);

    control.execute(event);

    verify(httpClient, never()).createHttpApi(any(), anyString());
  }

  @Test
  void shouldSkipWhenNotifyUrlIsBlank() {
    Payment payment = new Payment();
    payment.setId("pay-3");
    payment.setNotifyUrl("   ");

    PaymentEvent event = new PaymentEvent(payment, 1717000000000L);

    control.execute(event);

    verify(httpClient, never()).createHttpApi(any(), anyString());
  }

  @Test
  void shouldSkipWhenPaymentNotTerminal() {
    Payment payment = new Payment();
    payment.setId("pay-init");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.INITIATED);

    PaymentEvent event = new PaymentEvent(payment, 0L);

    control.execute(event);

    verify(httpClient, never()).createHttpApi(any(), anyString());
  }

  @Test
  void shouldThrowPartnerNotificationExceptionOnServerError() {
    Payment payment = new Payment();
    payment.setId("pay-5xx");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.COMPLETED);

    PaymentEvent event = new PaymentEvent(payment, 0L);

    HttpClient.Response response = new HttpClient.Response(503, "Service Unavailable");
    when(httpClient.createHttpApi(any(), anyString())).thenReturn(mockApi);
    when(mockApi.post(anyString())).thenReturn(response);

    PartnerNotificationException ex =
        assertThrows(PartnerNotificationException.class, () -> control.execute(event));

    assertEquals(503, ex.getStatusCode());
  }

  @Test
  void shouldSkipOnClientError() {
    Payment payment = new Payment();
    payment.setId("pay-4xx");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.COMPLETED);

    PaymentEvent event = new PaymentEvent(payment, 0L);

    HttpClient.Response response = new HttpClient.Response(404, "Not Found");
    when(httpClient.createHttpApi(any(), anyString())).thenReturn(mockApi);
    when(mockApi.post(anyString())).thenReturn(response);

    assertDoesNotThrow(() -> control.execute(event));
  }

  @Test
  void shouldThrowPartnerNotificationExceptionOnNetworkError() {
    Payment payment = new Payment();
    payment.setId("pay-io");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setStatus(PaymentStatus.COMPLETED);

    PaymentEvent event = new PaymentEvent(payment, 0L);

    when(httpClient.createHttpApi(any(), anyString())).thenReturn(mockApi);
    doThrow(new RuntimeException("Connection timeout")).when(mockApi).post(anyString());

    PartnerNotificationException ex =
        assertThrows(PartnerNotificationException.class, () -> control.execute(event));

    assertEquals(0, ex.getStatusCode());
    assertNotNull(ex.getCause());
  }
}
