package com.doroload.api.station.infrastructure.redis;

import com.doroload.api.station.infrastructure.mysql.LatestStatusRow;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// ev-charger-collector fast 배치(10분 주기)가 갱신하는 charger:status:{chargerId} Hash를 읽는
// read-through 캐시. TTL(45분) 안에서만 값이 있고, 그 밖은 자연스러운 Cache Miss다.
// Redis 장애도 예외 대신 빈 Map으로 흡수해 호출부가 MySQL(ChargerStatusResolver)로 폴백하게 한다.
@Component
public class RedisChargerStatusCache {

    private static final Logger log = LoggerFactory.getLogger(RedisChargerStatusCache.class);

    private static final String STATUS_FIELD = "status";
    private static final String SOURCE_UPDATED_AT_FIELD = "source_updated_at";
    private static final String COLLECTED_AT_FIELD = "collected_at";

    private final StringRedisTemplate redisTemplate;

    public RedisChargerStatusCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String buildKey(Long chargerId) {
        return "charger:status:" + chargerId;
    }

    // Nearby 검색은 최대 200개 charger를 한 번에 조회하므로, Redis Round Trip을 Pipeline 하나로 묶는다
    public Map<Long, LatestStatusRow> findByChargerIds(Collection<Long> chargerIds) {
        if (chargerIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = List.copyOf(chargerIds);
        try {
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringConnection = (StringRedisConnection) connection;
                for (Long chargerId : ids) {
                    stringConnection.hGetAll(buildKey(chargerId));
                }
                return null;
            });
            Map<Long, LatestStatusRow> found = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                @SuppressWarnings("unchecked")
                Map<String, String> fields = (Map<String, String>) results.get(i);
                LatestStatusRow row = toRow(ids.get(i), fields);
                if (row != null) {
                    found.put(ids.get(i), row);
                }
            }
            return found;
        } catch (Exception e) {
            log.warn("Redis charger status 조회 실패, MySQL로 폴백합니다: {}", e.toString());
            return Map.of();
        }
    }

    private LatestStatusRow toRow(Long chargerId, Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        String status = fields.get(STATUS_FIELD);
        if (status == null || status.isBlank()) {
            return null;
        }
        return new LatestStatusRow(
                chargerId,
                status,
                parseInstant(fields.get(SOURCE_UPDATED_AT_FIELD)),
                parseInstant(fields.get(COLLECTED_AT_FIELD)));
    }

    // collector는 RFC3339(UTC)로 기록하고, 소스 미제공이면 빈 문자열을 남긴다 (cache.go formatTime 참고)
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
