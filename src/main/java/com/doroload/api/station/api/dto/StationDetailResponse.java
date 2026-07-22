package com.doroload.api.station.api.dto;

import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.pricing.EstimatedPricingResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// GET /stations/{stationId} 응답 (REST 명세서 10.1)
public record StationDetailResponse(
        Long stationId,
        String stationName,
        String address,
        String stationType,
        GeoPoint location,
        String operatingHours,
        NetworkInfo network,
        List<ChargerItem> chargers,
        EstimatedPricingResponse estimatedPricing,
        List<SourceLinkItem> sourceLinks,
        Instant createdAt) {

    public record NetworkInfo(Long networkId, String networkName, String operatorLegalName) {
    }

    public record ChargerItem(
            Long chargerId,
            String externalChargerId,
            String chargerName,
            String chargerType,
            List<String> connectorCodes,
            BigDecimal maxPowerKw,
            String currentStatus,
            Instant sourceUpdatedAt,
            Instant collectedAt,
            String freshness) {
    }

    public record SourceLinkItem(
            Long sourceId,
            String sourceName,
            String sourceType,
            String sourceStationId,
            String matchMethod,
            BigDecimal matchScore) {
    }
}
