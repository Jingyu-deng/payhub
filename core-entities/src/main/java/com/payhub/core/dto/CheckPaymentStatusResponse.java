package com.payhub.core.dto;

import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckPaymentStatusResponse {

  private String paymentId;
  private String orderId;
  private String transactionId;
  private PaymentStatus status;
  private String gatewayName;
}
