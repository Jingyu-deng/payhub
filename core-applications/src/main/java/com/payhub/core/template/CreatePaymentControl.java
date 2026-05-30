package com.payhub.core.template;

import com.payhub.core.controls.CreatePaymentTemplate;
import com.payhub.core.domain.Payment;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentStatus;
import java.util.UUID;

/**
 * Creates a payment order — no gateway selected yet. The user will choose a payment method
 * afterward.
 */
public class CreatePaymentControl extends CreatePaymentTemplate {

  @Override
  protected void validate(PaymentInitiateRequest request) {
    if (request.getOrderId() == null || request.getOrderId().isBlank()) {
      throw new IllegalArgumentException("orderId is required");
    }
    if (request.getAmount() == null
        || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }
  }

  @Override
  protected Payment initiatePayment(PaymentInitiateRequest request) {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID().toString());
    payment.setOrderId(request.getOrderId());
    payment.setAmount(request.getAmount());
    payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : Currency.CNY);
    payment.setStatus(PaymentStatus.INITIATED);
    payment.setNotifyUrl(request.getNotifyUrl());
    return payment;
  }

  @Override
  protected PaymentInitiateResponse buildResponse(Payment payment) {
    return new PaymentInitiateResponse(payment.getId(), payment.getStatus(), null);
  }
}
