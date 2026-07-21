package com.doroload.api.station.infrastructure.mysql;

// 반경 3km 공간 검색 결과 한 행 (충전소 + 소속 Network·Operator + 직선거리)
public record StationCandidateRow(
        Long stationId,
        String stationName,
        String address,
        String stationType,
        String operatingHours,
        double latitude,
        double longitude,
        Long networkId,
        String networkName,
        String operatorLegalName,
        double straightDistanceMeters) {
}
