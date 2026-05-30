package com.payhub.core.controls;

import com.payhub.core.controls.base.ControlInjector;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.DuplicatePaymentException;
import com.payhub.core.exception.LockAcquisitionException;
import lombok.extern.slf4j.Slf4j;

/**
 * Template for creating a payment order. No gateway invocation — the user has not selected a
 * payment method yet. Lock, idempotency, persistence, and event publishing are handled here.
 */
@Slf4j
public abstract class CreatePaymentTemplate
    extends ControlInjector<PaymentInitiateRequest, PaymentInitiateResponse> {

  @Override
  public final PaymentInitiateResponse execute(PaymentInitiateRequest request) {

    validate(request);

    String lockKey = "lock:idempotency:" + request.getOrderId();
    String idempotencyKey = "processed:" + request.getOrderId();

    if (!idempotencyClient.acquireLock(lockKey, 5, 10)) {
      throw new LockAcquisitionException(request.getOrderId());
    }
    try {
      if (idempotencyClient.isAlreadyProcessed(idempotencyKey)) {
        throw new DuplicatePaymentException(request.getOrderId());
      }

      Payment payment = initiatePayment(request);

      databaseClient.save(payment);

      idempotencyClient.markAsProcessed(idempotencyKey);

      eventPublisher.publish(
          new PaymentEvent(PaymentStatus.INITIATED, payment, System.currentTimeMillis()));

      return buildResponse(payment);

    } finally {
      idempotencyClient.releaseLock(lockKey);
    }
  }

  // ── business steps (abstract, control-owned) ──

  protected abstract void validate(PaymentInitiateRequest request);

  protected abstract Payment initiatePayment(PaymentInitiateRequest request);

  protected abstract PaymentInitiateResponse buildResponse(Payment payment);
}
