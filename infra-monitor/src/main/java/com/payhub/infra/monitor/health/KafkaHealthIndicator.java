package com.payhub.infra.monitor.health;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Checks Kafka broker connectivity via {@link KafkaAdmin}'s configuration.
 *
 * <p>Uses {@link ObjectProvider} rather than {@code @ConditionalOnBean} so the indicator always
 * registers and simply reports {@code UNKNOWN} when no {@link KafkaAdmin} bean is present.
 */
@Component
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

  private final ObjectProvider<KafkaAdmin> kafkaAdminProvider;

  public KafkaHealthIndicator(ObjectProvider<KafkaAdmin> kafkaAdminProvider) {
    this.kafkaAdminProvider = kafkaAdminProvider;
  }

  @Override
  public Health health() {
    KafkaAdmin kafkaAdmin = kafkaAdminProvider.getIfAvailable();
    if (kafkaAdmin == null) {
      return Health.unknown().withDetail("kafka", "not configured (no KafkaAdmin bean)").build();
    }
    try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
      DescribeClusterResult cluster = client.describeCluster();
      String clusterId = cluster.clusterId().get();
      int nodeCount = cluster.nodes().get().size();
      return Health.up()
          .withDetail("kafka", "reachable")
          .withDetail("clusterId", clusterId)
          .withDetail("nodes", nodeCount)
          .build();
    } catch (Exception e) {
      log.warn("Kafka health check failed: {}", e.getMessage());
      return Health.down(e).withDetail("kafka", "unreachable").build();
    }
  }
}
