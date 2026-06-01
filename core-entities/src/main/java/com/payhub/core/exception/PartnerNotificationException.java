package com.payhub.core.exception;

/**
 * Thrown when a partner webhook notification fails with a retryable error (5xx or network error).
 */
public class PartnerNotificationException extends PaymentProcessingException {

  private final transient int statusCode;

  public PartnerNotificationException(String notifyUrl, int statusCode) {
    super("Partner notification failed: url=" + notifyUrl + ", status=" + statusCode);
    this.statusCode = statusCode;
  }

  public PartnerNotificationException(String notifyUrl, Throwable cause) {
    super("Partner notification failed: url=" + notifyUrl + ", error=" + cause.getMessage(), cause);
    this.statusCode = 0;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
