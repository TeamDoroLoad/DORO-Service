package com.doroload.api.route.infrastructure.tmap;

// TMAP 자동차 경로안내 요청 Body (구현 가이드 12.3)
public record TmapRouteRequest(
        double startX,
        double startY,
        double endX,
        double endY,
        String reqCoordType,
        String resCoordType,
        int searchOption,
        int totalValue) {
}
