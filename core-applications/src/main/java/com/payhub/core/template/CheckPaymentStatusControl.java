package com.payhub.core.template;

import com.payhub.core.controls.CheckPaymentStatusTemplate;
import com.payhub.core.dto.CheckPaymentStatusRequest;

public class CheckPaymentStatusControl extends CheckPaymentStatusTemplate {

  @Override
  protected void validate(CheckPaymentStatusRequest request) {
    if (request.getPaymentId() == null || request.getPaymentId().isBlank()) {
      throw new IllegalArgumentException("paymentId is required");
    }
  }
}
