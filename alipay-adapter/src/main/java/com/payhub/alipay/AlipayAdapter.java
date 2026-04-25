package com.payhub.alipay;

import com.payhub.common.payment.PaymentGateway;
import com.payhub.common.payment.PaymentResult;
import com.payhub.common.payment.RefundResult;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlipayAdapter implements PaymentGateway {
  @Override
  public PaymentResult processPayment(
      String orderId, BigDecimal amount, String currency, Map<String, String> additionalParams) {
    log.info("Alipay processing order {} amount {}", orderId, amount);
    return new PaymentResult(true, UUID.randomUUID().toString(), "Alipay success", null);
  }

  @Override
  public RefundResult refund(String transactionId, BigDecimal amount) {
    log.info("Alipay refund for transaction {}", transactionId);
    return new RefundResult(true, UUID.randomUUID().toString(), "Refund success");
  }

  @Override
  public String getGatewayName() {
    return "alipay";
  }
}
