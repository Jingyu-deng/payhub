package com.payhub.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "processed:order:";
    private static final Duration TTL = Duration.ofHours(24); // keep record for 24h

    public boolean isAlreadyProcessed(String orderId) {
        String key = KEY_PREFIX + orderId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markAsProcessed(String orderId) {
        String key = KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(key, "1", TTL);
    }
}