package com.payhub.infra.monitor.health;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Checks PostgreSQL connectivity with a simple {@code SELECT 1}. */
@Component
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

  private final DataSource dataSource;

  public DatabaseHealthIndicator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Health health() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("SELECT 1");
      return Health.up().withDetail("database", "PostgreSQL").build();
    } catch (Exception e) {
      log.warn("Database health check failed: {}", e.getMessage());
      return Health.down(e).withDetail("database", "PostgreSQL").build();
    }
  }
}
