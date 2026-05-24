package com.payhub.core.exception;

public class ServiceNotFoundException extends PaymentProcessingException {

  public ServiceNotFoundException(String serviceType) {
    super("No SPI implementation found for " + serviceType);
  }
}
