package com.payhub.core.domain;

public class PaymentEvent {

  public enum Type {
    INITIATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
  }

  private final Type type;
  private final String orderId;
  private final String paymentId;
  private final String gateway;
  private final String transactionId;
  private final long timestamp;

  public PaymentEvent(
      Type type, String orderId, String paymentId, String gateway, String transactionId) {
    this.type = type;
    this.orderId = orderId;
    this.paymentId = paymentId;
    this.gateway = gateway;
    this.transactionId = transactionId;
    this.timestamp = System.currentTimeMillis();
  }

  public Type getType() {
    return type;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public String getGateway() {
    return gateway;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "PaymentEvent{" + type + ", orderId='" + orderId + "', gateway='" + gateway + "'}";
  }
}
