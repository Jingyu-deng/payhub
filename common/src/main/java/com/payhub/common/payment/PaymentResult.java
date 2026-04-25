package com.payhub.common.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
  private boolean success;
  private String transactionId;
  private String message;
  private String rawResponse;
}
