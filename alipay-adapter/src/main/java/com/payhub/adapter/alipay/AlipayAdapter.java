package com.payhub.adapter.alipay;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.adapters.AdapterInjector;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.domain.RefundResult;
import com.payhub.core.enums.Currency;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class AlipayAdapter extends AdapterInjector implements Adapter {

  private static final Logger log = Logger.getLogger(AlipayAdapter.class.getName());

  @Override
  public PaymentResult processPayment(
      String orderId, BigDecimal amount, Currency currency, Map<String, String> params) {
    log.info("Alipay processing: orderId=" + orderId + ", amount=" + amount + " " + currency);

    var response =
        getHttpClient()
            .post(
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
        getHttpClient()
            .post(
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
  public String getGatewayName() {
    return "alipay";
  }
}
