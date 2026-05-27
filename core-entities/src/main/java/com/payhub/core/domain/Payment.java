package com.payhub.core.domain;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class Payment {

  private String id;
  private String orderId;
  private BigDecimal amount;
  private Currency currency;
  private PaymentStatus status;
  private PaymentGateway paymentGateway;
  private String transactionId;
  private String gatewayResponse;
  private String checkPgStatusControlJobKey;

  @Setter(AccessLevel.NONE)
  private Instant createdAt = Instant.now();
}
