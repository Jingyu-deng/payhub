package com.payhub.core.exception;

public class ControlInstantiationException extends PaymentProcessingException {

  public ControlInstantiationException(String className, Throwable cause) {
    super("Failed to instantiate " + className, cause);
  }
}
