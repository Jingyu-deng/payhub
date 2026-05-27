package com.payhub.adapter.wechat;

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
public class WechatPayAdapter extends AdapterInjector implements Adapter {

  @Override
  public PaymentResult processPayment(
      String orderId, BigDecimal amount, Currency currency, Map<String, String> params) {
    log.info("WeChat Pay processing: orderId=" + orderId + ", amount=" + amount + " " + currency);

    var response =
        getHttpClient()
            .post(
                "https://api.mch.weixin.qq.com/pay/unifiedorder",
                Map.of("Content-Type", "application/json"),
                "{\"orderId\":\"" + orderId + "\",\"amount\":\"" + amount + "\"}");

    if (response.is2xx()) {
      return new PaymentResult(
          true,
          "wx_" + UUID.randomUUID().toString().substring(0, 16),
          "Payment processed",
          response.getBody(),
          "https://api.mch.weixin.qq.com/pay/unifiedorder?orderId=" + orderId);
    }
    return new PaymentResult(
        false, null, "WeChat Pay API returned " + response.getStatusCode(), null, null);
  }

  @Override
  public RefundResult refund(String transactionId, BigDecimal amount) {
    log.info("WeChat Pay refund: transactionId=" + transactionId + ", amount=" + amount);

    var response =
        getHttpClient()
            .post(
                "https://api.mch.weixin.qq.com/secapi/pay/refund",
                Map.of("Content-Type", "application/json"),
                "{\"transactionId\":\"" + transactionId + "\",\"amount\":\"" + amount + "\"}");

    if (response.is2xx()) {
      return new RefundResult(
          true, "wx_refund_" + UUID.randomUUID().toString().substring(0, 16), "Refund processed");
    }
    return new RefundResult(
        false, null, "WeChat Pay refund API returned " + response.getStatusCode());
  }

  @Override
  public PaymentGateway getGateway() {
    return PaymentGateway.WECHAT_PAY;
  }

  @Override
  public PaymentStatusResult checkPaymentStatus(String transactionId) {
    log.info("WeChat Pay status check: transactionId=" + transactionId);

    var response =
        getHttpClient()
            .get(
                "https://api.mch.weixin.qq.com/pay/orderquery?transactionId=" + transactionId,
                Map.of("Content-Type", "application/json"));

    if (response.is2xx()) {
      return new PaymentStatusResult(PaymentStatus.COMPLETED, response.getBody());
    }
    return new PaymentStatusResult(PaymentStatus.FAILED, response.getBody());
  }
}
