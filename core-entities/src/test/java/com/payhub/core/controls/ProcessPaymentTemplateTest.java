package com.payhub.core.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.DuplicatePaymentException;
import com.payhub.core.exception.LockAcquisitionException;
import com.payhub.core.infra.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessPaymentTemplateTest {

  private IdempotencyClient idempotencyClient;
  private DatabaseClient databaseClient;
  private EventPublisher eventPublisher;
  private AdapterClient adapterClient;
  private Adapter adapter;
  private ProcessPaymentTemplateWithHook template;

  @BeforeEach
  void setUp() {
    idempotencyClient = mock(IdempotencyClient.class);
    databaseClient = mock(DatabaseClient.class);
    eventPublisher = mock(EventPublisher.class);
    adapterClient = mock(AdapterClient.class);
    adapter = mock(Adapter.class);

    template = new ProcessPaymentTemplateWithHook();
    template.setIdempotencyClient(idempotencyClient);
    template.setDatabaseClient(databaseClient);
    template.setEventPublisher(eventPublisher);
    template.setAdapterClient(adapterClient);
    template.setSchedulerClient(mock(SchedulerClient.class));
    template.setControlClient(mock(ControlClient.class));
    template.setHttpClient(mock(HttpClient.class));
  }

  @Test
  void shouldProcessPaymentSuccessfullyAndPublishEvent() {
    when(idempotencyClient.acquireLock("lock:payment:process:pay-1", 5, 10)).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed("payment:processed:pay-1")).thenReturn(false);

    Payment payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency(Currency.USD);
    when(databaseClient.findByOrderId("order-1")).thenReturn(Optional.of(payment));

    when(adapterClient.getAdapter(PaymentGateway.WECHAT_PAY)).thenReturn(adapter);
    when(adapter.processPayment(eq("order-1"), any(), any(), any()))
        .thenReturn(
            new PaymentResult(true, "txn-123", "OK", "{\"ok\":true}", "https://pay.example.com"));

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-1");
    request.setOrderId("order-1");
    request.setGatewayName(PaymentGateway.WECHAT_PAY);

    ProcessPaymentResponse response = template.execute(request);

    assertNotNull(response);
    assertEquals(PaymentStatus.PROCESSING, response.getStatus());
    assertEquals("txn-123", payment.getTransactionId());
    assertEquals(PaymentGateway.WECHAT_PAY, payment.getPaymentGateway());

    verify(eventPublisher).publish(any(PaymentEvent.class));
    verify(idempotencyClient).markAsProcessed("payment:processed:pay-1");
    verify(databaseClient).save(payment);
    verify(idempotencyClient).releaseLock("lock:payment:process:pay-1");
    assertTrue(template.hookCalled);
  }

  @Test
  void shouldMarkPaymentFailedWhenAdapterReturnsFailure() {
    when(idempotencyClient.acquireLock("lock:payment:process:pay-2", 5, 10)).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed("payment:processed:pay-2")).thenReturn(false);

    Payment payment = new Payment();
    payment.setId("pay-2");
    payment.setOrderId("order-2");
    when(databaseClient.findByOrderId("order-2")).thenReturn(Optional.of(payment));

    when(adapterClient.getAdapter(PaymentGateway.ALIPAY)).thenReturn(adapter);
    when(adapter.processPayment(any(), any(), any(), any()))
        .thenReturn(new PaymentResult(false, null, "Insufficient funds", "{\"ok\":false}", null));

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-2");
    request.setOrderId("order-2");
    request.setGatewayName(PaymentGateway.ALIPAY);

    ProcessPaymentResponse response = template.execute(request);

    assertEquals(PaymentStatus.FAILED, response.getStatus());
    assertEquals(PaymentStatus.FAILED, payment.getStatus());

    verify(eventPublisher, never()).publish(any());
    verify(idempotencyClient, never()).markAsProcessed(any());
    assertFalse(template.hookCalled);
  }

  @Test
  void shouldThrowLockAcquisitionExceptionWhenLockFails() {
    when(idempotencyClient.acquireLock(any(), anyLong(), anyLong())).thenReturn(false);

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-3");

    assertThrows(LockAcquisitionException.class, () -> template.execute(request));

    verify(adapterClient, never()).getAdapter(any());
  }

  @Test
  void shouldThrowDuplicatePaymentWhenAlreadyProcessed() {
    when(idempotencyClient.acquireLock(any(), anyLong(), anyLong())).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed(any())).thenReturn(true);

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-4");

    assertThrows(DuplicatePaymentException.class, () -> template.execute(request));

    verify(idempotencyClient).releaseLock(any());
  }

  @Test
  void shouldThrowWhenPaymentNotFound() {
    when(idempotencyClient.acquireLock(any(), anyLong(), anyLong())).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed(any())).thenReturn(false);
    when(databaseClient.findByOrderId("order-nonexistent")).thenReturn(Optional.empty());

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-5");
    request.setOrderId("order-nonexistent");

    assertThrows(IllegalArgumentException.class, () -> template.execute(request));

    verify(idempotencyClient).releaseLock(any());
  }

  @Test
  void shouldReleaseLockEvenWhenAdapterThrows() {
    when(idempotencyClient.acquireLock(any(), anyLong(), anyLong())).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed(any())).thenReturn(false);

    Payment payment = new Payment();
    payment.setId("pay-6");
    payment.setOrderId("order-6");
    when(databaseClient.findByOrderId("order-6")).thenReturn(Optional.of(payment));

    when(adapterClient.getAdapter(any())).thenReturn(adapter);
    when(adapter.processPayment(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Network error"));

    ProcessPaymentRequest request = new ProcessPaymentRequest();
    request.setPaymentId("pay-6");
    request.setOrderId("order-6");

    assertThrows(RuntimeException.class, () -> template.execute(request));

    verify(idempotencyClient).releaseLock(any());
  }

  /** Concrete subclass that tracks hook invocation. */
  static class ProcessPaymentTemplateWithHook extends ProcessPaymentTemplate {

    boolean hookCalled = false;

    @Override
    protected void validate(ProcessPaymentRequest request) {}

    @Override
    protected ProcessPaymentResponse buildResponse(Payment payment, PaymentResult result) {
      return new ProcessPaymentResponse(
          payment.getId(), payment.getStatus(), result.getPaymentUrl(), result.getTransactionId());
    }

    @Override
    protected void afterPaymentProcessed(Payment payment) {
      hookCalled = true;
    }
  }
}
