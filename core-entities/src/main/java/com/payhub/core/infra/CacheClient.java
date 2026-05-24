package com.payhub.core.infra;

import java.util.Optional;

/** Port for caching values with TTL — gateway configs, exchange rates, etc. */
public interface CacheClient {

  <T> Optional<T> get(String key, Class<T> type);

  void put(String key, Object value, long ttlSeconds);

  void evict(String key);
}
