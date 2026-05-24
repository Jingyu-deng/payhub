package com.payhub.core.exception;

public class LockAcquisitionException extends PaymentProcessingException {

  public LockAcquisitionException(String orderId) {
    super("Could not acquire lock for order " + orderId);
  }
}
