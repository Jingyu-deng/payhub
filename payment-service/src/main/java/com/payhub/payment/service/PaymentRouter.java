package com.payhub.payment.service;

import com.payhub.common.payment.PaymentGateway;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentRouter {

  private final Map<String, PaymentGateway> gatewayMap = new ConcurrentHashMap<>();

  // Spring will inject the List of beans (the ones we created via SPI config)
  public PaymentRouter(List<PaymentGateway> paymentGateways) {
    for (PaymentGateway gateway : paymentGateways) {
      gatewayMap.put(gateway.getGatewayName(), gateway);
      log.info("Registered payment gateway: {}", gateway.getGatewayName());
    }
  }

  public PaymentGateway getGateway(String name) {
    PaymentGateway gateway = gatewayMap.get(name);
    if (gateway == null) {
      throw new IllegalArgumentException("Unknown payment gateway: " + name);
    }
    return gateway;
  }

  // Example routing rule
  public PaymentGateway route(String userId, String productId) {
    if (userId != null && userId.toLowerCase().contains("wechat")) {
      return getGateway("wechat");
    }
    return getGateway("alipay");
  }
}
