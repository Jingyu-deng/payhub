package com.payhub.core.domain;

public class RefundResult {

  private final boolean success;
  private final String refundId;
  private final String message;

  public RefundResult(boolean success, String refundId, String message) {
    this.success = success;
    this.refundId = refundId;
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getRefundId() {
    return refundId;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "RefundResult{success="
        + success
        + ", refundId='"
        + refundId
        + "', message='"
        + message
        + "'}";
  }
}
