package com.payhub.core.dto;

import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessPaymentResponse {

  private String paymentId;
  private PaymentStatus status;
  private String paymentUrl;
  private String providerTransactionId;
}
