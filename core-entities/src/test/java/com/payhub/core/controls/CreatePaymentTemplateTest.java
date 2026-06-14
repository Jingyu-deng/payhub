package com.payhub.core.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.DuplicatePaymentException;
import com.payhub.core.exception.LockAcquisitionException;
import com.payhub.core.infra.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreatePaymentTemplateTest {

  private IdempotencyClient idempotencyClient;
  private DatabaseClient databaseClient;
  private EventPublisher eventPublisher;
  private TestCreatePaymentTemplate template;

  @BeforeEach
  void setUp() {
    idempotencyClient = mock(IdempotencyClient.class);
    databaseClient = mock(DatabaseClient.class);
    eventPublisher = mock(EventPublisher.class);

    template = new TestCreatePaymentTemplate();
    template.setIdempotencyClient(idempotencyClient);
    template.setDatabaseClient(databaseClient);
    template.setEventPublisher(eventPublisher);
    template.setAdapterClient(mock(AdapterClient.class));
    template.setSchedulerClient(mock(SchedulerClient.class));
    template.setControlClient(mock(ControlClient.class));
    template.setHttpClient(mock(HttpClient.class));
  }

  @Test
  void shouldSavePaymentAndPublishEventAndReturnResponse() {
    when(idempotencyClient.acquireLock("lock:idempotency:order-1", 5, 10)).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed("processed:order-1")).thenReturn(false);

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-1");
    request.setAmount(new BigDecimal("100.00"));

    PaymentInitiateResponse response = template.execute(request);

    assertNotNull(response);
    assertEquals(PaymentStatus.INITIATED, response.getStatus());

    verify(databaseClient).save(any(Payment.class));
    verify(idempotencyClient).markAsProcessed("processed:order-1");
    verify(eventPublisher).publish(any(PaymentEvent.class));
    verify(idempotencyClient).releaseLock("lock:idempotency:order-1");
  }

  @Test
  void shouldThrowLockAcquisitionExceptionWhenLockFails() {
    when(idempotencyClient.acquireLock("lock:idempotency:order-2", 5, 10)).thenReturn(false);

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-2");
    request.setAmount(new BigDecimal("50.00"));

    assertThrows(LockAcquisitionException.class, () -> template.execute(request));

    verify(idempotencyClient, never()).markAsProcessed(any());
    verify(databaseClient, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void shouldThrowDuplicatePaymentExceptionWhenAlreadyProcessed() {
    when(idempotencyClient.acquireLock("lock:idempotency:order-3", 5, 10)).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed("processed:order-3")).thenReturn(true);

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-3");
    request.setAmount(new BigDecimal("200.00"));

    assertThrows(DuplicatePaymentException.class, () -> template.execute(request));

    verify(idempotencyClient).releaseLock("lock:idempotency:order-3");
    verify(databaseClient, never()).save(any());
  }

  @Test
  void shouldReleaseLockEvenWhenSaveThrows() {
    when(idempotencyClient.acquireLock("lock:idempotency:order-4", 5, 10)).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed("processed:order-4")).thenReturn(false);
    doThrow(new RuntimeException("DB error")).when(databaseClient).save(any(Payment.class));

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-4");
    request.setAmount(new BigDecimal("300.00"));

    assertThrows(RuntimeException.class, () -> template.execute(request));

    verify(idempotencyClient).releaseLock("lock:idempotency:order-4");
  }

  @Test
  void shouldCallValidateBeforeLockAcquisition() {
    template =
        new TestCreatePaymentTemplate() {
          @Override
          protected void validate(PaymentInitiateRequest request) {
            throw new IllegalArgumentException("validation failed");
          }
        };
    template.setIdempotencyClient(idempotencyClient);
    template.setDatabaseClient(databaseClient);
    template.setEventPublisher(eventPublisher);

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-5");

    assertThrows(IllegalArgumentException.class, () -> template.execute(request));

    verify(idempotencyClient, never()).acquireLock(any(), anyLong(), anyLong());
  }

  @Test
  void shouldPassCreatedPaymentToSave() {
    when(idempotencyClient.acquireLock(any(), anyLong(), anyLong())).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed(any())).thenReturn(false);

    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-6");
    request.setAmount(new BigDecimal("500.00"));

    template.execute(request);

    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(databaseClient).save(captor.capture());
    Payment saved = captor.getValue();

    assertEquals("order-6", saved.getOrderId());
    assertEquals(new BigDecimal("500.00"), saved.getAmount());
    assertEquals(PaymentStatus.INITIATED, saved.getStatus());
  }

  /** Minimal concrete subclass for testing the template. */
  static class TestCreatePaymentTemplate extends CreatePaymentTemplate {

    private Payment fixedPayment;

    TestCreatePaymentTemplate() {
      this.fixedPayment = new Payment();
      this.fixedPayment.setId("pay-test");
      this.fixedPayment.setOrderId("default-order");
      this.fixedPayment.setAmount(new BigDecimal("100.00"));
      this.fixedPayment.setStatus(PaymentStatus.INITIATED);
    }

    @Override
    protected void validate(PaymentInitiateRequest request) {}

    @Override
    protected Payment initiatePayment(PaymentInitiateRequest request) {
      Payment p = new Payment();
      p.setId("pay-test");
      p.setOrderId(request.getOrderId());
      p.setAmount(request.getAmount());
      p.setStatus(PaymentStatus.INITIATED);
      return p;
    }

    @Override
    protected PaymentInitiateResponse buildResponse(Payment payment) {
      return new PaymentInitiateResponse(payment.getId(), payment.getStatus(), null);
    }
  }
}
