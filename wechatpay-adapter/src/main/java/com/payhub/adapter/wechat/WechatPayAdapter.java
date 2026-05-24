package com.payhub.adapter.wechat;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.adapters.AdapterInjector;
import com.payhub.core.domain.PaymentResult;
import com.payhub.core.domain.RefundResult;
import com.payhub.core.enums.Currency;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class WechatPayAdapter extends AdapterInjector implements Adapter {

  private static final Logger log = Logger.getLogger(WechatPayAdapter.class.getName());

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
  public String getGatewayName() {
    return "wechat";
  }
}
