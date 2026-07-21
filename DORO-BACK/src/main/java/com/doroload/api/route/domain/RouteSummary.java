package com.doroload.api.route.domain;

import java.time.Instant;

// TMAP 자동차 경로 요약 (REST 명세서 9.1 route Block)
public record RouteSummary(
        RouteStatus status,
        Integer distanceMeters,
        Integer durationSeconds,
        String provider,
        Instant calculatedAt,
        boolean cacheHit,
        String reasonCode) {

    public static RouteSummary cached(Integer distanceMeters, Integer durationSeconds, Instant calculatedAt) {
        return new RouteSummary(RouteStatus.AVAILABLE, distanceMeters, durationSeconds, "TMAP", calculatedAt, true, null);
    }

    public static RouteSummary fresh(Integer distanceMeters, Integer durationSeconds, Instant calculatedAt) {
        return new RouteSummary(RouteStatus.AVAILABLE, distanceMeters, durationSeconds, "TMAP", calculatedAt, false, null);
    }

    public static RouteSummary unavailable(String reasonCode) {
        return new RouteSummary(RouteStatus.UNAVAILABLE, null, null, "TMAP", null, false, reasonCode);
    }
}
