package com.payhub.infra.database;

import com.payhub.core.domain.Payment;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentMethod;
import com.payhub.core.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentEntity {

  @Id private String id;

  @Column(nullable = false)
  private String orderId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod;

  @Column private String transactionId;

  @Column private String gatewayResponse;

  @Column(updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }

  public static PaymentEntity fromDomain(Payment payment) {
    PaymentEntity entity = new PaymentEntity();
    entity.id = payment.getId();
    entity.orderId = payment.getOrderId();
    entity.amount = payment.getAmount();
    entity.currency = payment.getCurrency();
    entity.status = payment.getStatus();
    entity.paymentMethod = payment.getPaymentMethod();
    entity.transactionId = payment.getTransactionId();
    entity.gatewayResponse = payment.getGatewayResponse();
    entity.createdAt = payment.getCreatedAt();
    return entity;
  }

  public Payment toDomain() {
    Payment payment = new Payment();
    payment.setId(id);
    payment.setOrderId(orderId);
    payment.setAmount(amount);
    payment.setCurrency(currency);
    payment.setStatus(status);
    payment.setPaymentMethod(paymentMethod);
    payment.setTransactionId(transactionId);
    payment.setGatewayResponse(gatewayResponse);
    return payment;
  }

  // ── Getters / setters ──

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
