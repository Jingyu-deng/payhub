package com.payhub.payment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public Timer paymentProcessingTimer(MeterRegistry registry) {
    return Timer.builder("payment.processing.duration")
        .description("Time taken to process a payment")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);
  }

  @Bean
  public Counter paymentSuccessCounter(MeterRegistry registry) {
    return Counter.builder("payment.processed.total")
        .description("Total number of payments processed")
        .tag("status", "success")
        .register(registry);
  }

  @Bean
  public Counter paymentFailureCounter(MeterRegistry registry) {
    return Counter.builder("payment.processed.total")
        .description("Total number of payments processed")
        .tag("status", "failure")
        .register(registry);
  }
}
