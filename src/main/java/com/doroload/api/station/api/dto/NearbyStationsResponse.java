package com.doroload.api.station.api.dto;

import com.doroload.api.common.geo.GeoPoint;
import java.time.Instant;
import java.util.List;

// GET /stations/nearby 응답 (REST 명세서 7.1)
public record NearbyStationsResponse(
        GeoPoint center, int radiusMeters, List<NearbyStationItem> stations, PaginationInfo pagination) {

    public record NearbyStationItem(
            Long stationId,
            String stationName,
            String address,
            GeoPoint location,
            double straightDistanceMeters,
            String operatingHours,
            NetworkInfo network,
            boolean compatible,
            List<String> compatibleConnectorCodes,
            int availableCompatibleChargerCount,
            int totalCompatibleChargerCount,
            int totalChargerCount,
            Instant latestStatusUpdatedAt,
            SourceInfo source) {
    }

    public record NetworkInfo(Long networkId, String networkName, String operatorName) {
    }

    // sourceName이 null이면(연결된 원천 매칭이 없으면) source 자체를 null로 내려도 되지만,
    // 프론트가 source.sourceName을 그대로 읽으므로 필드 자체는 항상 유지한다.
    public record SourceInfo(String sourceName, Instant collectedAt) {
    }

    public record PaginationInfo(int page, int size, long totalElements, int totalPages, boolean hasNext) {
    }
}
