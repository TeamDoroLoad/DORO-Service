package com.doroload.api.station.application;

import com.doroload.api.station.infrastructure.mysql.LatestChargerStatusRepository;
import com.doroload.api.station.infrastructure.mysql.LatestStatusRow;
import com.doroload.api.station.infrastructure.redis.RedisChargerStatusCache;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

// charger 최신 상태 조회 창구. Redis(collector fast 배치, 최대 10분 이내 신선도)를 먼저 보고,
// 캐시에 없는 charger(TTL 만료, fast 배치가 아직 못 본 신규 charger 등)만 MySQL
// (charger_status_history, collector slow 배치 기준 최대 2시간 이내 신선도)로 보충한다.
@Service
public class ChargerStatusResolver {

    private final RedisChargerStatusCache redisChargerStatusCache;
    private final LatestChargerStatusRepository latestChargerStatusRepository;

    public ChargerStatusResolver(
            RedisChargerStatusCache redisChargerStatusCache, LatestChargerStatusRepository latestChargerStatusRepository) {
        this.redisChargerStatusCache = redisChargerStatusCache;
        this.latestChargerStatusRepository = latestChargerStatusRepository;
    }

    public Map<Long, LatestStatusRow> resolve(Collection<Long> chargerIds) {
        if (chargerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, LatestStatusRow> fromRedis = redisChargerStatusCache.findByChargerIds(chargerIds);
        List<Long> missingIds = chargerIds.stream().filter(id -> !fromRedis.containsKey(id)).toList();
        if (missingIds.isEmpty()) {
            return fromRedis;
        }

        Map<Long, LatestStatusRow> merged = new HashMap<>(fromRedis);
        latestChargerStatusRepository.findLatestByChargerIds(missingIds)
                .forEach(row -> merged.put(row.chargerId(), row));
        return merged;
    }
}
