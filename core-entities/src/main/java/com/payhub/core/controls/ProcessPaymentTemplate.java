package com.payhub.core.controls;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.DuplicatePaymentException;
import com.payhub.core.exception.LockAcquisitionException;

/**
 * Template for processing a payment after the user selects a payment method (gateway). Dispatches
 * to the SPI-discovered {@link Adapter}, persists the result, and publishes an event.
 */
public abstract class ProcessPaymentTemplate
    extends ControlInjector<ProcessPaymentRequest, ProcessPaymentResponse> {

  @Override
  public final ProcessPaymentResponse execute(ProcessPaymentRequest request) {

    validate(request);

    String lockKey = "lock:payment:process:" + request.getPaymentId();
    String idempotencyKey = "payment:processed:" + request.getPaymentId();

    if (!idempotencyClient.acquireLock(lockKey, 5, 10)) {
      throw new LockAcquisitionException(request.getPaymentId());
    }
    try {
      if (idempotencyClient.isAlreadyProcessed(idempotencyKey)) {
        throw new DuplicatePaymentException(request.getPaymentId());
      }

      Adapter adapter = adapterClient.getAdapter(request.getGatewayName());

      Payment payment =
          databaseClient
              .findByOrderId(request.getOrderId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Payment not found for order: " + request.getOrderId()));

      PaymentResult result =
          adapter.processPayment(
              request.getOrderId(),
              payment.getAmount(),
              payment.getCurrency(),
              request.getParams() != null ? request.getParams() : java.util.Collections.emptyMap());

      payment.setTransactionId(result.getTransactionId());
      payment.setGatewayResponse(result.getRawResponse());

      if (result.isSuccess()) {
        payment.setStatus(PaymentStatus.PROCESSING);
        idempotencyClient.markAsProcessed(idempotencyKey);

        eventPublisher.publish(
            new PaymentEvent(
                PaymentEvent.Type.PROCESSING,
                payment.getOrderId(),
                payment.getId(),
                adapter.getGatewayName(),
                result.getTransactionId()));
      } else {
        payment.setStatus(PaymentStatus.FAILED);
      }

      databaseClient.save(payment);

      return buildResponse(payment, result);

    } finally {
      idempotencyClient.releaseLock(lockKey);
    }
  }

  protected abstract void validate(ProcessPaymentRequest request);

  protected abstract ProcessPaymentResponse buildResponse(Payment payment, PaymentResult result);
}
