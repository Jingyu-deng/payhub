package com.payhub.infra.idempotent;

import com.payhub.core.infra.IdempotencyClient;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis-backed {@link IdempotencyClient} powered by Redisson.
 *
 * <p>Idempotency keys are stored as {@code RBucket} entries with a configurable TTL so they
 * auto-expire. Distributed locks use Redisson {@link RLock} with the same semantics as the port
 * interface: wait up to {@code waitSeconds} to acquire, hold for at most {@code holdSeconds}.
 */
@Component
public class IdempotencyClientImpl implements IdempotencyClient {

  private final RedissonClient redissonClient;
  private final long idempotencyTtlSeconds;

  public IdempotencyClientImpl(
      RedissonClient redissonClient,
      @Value("${payhub.redis.idempotency-ttl-seconds:86400}") long idempotencyTtlSeconds) {
    this.redissonClient = redissonClient;
    this.idempotencyTtlSeconds = idempotencyTtlSeconds;
  }

  @Override
  public boolean isAlreadyProcessed(String key) {
    return redissonClient.getBucket(key).get() != null;
  }

  @Override
  public void markAsProcessed(String key) {
    redissonClient.getBucket(key).set(true, idempotencyTtlSeconds, TimeUnit.SECONDS);
  }

  @Override
  public boolean acquireLock(String key, long waitSeconds, long holdSeconds) {
    RLock lock = redissonClient.getLock(key);
    try {
      return lock.tryLock(waitSeconds, holdSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public void releaseLock(String key) {
    RLock lock = redissonClient.getLock(key);
    if (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }
}
