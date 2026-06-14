package com.payhub.infra.database;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
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
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
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
  @Column
  private PaymentGateway paymentGateway;

  @Column private String transactionId;

  @Column private String gatewayResponse;

  @Column private String notifyUrl;

  @Column private String checkPgStatusControlJobKey;

  @Column(updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }
}
