package com.payhub.payment.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservationConfig {

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> skipActuatorObservations() {
    return registry -> registry.observationConfig()
        .observationPredicate((name, context) -> {
          if (context instanceof ServerRequestObservationContext serverContext) {
            return !serverContext.getCarrier().getRequestURI().startsWith("/actuator");
          }
          return true;
        });
  }
}
