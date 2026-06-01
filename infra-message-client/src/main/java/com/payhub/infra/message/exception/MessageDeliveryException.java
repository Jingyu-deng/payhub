package com.payhub.infra.message.exception;

/** Base exception for message delivery failures. */
public class MessageDeliveryException extends RuntimeException {

  public MessageDeliveryException(String message) {
    super(message);
  }

  public MessageDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
