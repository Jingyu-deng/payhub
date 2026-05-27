package com.payhub.core.dto;

import com.payhub.core.enums.PaymentGateway;
import java.util.Map;
import lombok.Data;

/** Request to process a payment after the user selects a payment method. */
@Data
public class ProcessPaymentRequest {

  private String paymentId;
  private String orderId;
  private PaymentGateway gatewayName;
  private Map<String, String> params;
}
