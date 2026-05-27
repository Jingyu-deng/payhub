package com.payhub.core.controls;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.controls.base.ControlInjector;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentStatusResult;
import com.payhub.core.dto.CheckPaymentStatusRequest;
import com.payhub.core.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Template for checking the status of a payment with the gateway. Looks up the payment, queries the
 * adapter for the current status, updates the persisted status, and returns whether the payment has
 * reached a terminal state.
 *
 * <p>Triggered by the periodic timer scheduled in {@link ProcessPaymentTemplate}, not by an
 * endpoint.
 */
@Slf4j
public abstract class CheckPaymentStatusTemplate
    extends ControlInjector<CheckPaymentStatusRequest, Boolean> {

  @Override
  public final Boolean execute(CheckPaymentStatusRequest request) {

    validate(request);

    Payment payment =
        databaseClient
            .findByPaymentId(request.getPaymentId())
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found: " + request.getPaymentId()));

    Adapter adapter = adapterClient.getAdapter(payment.getPaymentGateway());

    PaymentStatusResult result = adapter.checkPaymentStatus(payment.getTransactionId());

    payment.setStatus(result.getStatus());
    payment.setGatewayResponse(result.getRawResponse());
    databaseClient.save(payment);

    return isTerminal(payment.getStatus());
  }

  private static boolean isTerminal(PaymentStatus status) {
    return status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED;
  }

  protected abstract void validate(CheckPaymentStatusRequest request);
}
