package com.payhub.infra.message.exception;

/** Thrown when a required Kafka header is missing from an incoming message. Non-retryable. */
public class MissingHeaderException extends MessageDeliveryException {

  public MissingHeaderException(String headerName) {
    super("Missing required header '" + headerName + "' on Kafka message");
  }
}
