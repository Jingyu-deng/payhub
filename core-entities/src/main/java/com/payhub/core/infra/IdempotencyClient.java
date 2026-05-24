package com.payhub.core.infra;

/**
 * Infrastructure interface for idempotency checking. Implementations live in the infra module
 * (Redis, database, etc.).
 */
public interface IdempotencyClient {

  boolean isAlreadyProcessed(String key);

  void markAsProcessed(String key);

  boolean acquireLock(String key, long waitSeconds, long holdSeconds);

  void releaseLock(String key);
}
