package com.doroload.api.route.infrastructure.redis;

import java.time.Instant;

// Redis에 저장하는 Route Cache Value. 성공 결과만 저장하며 TTL이 지나면 자연 소멸한다.
public record RouteCacheValue(int distanceMeters, int durationSeconds, Instant calculatedAt) {
}
