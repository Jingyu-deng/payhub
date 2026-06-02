package com.payhub.adapter.alipay;

import com.payhub.adapter.alipay.dto.AlipayPaymentRequest;
import com.payhub.adapter.alipay.dto.AlipayRefundRequest;
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

  private static final String BASE_URL = "https://openapi.alipay.com";

  private volatile AlipayApi alipayApi;

  private AlipayApi getAlipayApi() {
    if (alipayApi == null) {
      synchronized (this) {
        if (alipayApi == null) {
          alipayApi = httpClient.createHttpApi(AlipayApi.class, BASE_URL);
        }
      }
    }
    return alipayApi;
  }

  @Override
  public PaymentResult processPayment(
      String orderId, BigDecimal amount, Currency currency, Map<String, String> params) {
    log.info("Alipay processing: orderId={}, amount={} {}", orderId, amount, currency);

    var request = new AlipayPaymentRequest().setOrderId(orderId).setAmount(amount.toString());

    var response = getAlipayApi().processPayment(request);

    if (response.is2xx()) {
      return new PaymentResult(
          true,
          "ali_" + UUID.randomUUID().toString().substring(0, 16),
          "Payment processed",
          response.getBody(),
          BASE_URL + "/gateway.do?orderId=" + orderId);
    }
    return new PaymentResult(
        false, null, "Alipay API returned " + response.getStatusCode(), null, null);
  }

  @Override
  public RefundResult refund(String transactionId, BigDecimal amount) {
    log.info("Alipay refund: transactionId={}, amount={}", transactionId, amount);

    var request =
        new AlipayRefundRequest()
            .setTransactionId(transactionId)
            .setAmount(amount.toString())
            .setAction("refund");

    var response = getAlipayApi().refund(request);

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
    log.info("Alipay status check: transactionId={}", transactionId);

    var response = getAlipayApi().checkPaymentStatus(transactionId);

    if (response.is2xx()) {
      return new PaymentStatusResult(PaymentStatus.COMPLETED, response.getBody());
    }
    return new PaymentStatusResult(PaymentStatus.FAILED, response.getBody());
  }
}
