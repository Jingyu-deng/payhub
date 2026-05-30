package com.payhub.core.domain;

import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

  private PaymentStatus type;
  private Payment payment;
  private long timestamp;
}
