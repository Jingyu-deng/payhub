package com.payhub.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResult {

  private final boolean success;
  private final String transactionId;
  private final String message;
  private final String rawResponse;
  private final String paymentUrl;
}
