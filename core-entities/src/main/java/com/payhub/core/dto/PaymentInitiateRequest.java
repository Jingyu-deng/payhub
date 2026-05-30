package com.payhub.core.dto;

import com.payhub.core.enums.Currency;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentInitiateRequest {

  private String orderId;
  private BigDecimal amount;
  private Currency currency;
  private String notifyUrl;
}
