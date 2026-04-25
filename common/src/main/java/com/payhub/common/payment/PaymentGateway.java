package com.payhub.common.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {
  PaymentResult processPayment(
      String orderId, BigDecimal amount, String currency, Map<String, String> additionalParams);

  RefundResult refund(String transactionId, BigDecimal amount);

  String getGatewayName();
}
