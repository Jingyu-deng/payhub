package com.payhub.core.template;

import com.payhub.core.controls.ProcessPaymentTemplate;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.dto.CheckPaymentStatusRequest;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import java.time.Duration;

public class ProcessPaymentControl extends ProcessPaymentTemplate {

  @Override
  protected void validate(ProcessPaymentRequest request) {
    if (request.getPaymentId() == null || request.getPaymentId().isBlank()) {
      throw new IllegalArgumentException("paymentId is required");
    }
    if (request.getOrderId() == null || request.getOrderId().isBlank()) {
      throw new IllegalArgumentException("orderId is required");
    }
    if (request.getGatewayName() == null) {
      throw new IllegalArgumentException("gatewayName is required");
    }
  }

  @Override
  protected ProcessPaymentResponse buildResponse(Payment payment, PaymentResult result) {
    return new ProcessPaymentResponse(
        payment.getId(), payment.getStatus(), result.getPaymentUrl(), result.getTransactionId());
  }

  @Override
  protected void afterPaymentProcessed(Payment payment) {
    String jobKey =
        schedulerClient.scheduleRecurring(
            CheckPaymentStatusControl.class,
            new CheckPaymentStatusRequest(payment.getId()),
            Duration.ofSeconds(30),
            Duration.ofMinutes(5));
    payment.setCheckPgStatusControlJobKey(jobKey);
  }
}
