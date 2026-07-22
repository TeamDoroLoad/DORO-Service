package com.doroload.api.route.infrastructure.redis;

import com.doroload.api.common.config.TmapProperties;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 동일 Route Cache Miss의 중복 TMAP 호출을 막기 위한 짧은 분산 Lock (구현 가이드 13.5). TTL 만료에 의존하며 명시적 해제는 하지 않는다.
@Component
public class RedisRouteLock {

    private final StringRedisTemplate redisTemplate;
    private final TmapProperties tmapProperties;

    public RedisRouteLock(StringRedisTemplate redisTemplate, TmapProperties tmapProperties) {
        this.redisTemplate = redisTemplate;
        this.tmapProperties = tmapProperties;
    }

    // Lock 획득에 성공하면 true. Redis 장애 시에도 안전하게 false를 반환한다(Fail-closed).
    public boolean tryLock(String routeCacheKey) {
        try {
            String lockKey = "lock:" + routeCacheKey;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, UUID.randomUUID().toString(), Duration.ofMillis(tmapProperties.routeLockTtlMillis()));
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
