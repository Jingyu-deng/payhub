package com.payhub.infra.message.exception;

/** Thrown when an event class cannot be resolved or does not implement BaseEvent. Non-retryable. */
public class UnknownEventTypeException extends MessageDeliveryException {

  public UnknownEventTypeException(String className) {
    super("Unknown event type: " + className);
  }

  public UnknownEventTypeException(String className, Throwable cause) {
    super("Unknown event type: " + className, cause);
  }
}
