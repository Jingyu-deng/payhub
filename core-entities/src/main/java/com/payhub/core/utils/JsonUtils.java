package com.payhub.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payhub.core.exception.SerializationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

  private static final JsonMapper MAPPER =
      JsonMapper.builder().addModule(new JavaTimeModule()).build();

  public static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize object to JSON: {}", e.getMessage(), e);
      throw new SerializationException("Failed to serialize object to JSON", e);
    }
  }

  public static String toPrettyJson(Object value) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize object to pretty JSON: {}", e.getMessage(), e);
      throw new SerializationException("Failed to serialize object to pretty JSON", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize JSON to {}: {}", type.getSimpleName(), e.getMessage(), e);
      throw new SerializationException("Failed to deserialize JSON", e);
    }
  }

  public static <T> T fromJson(String json, TypeReference<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to deserialize JSON to {}: {}", type.getType().getTypeName(), e.getMessage(), e);
      throw new SerializationException("Failed to deserialize JSON", e);
    }
  }

  public static JsonMapper mapper() {
    return MAPPER;
  }
}
