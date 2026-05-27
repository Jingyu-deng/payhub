package com.payhub.adapter.alipay;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.adapters.AdapterInjector;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.domain.PaymentStatusResult;
import com.payhub.core.domain.RefundResult;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlipayAdapter extends AdapterInjector implements Adapter {

  @Override
  public PaymentResult processPayment(
      String orderId, BigDecimal amount, Currency currency, Map<String, String> params) {
    log.info("Alipay processing: orderId=" + orderId + ", amount=" + amount + " " + currency);

    var response =
        httpClient.post(
            "https://openapi.alipay.com/gateway.do",
            Map.of("Content-Type", "application/json"),
            "{\"orderId\":\"" + orderId + "\",\"amount\":\"" + amount + "\"}");

    if (response.is2xx()) {
      return new PaymentResult(
          true,
          "ali_" + UUID.randomUUID().toString().substring(0, 16),
          "Payment processed",
          response.getBody(),
          "https://openapi.alipay.com/gateway.do?orderId=" + orderId);
    }
    return new PaymentResult(
        false, null, "Alipay API returned " + response.getStatusCode(), null, null);
  }

  @Override
  public RefundResult refund(String transactionId, BigDecimal amount) {
    log.info("Alipay refund: transactionId=" + transactionId + ", amount=" + amount);

    var response =
        httpClient.post(
            "https://openapi.alipay.com/gateway.do",
            Map.of("Content-Type", "application/json"),
            "{\"transactionId\":\""
                + transactionId
                + "\",\"amount\":\""
                + amount
                + "\",\"action\":\"refund\"}");

    if (response.is2xx()) {
      return new RefundResult(
          true, "ali_refund_" + UUID.randomUUID().toString().substring(0, 16), "Refund processed");
    }
    return new RefundResult(false, null, "Alipay refund API returned " + response.getStatusCode());
  }

  @Override
  public PaymentGateway getGateway() {
    return PaymentGateway.ALIPAY;
  }

  @Override
  public PaymentStatusResult checkPaymentStatus(String transactionId) {
    log.info("Alipay status check: transactionId=" + transactionId);

    var response =
        getHttpClient()
            .get(
                "https://openapi.alipay.com/gateway.do?transactionId=" + transactionId,
                Map.of("Content-Type", "application/json"));

    if (response.is2xx()) {
      return new PaymentStatusResult(PaymentStatus.COMPLETED, response.getBody());
    }
    return new PaymentStatusResult(PaymentStatus.FAILED, response.getBody());
  }
}
