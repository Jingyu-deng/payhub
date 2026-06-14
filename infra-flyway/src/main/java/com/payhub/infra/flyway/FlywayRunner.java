package com.payhub.infra.flyway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.flywaydb.core.Flyway;

/**
 * Standalone Flyway CLI — called by Gradle tasks in build.gradle.
 *
 * <p>Reads {@code flyway.conf} from the classpath, then overlays {@code FLYWAY_*} environment
 * variables (Flyway's native mechanism). The {@code ENV_DATASOURCE_*} env vars are also checked as
 * a fallback so both Spring Boot and standalone CLI can share the same variables.
 *
 * <pre>
 *   java FlywayRunner migrate
 *   java FlywayRunner clean
 *   java FlywayRunner info
 *   java FlywayRunner baseline
 * </pre>
 */
public final class FlywayRunner {

  private FlywayRunner() {}

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: FlywayRunner <migrate|clean|info|baseline>");
      System.exit(1);
    }

    var config = Flyway.configure();

    // 1. Load flyway.conf from classpath
    try (InputStream in = FlywayRunner.class.getClassLoader().getResourceAsStream("flyway.conf")) {
      if (in != null) {
        var props = new Properties();
        props.load(in);
        config.configuration(props);
      }
    } catch (IOException e) {
      System.err.println("Warning: could not load flyway.conf — " + e.getMessage());
    }

    // 2. Override with FLYWAY_* or ENV_DATASOURCE_* env vars
    applyEnv(config, "flyway.url", "FLYWAY_URL", "ENV_DATASOURCE_URL");
    applyEnv(config, "flyway.user", "FLYWAY_USER", "ENV_DATASOURCE_USERNAME");
    applyEnv(config, "flyway.password", "FLYWAY_PASSWORD", "ENV_DATASOURCE_PASSWORD");

    Flyway flyway = config.load();

    switch (args[0]) {
      case "migrate" -> {
        int applied = flyway.migrate().migrationsExecuted;
        System.out.println(applied + " migration(s) applied.");
      }
      case "clean" -> {
        flyway.clean();
        System.out.println("Database cleaned.");
      }
      case "info" -> {
        var all = flyway.info().all();
        if (all.length == 0) {
          System.out.println("No migrations found.");
        }
        for (var m : all) {
          System.out.printf("%-20s %-12s %s%n", m.getVersion(), m.getState(), m.getDescription());
        }
      }
      case "baseline" -> {
        flyway.baseline();
        System.out.println("Database baselined.");
      }
      default -> {
        System.err.println("Unknown command: " + args[0]);
        System.exit(1);
      }
    }
  }

  private static void applyEnv(
      org.flywaydb.core.api.configuration.FluentConfiguration config,
      String flywayProperty,
      String... envNames) {
    for (var name : envNames) {
      var value = System.getenv(name);
      if (value != null && !value.isBlank()) {
        var props = new Properties();
        props.setProperty(flywayProperty, value);
        config.configuration(props);
        return;
      }
    }
  }
}
