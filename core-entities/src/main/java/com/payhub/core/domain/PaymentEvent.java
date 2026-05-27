package com.payhub.core.domain;

import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentEvent {

  private final PaymentStatus type;
  private final String orderId;
  private final String paymentId;
  private final PaymentGateway gateway;
  private final String transactionId;
  private final long timestamp;
}
