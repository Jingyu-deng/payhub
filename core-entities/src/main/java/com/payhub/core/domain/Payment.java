package com.payhub.core.domain;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentMethod;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model — pure POJO. No framework annotations. The persistence representation ({@code
 * PaymentEntity}) lives in {@code infra-database-client}.
 */
public class Payment {

  private String id;
  private String orderId;
  private BigDecimal amount;
  private Currency currency;
  private PaymentStatus status;
  private PaymentMethod paymentMethod;
  private String transactionId;
  private String gatewayResponse;
  private Instant createdAt;

  public Payment() {
    this.createdAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Currency getCurrency() {
    return currency;
  }

  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getGatewayResponse() {
    return gatewayResponse;
  }

  public void setGatewayResponse(String gatewayResponse) {
    this.gatewayResponse = gatewayResponse;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
