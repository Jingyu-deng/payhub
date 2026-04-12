package com.payhub.payment.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

  private final RedissonClient redissonClient;
  private final Map<String, Integer> stock = new ConcurrentHashMap<>();

  {
    stock.put("P001", 10);
    stock.put("P002", 5);
  }

  public boolean deductStock(String productId, int quantity) {
    String lockKey = "lock:stock:" + productId;
    RLock lock = redissonClient.getLock(lockKey);
    try {
      if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        try {
          Integer current = stock.getOrDefault(productId, 0);
          if (current >= quantity) {
            stock.put(productId, current - quantity);
            log.info(
                "Deducted {} of {}, remaining stock: {}", quantity, productId, current - quantity);
            return true;
          } else {
            log.warn(
                "Insufficient stock for {}, requested {}, available {}",
                productId,
                quantity,
                current);
            return false;
          }
        } finally {
          lock.unlock();
        }
      } else {
        log.warn("Could not acquire lock for {}", productId);
        return false;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while acquiring lock for {}", productId, e);
      return false;
    }
  }

  public int getStock(String productId) {
    return stock.getOrDefault(productId, 0);
  }
}
