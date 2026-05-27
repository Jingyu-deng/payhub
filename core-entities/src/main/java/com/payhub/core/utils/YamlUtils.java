package com.payhub.core.utils;

import com.payhub.core.exception.SerializationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class YamlUtils {

  private static final Logger log = LoggerFactory.getLogger(YamlUtils.class);
  private static final Yaml YAML = new Yaml();

  public static String toYaml(Object value) {
    try {
      return YAML.dump(value);
    } catch (Exception e) {
      log.error("Failed to serialize object to YAML: {}", e.getMessage(), e);
      throw new SerializationException("Failed to serialize object to YAML", e);
    }
  }

  public static <T> T fromYaml(String yaml, Class<T> type) {
    try {
      return YAML.loadAs(yaml, type);
    } catch (Exception e) {
      log.error("Failed to deserialize YAML to {}: {}", type.getSimpleName(), e.getMessage(), e);
      throw new SerializationException("Failed to deserialize YAML", e);
    }
  }

  public static <T> T loadAs(InputStream input, Class<T> type) {
    try {
      return YAML.loadAs(input, type);
    } catch (Exception e) {
      log.error(
          "Failed to load YAML from stream as {}: {}", type.getSimpleName(), e.getMessage(), e);
      throw new SerializationException("Failed to load YAML from stream", e);
    }
  }

  public static <T> T loadAs(String filePath, Class<T> type) {
    Path path = Path.of(filePath);
    try (InputStream input = Files.newInputStream(path)) {
      return loadAs(input, type);
    } catch (IOException e) {
      log.error("Failed to read YAML file {}: {}", filePath, e.getMessage(), e);
      throw new UncheckedIOException("Failed to read YAML file: " + filePath, e);
    }
  }

  public static <T> T loadAs(Path filePath, Class<T> type) {
    return loadAs(filePath.toString(), type);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> loadAsMap(InputStream input) {
    return loadAs(input, Map.class);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> loadAsMap(String filePath) {
    return loadAs(filePath, Map.class);
  }

  public static Map<String, Object> loadAsMap(Path filePath) {
    return loadAsMap(filePath.toString());
  }

  public static Map<String, Object> loadAsMapFromClasspath(String resource) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    InputStream input = classLoader.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalArgumentException("Resource not found on classpath: " + resource);
    }
    try (input) {
      return loadAsMap(input);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close classpath resource: " + resource, e);
    }
  }

  public static Yaml yaml() {
    return YAML;
  }
}
