package com.payhub.core.dto;

import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentInitiateResponse {

  private String paymentId;
  private PaymentStatus status;
  private String redirectUrl; // Payment method Selection page
}
