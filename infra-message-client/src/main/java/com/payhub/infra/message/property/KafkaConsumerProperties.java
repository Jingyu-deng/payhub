package com.payhub.infra.message.property;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.kafka.consumer")
@Data
public class KafkaConsumerProperties {

  private RetryConfig retry = new RetryConfig();

  @Data
  public static class RetryConfig {
    private long initialIntervalMs = 1000;
    private double multiplier = 2.0;
    private long maxIntervalMs = 60000;
    private long maxElapsedTimeMs = 31000;
    private List<String> notRetryableExceptions = List.of();
  }
}
