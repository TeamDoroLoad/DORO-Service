package com.doroload.api.route.application;

import com.doroload.api.common.config.TmapProperties;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.common.infrastructure.redis.RedisRateLimiter;
import com.doroload.api.route.domain.RouteStatus;
import com.doroload.api.route.domain.RouteSummary;
import com.doroload.api.route.infrastructure.redis.RedisRouteCache;
import com.doroload.api.route.infrastructure.redis.RedisRouteLock;
import com.doroload.api.route.infrastructure.redis.RouteCacheValue;
import com.doroload.api.route.infrastructure.tmap.TmapRouteClient;
import java.util.Optional;
import org.springframework.stereotype.Service;

// Route Cache → Rate Limit → 짧은 Lock → TMAP 호출 순서로 처리하는 단일 창구 (구현 가이드 13.4)
@Service
public class RouteResolverService {

    private static final String RATE_LIMIT_BUCKET = "tmap:route";
    private static final long LOCK_WAIT_MILLIS = 150;

    private final RedisRouteCache routeCache;
    private final RedisRouteLock routeLock;
    private final RedisRateLimiter rateLimiter;
    private final TmapRouteClient tmapRouteClient;
    private final TmapProperties tmapProperties;

    public RouteResolverService(
            RedisRouteCache routeCache,
            RedisRouteLock routeLock,
            RedisRateLimiter rateLimiter,
            TmapRouteClient tmapRouteClient,
            TmapProperties tmapProperties) {
        this.routeCache = routeCache;
        this.routeLock = routeLock;
        this.rateLimiter = rateLimiter;
        this.tmapRouteClient = tmapRouteClient;
        this.tmapProperties = tmapProperties;
    }

    public RouteSummary resolve(GeoPoint origin, Long stationId, GeoPoint destination) {
        String cacheKey = routeCache.buildKey(origin.toCellKey(), stationId);

        Optional<RouteSummary> cached = readCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (!rateLimiter.tryAcquire(RATE_LIMIT_BUCKET, tmapProperties.rateLimit().routeLimitPerMinute())) {
            return RouteSummary.unavailable("TMAP_RATE_LIMITED");
        }

        if (!routeLock.tryLock(cacheKey)) {
            sleepBriefly();
            return readCache(cacheKey).orElseGet(() -> RouteSummary.unavailable("TMAP_LOCK_CONTENDED"));
        }

        RouteSummary summary = tmapRouteClient.getRouteSummary(origin, destination);
        if (summary.status() == RouteStatus.AVAILABLE) {
            routeCache.put(cacheKey, new RouteCacheValue(summary.distanceMeters(), summary.durationSeconds(), summary.calculatedAt()));
        }
        return summary;
    }

    private Optional<RouteSummary> readCache(String cacheKey) {
        return routeCache.get(cacheKey)
                .map(v -> RouteSummary.cached(v.distanceMeters(), v.durationSeconds(), v.calculatedAt()));
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(LOCK_WAIT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
