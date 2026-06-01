package com.payhub.core.exception;

/** Thrown when a Quartz job cannot be scheduled, executed, or cancelled. */
public class JobSchedulingException extends RuntimeException {

  public JobSchedulingException(String message, Throwable cause) {
    super(message, cause);
  }
}
