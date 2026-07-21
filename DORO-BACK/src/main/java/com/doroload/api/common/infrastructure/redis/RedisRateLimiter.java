package com.doroload.api.common.infrastructure.redis;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 분 단위 고정 창(Fixed Window) 기반 외부 API 호출 제한. 여러 Pod가 공유하도록 Redis Counter를 사용한다.
@Component
public class RedisRateLimiter {

    private static final Duration KEY_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // bucketKey 분당 limitPerMinute 회 호출까지 허용. Redis 장애 시 안전하게 Fail-closed(false) 처리한다.
    public boolean tryAcquire(String bucketKey, int limitPerMinute) {
        long windowEpochMinute = Instant.now().getEpochSecond() / 60;
        String key = "ratelimit:" + bucketKey + ":" + windowEpochMinute;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return false;
            }
            if (count == 1L) {
                redisTemplate.expire(key, KEY_TTL);
            }
            return count <= limitPerMinute;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
