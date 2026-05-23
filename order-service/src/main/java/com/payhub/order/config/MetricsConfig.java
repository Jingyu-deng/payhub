package com.payhub.order.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public Timer orderCreationTimer(MeterRegistry registry) {
    return Timer.builder("order.creation.duration")
        .description("Time taken to create an order")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);
  }
}
