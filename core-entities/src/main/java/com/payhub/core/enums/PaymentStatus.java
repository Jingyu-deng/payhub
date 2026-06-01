package com.payhub.core.enums;

public enum PaymentStatus {
  INITIATED,
  PROCESSING,
  COMPLETED,
  FAILED;

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED;
  }
}
