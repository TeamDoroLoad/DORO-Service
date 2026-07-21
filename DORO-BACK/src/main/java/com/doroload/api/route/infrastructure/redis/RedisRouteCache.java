package com.doroload.api.route.infrastructure.redis;

import com.doroload.api.common.config.TmapProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// TMAP 경로 결과 단기 캐시. MySQL을 대체하지 않는 보조 저장소이며 성공 응답만 TTL과 함께 저장한다 (구현 가이드 13.3).
@Component
public class RedisRouteCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TmapProperties tmapProperties;

    public RedisRouteCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, TmapProperties tmapProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tmapProperties = tmapProperties;
    }

    public String buildKey(String originCell, Long stationId) {
        return "route:v1:" + originCell + ":" + stationId + ":option0";
    }

    // Cache 조회 실패(장애 포함)는 예외 대신 빈 값으로 흡수해 호출부가 Cache Miss와 동일하게 처리하게 한다
    public Optional<RouteCacheValue> get(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, RouteCacheValue.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // TMAP 성공 응답만 설정된 TTL(180~300초 권장)로 저장한다
    public void put(String key, RouteCacheValue value) {
        try {
            String raw = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, raw, Duration.ofSeconds(tmapProperties.routeCacheTtlSeconds()));
        } catch (Exception e) {
            // Cache 저장 실패는 다음 요청에서 Cache Miss로 자연스럽게 재시도되므로 무시한다
        }
    }
}
