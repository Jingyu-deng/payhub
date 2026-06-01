package com.payhub.infra.message;

import com.payhub.infra.common.YamlPropertySourceFactory;
import com.payhub.infra.message.property.KafkaConsumerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@PropertySource(value = "classpath:kafka.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(KafkaConsumerProperties.class)
@RequiredArgsConstructor
@Slf4j
public class PayHubKafkaConfig {

  private final KafkaConsumerProperties consumerProperties;

  @Bean
  public CommonErrorHandler errorHandler() {
    KafkaConsumerProperties.RetryConfig retry = consumerProperties.getRetry();

    ExponentialBackOff backOff = new ExponentialBackOff();
    backOff.setInitialInterval(retry.getInitialIntervalMs());
    backOff.setMultiplier(retry.getMultiplier());
    backOff.setMaxInterval(retry.getMaxIntervalMs());
    backOff.setMaxElapsedTime(retry.getMaxElapsedTimeMs());

    DefaultErrorHandler handler =
        new DefaultErrorHandler(
            (rec, ex) ->
                log.warn(
                    "Exhausted retries for record key={}, offset={}, skipping",
                    rec.key(),
                    rec.offset()),
            backOff);

    for (String className : retry.getNotRetryableExceptions()) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends Exception> exceptionClass =
            (Class<? extends Exception>) Class.forName(className);
        handler.addNotRetryableExceptions(exceptionClass);
      } catch (ClassNotFoundException e) {
        log.error("Non-retryable exception class not found: {}", className, e);
      }
    }

    return handler;
  }
}
