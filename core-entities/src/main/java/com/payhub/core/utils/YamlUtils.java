package com.payhub.core.utils;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * YAML utilities backed by Jackson, so annotations like {@code @JsonNaming} on the target POJO are
 * honoured.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class YamlUtils {

  private static final YAMLMapper MAPPER =
      YAMLMapper.builder().addModule(new JavaTimeModule()).build();

  /** Loads a YAML resource from the classpath into the given type. */
  public static <T> T loadFromClasspath(String resource, Class<T> type) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    InputStream input = classLoader.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalArgumentException("Resource not found on classpath: " + resource);
    }
    try (input) {
      return MAPPER.readValue(input, type);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load YAML from classpath: " + resource, e);
    }
  }

  public static YAMLMapper mapper() {
    return MAPPER;
  }
}
