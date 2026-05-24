package com.payhub.core.exception;

public class DuplicatePaymentException extends PaymentProcessingException {

  public DuplicatePaymentException(String orderId) {
    super("Order " + orderId + " has already been processed");
  }
}
