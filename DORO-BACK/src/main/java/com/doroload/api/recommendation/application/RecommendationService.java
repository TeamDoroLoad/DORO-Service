package com.doroload.api.recommendation.application;

import com.doroload.api.common.enums.ChargerStatus;
import com.doroload.api.common.enums.Freshness;
import com.doroload.api.common.error.ErrorCode;
import com.doroload.api.common.error.NotFoundException;
import com.doroload.api.common.freshness.FreshnessCalculator;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.pricing.EstimatedPricingPolicy;
import com.doroload.api.recommendation.CandidateAggregate;
import com.doroload.api.recommendation.RecommendationProperties;
import com.doroload.api.recommendation.SearchProperties;
import com.doroload.api.recommendation.api.dto.RecommendationRequest;
import com.doroload.api.recommendation.api.dto.RecommendationResponse;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.AvailabilityInfo;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.CandidateItem;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.CompatibilityInfo;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.DataSourceItem;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.NetworkInfo;
import com.doroload.api.recommendation.api.dto.RecommendationResponse.RouteInfo;
import com.doroload.api.route.application.RouteResolverService;
import com.doroload.api.route.domain.RouteStatus;
import com.doroload.api.route.domain.RouteSummary;
import com.doroload.api.station.domain.StationSourceLink;
import com.doroload.api.station.infrastructure.mysql.ChargerConnectorRow;
import com.doroload.api.station.infrastructure.mysql.LatestChargerStatusRepository;
import com.doroload.api.station.infrastructure.mysql.LatestStatusRow;
import com.doroload.api.station.infrastructure.mysql.StationCandidateRow;
import com.doroload.api.station.infrastructure.mysql.StationSourceLinkJpaRepository;
import com.doroload.api.station.infrastructure.mysql.StationSpatialRepository;
import com.doroload.api.vehicle.infrastructure.mysql.VehicleConnectorJpaRepository;
import com.doroload.api.vehicle.infrastructure.mysql.VehicleTrimJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 반경 3km 후보 조회 → 호환성·가용성 결합 → 규칙 기반 상위 3개 선정 → TMAP 경로 결합의 전체 Orchestrator (구현 가이드 9.4)
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final VehicleTrimJpaRepository vehicleTrimJpaRepository;
    private final VehicleConnectorJpaRepository vehicleConnectorJpaRepository;
    private final StationSpatialRepository stationSpatialRepository;
    private final LatestChargerStatusRepository latestChargerStatusRepository;
    private final StationSourceLinkJpaRepository stationSourceLinkJpaRepository;
    private final FreshnessCalculator freshnessCalculator;
    private final RecommendationScorer recommendationScorer;
    private final RouteResolverService routeResolverService;
    private final EstimatedPricingPolicy estimatedPricingPolicy;
    private final SearchProperties searchProperties;
    private final RecommendationProperties recommendationProperties;

    public RecommendationService(
            VehicleTrimJpaRepository vehicleTrimJpaRepository,
            VehicleConnectorJpaRepository vehicleConnectorJpaRepository,
            StationSpatialRepository stationSpatialRepository,
            LatestChargerStatusRepository latestChargerStatusRepository,
            StationSourceLinkJpaRepository stationSourceLinkJpaRepository,
            FreshnessCalculator freshnessCalculator,
            RecommendationScorer recommendationScorer,
            RouteResolverService routeResolverService,
            EstimatedPricingPolicy estimatedPricingPolicy,
            SearchProperties searchProperties,
            RecommendationProperties recommendationProperties) {
        this.vehicleTrimJpaRepository = vehicleTrimJpaRepository;
        this.vehicleConnectorJpaRepository = vehicleConnectorJpaRepository;
        this.stationSpatialRepository = stationSpatialRepository;
        this.latestChargerStatusRepository = latestChargerStatusRepository;
        this.stationSourceLinkJpaRepository = stationSourceLinkJpaRepository;
        this.freshnessCalculator = freshnessCalculator;
        this.recommendationScorer = recommendationScorer;
        this.routeResolverService = routeResolverService;
        this.estimatedPricingPolicy = estimatedPricingPolicy;
        this.searchProperties = searchProperties;
        this.recommendationProperties = recommendationProperties;
    }

    public Result recommend(RecommendationRequest request) {
        if (!vehicleTrimJpaRepository.existsById(request.vehicleTrimId())) {
            throw new NotFoundException(ErrorCode.VEHICLE_TRIM_NOT_FOUND, "요청한 차량 트림을 찾을 수 없습니다.");
        }

        String searchId = UUID.randomUUID().toString();
        GeoPoint center = request.location();
        List<String> compatibleCodes = vehicleConnectorJpaRepository.findConnectorCodesByTrimId(request.vehicleTrimId());
        if (compatibleCodes.isEmpty()) {
            return emptyResult(searchId, center);
        }

        List<StationCandidateRow> rows = stationSpatialRepository.findCandidates(
                center.toWkt(),
                center.boundingPolygonWkt(searchProperties.radiusMeters()),
                searchProperties.radiusMeters(),
                compatibleCodes,
                searchProperties.internalCandidateLimit());
        if (rows.isEmpty()) {
            return emptyResult(searchId, center);
        }

        List<CandidateAggregate> candidates = buildAggregates(rows, new HashSet<>(compatibleCodes), request.memberNetworkIds());
        if (candidates.isEmpty()) {
            return emptyResult(searchId, center);
        }

        List<Scored> topCandidates = candidates.stream()
                .map(c -> new Scored(c, recommendationScorer.score(c)))
                .sorted(Comparator
                        .comparing((Scored s) -> s.scoreResult.totalScore())
                        .reversed()
                        .thenComparing(s -> s.candidate.row().straightDistanceMeters())
                        .thenComparing(s -> s.candidate.row().stationId()))
                .limit(searchProperties.resultLimit())
                .toList();

        Map<Long, List<StationSourceLink>> sourceLinksByStation = fetchSourceLinks(topCandidates);

        boolean[] partialHolder = {false};
        List<String> metaWarnings = new ArrayList<>();
        List<CandidateItem> items = new ArrayList<>();
        int rank = 1;
        for (Scored scored : topCandidates) {
            RouteSummary route = routeResolverService.resolve(
                    center, scored.candidate.row().stationId(),
                    new GeoPoint(scored.candidate.row().latitude(), scored.candidate.row().longitude()));
            if (route.status() == RouteStatus.UNAVAILABLE) {
                partialHolder[0] = true;
                if (metaWarnings.isEmpty()) {
                    metaWarnings.add("자동차 이동시간을 조회하지 못해 직선 거리로 표시합니다.");
                }
            }
            items.add(toCandidateItem(rank++, scored, route, sourceLinksByStation));
        }

        log.info("추천 완료: searchId={}, policyVersion={}, candidateCount={}",
                searchId, recommendationProperties.policyVersion(), items.size());
        return new Result(
                new RecommendationResponse(searchId, center, searchProperties.radiusMeters(), items.size(), items),
                partialHolder[0],
                metaWarnings);
    }

    private List<CandidateAggregate> buildAggregates(
            List<StationCandidateRow> rows, Set<String> compatibleCodes, List<Long> memberNetworkIds) {
        List<Long> stationIds = rows.stream().map(StationCandidateRow::stationId).toList();
        List<ChargerConnectorRow> chargerConnectorRows = stationSpatialRepository.findChargerConnectorRows(stationIds);

        Map<Long, List<ChargerConnectorRow>> rowsByCharger =
                chargerConnectorRows.stream().collect(Collectors.groupingBy(ChargerConnectorRow::chargerId));
        Map<Long, Set<Long>> chargerIdsByStation = new java.util.HashMap<>();
        for (ChargerConnectorRow r : chargerConnectorRows) {
            chargerIdsByStation.computeIfAbsent(r.stationId(), k -> new LinkedHashSet<>()).add(r.chargerId());
        }

        Set<Long> allChargerIds = chargerConnectorRows.stream().map(ChargerConnectorRow::chargerId).collect(Collectors.toSet());
        Map<Long, LatestStatusRow> latestByCharger = latestChargerStatusRepository.findLatestByChargerIds(allChargerIds).stream()
                .collect(Collectors.toMap(LatestStatusRow::chargerId, Function.identity()));

        Instant now = Instant.now();
        List<CandidateAggregate> aggregates = new ArrayList<>();
        for (StationCandidateRow row : rows) {
            CandidateAggregate aggregate = buildAggregate(
                    row, chargerIdsByStation.getOrDefault(row.stationId(), Set.of()),
                    rowsByCharger, latestByCharger, compatibleCodes, memberNetworkIds, now);
            if (aggregate != null && !aggregate.allOutOfService()) {
                aggregates.add(aggregate);
            }
        }
        return aggregates;
    }

    private CandidateAggregate buildAggregate(
            StationCandidateRow row,
            Set<Long> chargerIds,
            Map<Long, List<ChargerConnectorRow>> rowsByCharger,
            Map<Long, LatestStatusRow> latestByCharger,
            Set<String> compatibleCodes,
            List<Long> memberNetworkIds,
            Instant now) {
        Set<String> matchedConnectorCodes = new LinkedHashSet<>();
        int available = 0;
        int busy = 0;
        int unknown = 0;
        int total = 0;
        BigDecimal bestPowerKw = null;
        Instant latestUpdated = null;
        Instant latestCollected = null;

        for (Long chargerId : chargerIds) {
            List<ChargerConnectorRow> connectorRows = rowsByCharger.getOrDefault(chargerId, List.of());
            List<String> ownMatchedCodes =
                    connectorRows.stream().map(ChargerConnectorRow::connectorCode).filter(compatibleCodes::contains).toList();
            if (ownMatchedCodes.isEmpty()) {
                continue;
            }
            total++;
            matchedConnectorCodes.addAll(ownMatchedCodes);

            BigDecimal power = connectorRows.get(0).maxPowerKw();
            if (power != null && (bestPowerKw == null || power.compareTo(bestPowerKw) > 0)) {
                bestPowerKw = power;
            }

            LatestStatusRow statusRow = latestByCharger.get(chargerId);
            ChargerStatus status = statusRow != null ? ChargerStatus.fromDb(statusRow.status()) : ChargerStatus.UNKNOWN;
            if (status == ChargerStatus.AVAILABLE) {
                available++;
            } else if (status == ChargerStatus.CHARGING || status == ChargerStatus.RESERVED) {
                busy++;
            } else if (!status.isOutOfService()) {
                unknown++;
            }
            if (statusRow != null) {
                if (statusRow.sourceUpdatedAt() != null && (latestUpdated == null || statusRow.sourceUpdatedAt().isAfter(latestUpdated))) {
                    latestUpdated = statusRow.sourceUpdatedAt();
                }
                if (statusRow.collectedAt() != null && (latestCollected == null || statusRow.collectedAt().isAfter(latestCollected))) {
                    latestCollected = statusRow.collectedAt();
                }
            }
        }

        if (total == 0) {
            return null;
        }

        Freshness freshness = freshnessCalculator.classify(latestUpdated, latestCollected, now);
        boolean membershipMatched = memberNetworkIds.contains(row.networkId());
        return new CandidateAggregate(
                row, List.copyOf(matchedConnectorCodes), available, busy, unknown, total,
                bestPowerKw, latestUpdated, latestCollected, freshness, membershipMatched);
    }

    private Map<Long, List<StationSourceLink>> fetchSourceLinks(List<Scored> topCandidates) {
        List<Long> stationIds = topCandidates.stream().map(s -> s.candidate.row().stationId()).toList();
        return stationSourceLinkJpaRepository.findByStationIds(stationIds).stream()
                .collect(Collectors.groupingBy(link -> link.getId().getStationId()));
    }

    private CandidateItem toCandidateItem(
            int rank, Scored scored, RouteSummary route, Map<Long, List<StationSourceLink>> sourceLinksByStation) {
        StationCandidateRow row = scored.candidate.row();
        NetworkInfo network = new NetworkInfo(
                row.networkId(), row.networkName(), row.operatorLegalName(), scored.candidate.membershipMatched());
        RouteInfo routeInfo = new RouteInfo(
                route.status().name(), route.distanceMeters(), route.durationSeconds(), route.provider(),
                route.calculatedAt(), route.cacheHit(), route.reasonCode());
        CompatibilityInfo compatibility = new CompatibilityInfo(true, scored.candidate.matchedConnectorCodes());
        AvailabilityInfo availability = new AvailabilityInfo(
                scored.candidate.availableCount(), scored.candidate.busyCount(), scored.candidate.unknownCount(),
                scored.candidate.totalCount(), scored.candidate.latestStatusUpdatedAt(),
                scored.candidate.latestCollectedAt(), scored.candidate.freshness().name());
        List<DataSourceItem> dataSources = sourceLinksByStation.getOrDefault(row.stationId(), List.of()).stream()
                .map(link -> new DataSourceItem(
                        link.getDataSource().getSourceId(), link.getDataSource().getSourceName(),
                        link.getDataSource().getSourceType(), link.getSourceStationId(), link.getMatchMethod(), link.getMatchScore()))
                .toList();

        return new CandidateItem(
                rank, row.stationId(), row.stationName(), row.address(), row.stationType(), row.operatingHours(),
                new GeoPoint(row.latitude(), row.longitude()), network, scored.scoreResult.totalScore(),
                scored.scoreResult.reasons(), scored.scoreResult.warnings(),
                (int) Math.round(row.straightDistanceMeters()), routeInfo, compatibility, availability,
                estimatedPricingPolicy.current(), dataSources);
    }

    private Result emptyResult(String searchId, GeoPoint center) {
        RecommendationResponse response =
                new RecommendationResponse(searchId, center, searchProperties.radiusMeters(), 0, List.of());
        return new Result(response, false, List.of("반경 3km 이내에서 선택 차량과 호환되는 충전소를 찾지 못했습니다."));
    }

    private record Scored(CandidateAggregate candidate, RecommendationScorer.Result scoreResult) {
    }

    public record Result(RecommendationResponse response, boolean partial, List<String> warnings) {
    }
}
