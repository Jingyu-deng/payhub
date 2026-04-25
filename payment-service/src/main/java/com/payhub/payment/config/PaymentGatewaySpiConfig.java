package com.payhub.payment.config;

import com.payhub.common.payment.PaymentGateway;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PaymentGatewaySpiConfig {

  @Bean
  public List<PaymentGateway> paymentGateways() {
    ServiceLoader<PaymentGateway> loader = ServiceLoader.load(PaymentGateway.class);
    List<PaymentGateway> gateways =
        StreamSupport.stream(loader.spliterator(), false)
            .peek(gw -> log.info("Discovered payment gateway via SPI: {}", gw.getGatewayName()))
            .collect(Collectors.toList());
    if (gateways.isEmpty()) {
      log.warn(
          "No payment gateway implementations found. Ensure META-INF/services files are present.");
    }
    return gateways;
  }
}
