package com.payhub.core.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.domain.PaymentStatusResult;
import com.payhub.core.dto.CheckPaymentStatusRequest;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CheckPaymentStatusTemplateTest {

  private AdapterClient adapterClient;
  private DatabaseClient databaseClient;
  private EventPublisher eventPublisher;
  private TestCheckPaymentStatusTemplate template;

  @BeforeEach
  void setUp() {
    adapterClient = mock(AdapterClient.class);
    databaseClient = mock(DatabaseClient.class);
    eventPublisher = mock(EventPublisher.class);

    template = new TestCheckPaymentStatusTemplate();
    template.setAdapterClient(adapterClient);
    template.setDatabaseClient(databaseClient);
    template.setEventPublisher(eventPublisher);
    template.setIdempotencyClient(mock(IdempotencyClient.class));
    template.setSchedulerClient(mock(SchedulerClient.class));
    template.setControlClient(mock(ControlClient.class));
    template.setHttpClient(mock(HttpClient.class));
  }

  @Test
  void shouldPublishEventWhenPaymentCompletes() {
    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setPaymentGateway(PaymentGateway.WECHAT_PAY);
    payment.setTransactionId("txn-123");
    payment.setNotifyUrl("https://partner.example.com/webhook");

    when(databaseClient.findByPaymentId("pay-1")).thenReturn(Optional.of(payment));

    Adapter adapter = mock(Adapter.class);
    when(adapterClient.getAdapter(PaymentGateway.WECHAT_PAY)).thenReturn(adapter);

    PaymentStatusResult result =
        new PaymentStatusResult(PaymentStatus.COMPLETED, "{\"status\":\"SUCCESS\"}");
    when(adapter.checkPaymentStatus("txn-123")).thenReturn(result);

    CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
    request.setPaymentId("pay-1");

    Boolean terminal = template.execute(request);

    assertTrue(terminal);

    ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
    verify(eventPublisher).publish(captor.capture());
    PaymentEvent published = captor.getValue();
    assertEquals(PaymentStatus.COMPLETED, published.getType());
    assertEquals("pay-1", published.getPayment().getId());
    assertEquals(
        "https://partner.example.com/webhook", published.getPayment().getNotifyUrl());
  }

  @Test
  void shouldPublishEventWhenPaymentFails() {
    Payment payment = new Payment();
    payment.setId("pay-2");
    payment.setPaymentGateway(PaymentGateway.ALIPAY);
    payment.setTransactionId("txn-456");

    when(databaseClient.findByPaymentId("pay-2")).thenReturn(Optional.of(payment));

    Adapter adapter = mock(Adapter.class);
    when(adapterClient.getAdapter(PaymentGateway.ALIPAY)).thenReturn(adapter);

    PaymentStatusResult result =
        new PaymentStatusResult(PaymentStatus.FAILED, "{\"status\":\"FAIL\"}");
    when(adapter.checkPaymentStatus("txn-456")).thenReturn(result);

    CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
    request.setPaymentId("pay-2");

    Boolean terminal = template.execute(request);

    assertTrue(terminal);

    ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
    verify(eventPublisher).publish(captor.capture());
    assertEquals(PaymentStatus.FAILED, captor.getValue().getType());
  }

  @Test
  void shouldNotPublishEventWhenPaymentStillProcessing() {
    Payment payment = new Payment();
    payment.setId("pay-3");
    payment.setPaymentGateway(PaymentGateway.WECHAT_PAY);
    payment.setTransactionId("txn-789");

    when(databaseClient.findByPaymentId("pay-3")).thenReturn(Optional.of(payment));

    Adapter adapter = mock(Adapter.class);
    when(adapterClient.getAdapter(PaymentGateway.WECHAT_PAY)).thenReturn(adapter);

    PaymentStatusResult result =
        new PaymentStatusResult(PaymentStatus.PROCESSING, "{}");
    when(adapter.checkPaymentStatus("txn-789")).thenReturn(result);

    CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
    request.setPaymentId("pay-3");

    Boolean terminal = template.execute(request);

    assertFalse(terminal);
    verify(eventPublisher, never()).publish(any());
  }

  /** Minimal concrete subclass for testing the template. */
  static class TestCheckPaymentStatusTemplate extends CheckPaymentStatusTemplate {
    @Override
    protected void validate(CheckPaymentStatusRequest request) {}
  }
}
