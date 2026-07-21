package com.doroload.api.recommendation.api.dto;

import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.pricing.EstimatedPricingResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// POST /stations/recommendations 응답 (REST 명세서 9.1)
public record RecommendationResponse(
        String searchId, GeoPoint center, int radiusMeters, int candidateCount, List<CandidateItem> candidates) {

    public record CandidateItem(
            int rank,
            Long stationId,
            String stationName,
            String address,
            String stationType,
            String operatingHours,
            GeoPoint location,
            NetworkInfo network,
            BigDecimal score,
            List<String> reasons,
            List<String> warnings,
            int straightDistanceMeters,
            RouteInfo route,
            CompatibilityInfo compatibility,
            AvailabilityInfo availability,
            EstimatedPricingResponse estimatedPricing,
            List<DataSourceItem> dataSources) {
    }

    public record NetworkInfo(Long networkId, String networkName, String operatorLegalName, boolean membershipMatched) {
    }

    public record RouteInfo(
            String status,
            Integer distanceMeters,
            Integer durationSeconds,
            String provider,
            Instant calculatedAt,
            boolean cacheHit,
            String reasonCode) {
    }

    public record CompatibilityInfo(boolean compatible, List<String> matchedConnectorCodes) {
    }

    public record AvailabilityInfo(
            int availableCompatibleChargers,
            int busyCompatibleChargers,
            int unknownCompatibleChargers,
            int totalCompatibleChargers,
            Instant latestStatusUpdatedAt,
            Instant latestCollectedAt,
            String freshness) {
    }

    public record DataSourceItem(
            Long sourceId, String sourceName, String sourceType, String sourceStationId, String matchMethod, BigDecimal matchScore) {
    }
}
