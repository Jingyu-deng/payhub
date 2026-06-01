package com.payhub.core.template;

import com.payhub.core.controls.ProcessPaymentTemplate;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.dto.CheckPaymentStatusRequest;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.core.properties.CheckPaymentStatusProperties;
import com.payhub.core.utils.YamlUtils;
import java.time.Duration;

public class ProcessPaymentControl extends ProcessPaymentTemplate {

  private static final String CHECK_STATUS_CONFIG = "check-payment-status.yml";

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
    CheckPaymentStatusProperties props =
        YamlUtils.loadFromClasspath(CHECK_STATUS_CONFIG, CheckPaymentStatusProperties.class);

    String jobKey =
        schedulerClient.scheduleRecurring(
            CheckPaymentStatusControl.class,
            new CheckPaymentStatusRequest(payment.getId()),
            Duration.ofSeconds(props.getPollIntervalSeconds()),
            Duration.ofMinutes(props.getMaxPollDurationMinutes()));
    payment.setCheckPgStatusControlJobKey(jobKey);
  }
}
