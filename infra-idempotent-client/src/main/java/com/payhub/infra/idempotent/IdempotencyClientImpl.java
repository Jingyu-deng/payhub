package com.payhub.infra.idempotent;

import com.payhub.core.infra.IdempotencyClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyClientImpl implements IdempotencyClient {

  private final Map<String, Boolean> processed = new ConcurrentHashMap<>();
  private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  @Override
  public boolean isAlreadyProcessed(String key) {
    return processed.getOrDefault(key, false);
  }

  @Override
  public void markAsProcessed(String key) {
    processed.put(key, true);
  }

  @Override
  public boolean acquireLock(String key, long waitSeconds, long holdSeconds) {
    locks.putIfAbsent(key, new ReentrantLock());
    ReentrantLock lock = locks.get(key);
    try {
      return lock.tryLock(waitSeconds, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public void releaseLock(String key) {
    ReentrantLock lock = locks.get(key);
    if (lock != null && lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }
}
