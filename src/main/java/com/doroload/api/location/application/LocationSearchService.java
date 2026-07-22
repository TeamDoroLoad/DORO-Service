package com.doroload.api.location.application;

import com.doroload.api.common.config.TmapProperties;
import com.doroload.api.common.error.RateLimitException;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.common.infrastructure.redis.RedisRateLimiter;
import com.doroload.api.location.api.dto.LocationSearchResponse;
import com.doroload.api.location.api.dto.LocationSearchResponse.LocationItem;
import com.doroload.api.location.infrastructure.tmap.TmapLocationClient;
import com.doroload.api.location.infrastructure.tmap.TmapPlace;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

// TMAP 주소·POI 검색을 Frontend 계약으로 정규화 (MySQL 미사용, Redis는 호출 속도 제한만 사용)
@Service
public class LocationSearchService {

    private static final String RATE_LIMIT_BUCKET = "tmap:location";

    private final TmapLocationClient tmapLocationClient;
    private final RedisRateLimiter rateLimiter;
    private final TmapProperties tmapProperties;

    public LocationSearchService(
            TmapLocationClient tmapLocationClient, RedisRateLimiter rateLimiter, TmapProperties tmapProperties) {
        this.tmapLocationClient = tmapLocationClient;
        this.rateLimiter = rateLimiter;
        this.tmapProperties = tmapProperties;
    }

    public LocationSearchResponse search(String query, int limit) {
        if (!rateLimiter.tryAcquire(RATE_LIMIT_BUCKET, tmapProperties.rateLimit().locationLimitPerMinute())) {
            throw new RateLimitException("위치 검색 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }
        AtomicInteger index = new AtomicInteger(0);
        var items = tmapLocationClient.search(query, limit).stream()
                .limit(limit)
                .map(place -> toItem(place, index.incrementAndGet()))
                .toList();
        return new LocationSearchResponse(items);
    }

    private LocationItem toItem(TmapPlace place, int index) {
        String placeId = "tmap:poi:" + (place.externalId() != null ? place.externalId() : index);
        String formattedAddress = place.roadAddress() != null ? place.roadAddress() : place.jibunAddress();
        GeoPoint location = new GeoPoint(place.latitude(), place.longitude());
        return new LocationItem(
                placeId, "POI", place.name(), formattedAddress, place.roadAddress(), place.jibunAddress(), location, "TMAP");
    }
}
