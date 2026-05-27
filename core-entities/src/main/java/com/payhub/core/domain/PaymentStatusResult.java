package com.payhub.core.domain;

import com.payhub.core.enums.PaymentStatus;
import lombok.Value;

@Value
public class PaymentStatusResult {

  PaymentStatus status;
  String rawResponse;
}
