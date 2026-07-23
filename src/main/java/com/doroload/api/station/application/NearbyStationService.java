package com.doroload.api.station.application;

import com.doroload.api.common.enums.ChargerStatus;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.station.api.dto.NearbyStationsResponse;
import com.doroload.api.station.api.dto.NearbyStationsResponse.NearbyStationItem;
import com.doroload.api.station.api.dto.NearbyStationsResponse.NetworkInfo;
import com.doroload.api.station.api.dto.NearbyStationsResponse.PaginationInfo;
import com.doroload.api.station.api.dto.NearbyStationsResponse.SourceInfo;
import com.doroload.api.station.domain.StationSourceLink;
import com.doroload.api.station.infrastructure.mysql.ChargerConnectorRow;
import com.doroload.api.station.infrastructure.mysql.LatestStatusRow;
import com.doroload.api.station.infrastructure.mysql.StationCandidateRow;
import com.doroload.api.station.infrastructure.mysql.StationSourceLinkJpaRepository;
import com.doroload.api.station.infrastructure.mysql.StationSpatialRepository;
import com.doroload.api.vehicle.infrastructure.mysql.VehicleConnectorJpaRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 좌표 주변 충전소 검색 (REST 명세서 7.1, 구현 가이드 7.5~7.6). 결과가 0건인 것은 에러가 아니라
// 정상 200 OK + stations: [] 로 응답한다 (프론트 계약 — v0.2 문서 §2.5 참고).
@Service
@Transactional(readOnly = true)
public class NearbyStationService {

    // 구현 가이드 7.5 권장: 결과 상한을 내부적으로 둔다(초기 권장 200개)
    private static final int INTERNAL_CANDIDATE_LIMIT = 200;

    private final StationSpatialRepository stationSpatialRepository;
    private final ChargerStatusResolver chargerStatusResolver;
    private final StationSourceLinkJpaRepository stationSourceLinkJpaRepository;
    private final VehicleConnectorJpaRepository vehicleConnectorJpaRepository;

    public NearbyStationService(
            StationSpatialRepository stationSpatialRepository,
            ChargerStatusResolver chargerStatusResolver,
            StationSourceLinkJpaRepository stationSourceLinkJpaRepository,
            VehicleConnectorJpaRepository vehicleConnectorJpaRepository) {
        this.stationSpatialRepository = stationSpatialRepository;
        this.chargerStatusResolver = chargerStatusResolver;
        this.stationSourceLinkJpaRepository = stationSourceLinkJpaRepository;
        this.vehicleConnectorJpaRepository = vehicleConnectorJpaRepository;
    }

    public NearbyStationsResponse search(
            double latitude,
            double longitude,
            int radiusMeters,
            Long vehicleTrimId,
            List<String> connectorCodes,
            List<Long> networkIds,
            boolean availableOnly,
            int page,
            int size) {

        GeoPoint center = new GeoPoint(latitude, longitude);
        Set<String> effectiveConnectorCodes = resolveEffectiveConnectorCodes(vehicleTrimId, connectorCodes);
        if (effectiveConnectorCodes.isEmpty()) {
            return emptyResponse(center, radiusMeters, page, size);
        }

        List<StationCandidateRow> candidates = stationSpatialRepository.findCandidates(
                center.toWkt(), center.boundingPolygonWkt(radiusMeters), radiusMeters,
                effectiveConnectorCodes, INTERNAL_CANDIDATE_LIMIT);
        if (candidates.isEmpty()) {
            return emptyResponse(center, radiusMeters, page, size);
        }

        List<Long> stationIds = candidates.stream().map(StationCandidateRow::stationId).toList();

        Map<Long, List<ChargerConnectorRow>> connectorRowsByStation = stationSpatialRepository
                .findChargerConnectorRows(stationIds).stream()
                .collect(Collectors.groupingBy(ChargerConnectorRow::stationId));

        List<Long> allChargerIds = connectorRowsByStation.values().stream()
                .flatMap(List::stream)
                .map(ChargerConnectorRow::chargerId)
                .distinct()
                .toList();
        Map<Long, LatestStatusRow> latestStatusByChargerId = chargerStatusResolver.resolve(allChargerIds);

        Map<Long, StationSourceLink> primarySourceByStation = stationSourceLinkJpaRepository
                .findByStationIds(stationIds).stream()
                .collect(Collectors.toMap(
                        link -> link.getId().getStationId(),
                        Function.identity(),
                        NearbyStationService::preferHigherMatchScore));

        Set<Long> networkIdFilter = (networkIds == null || networkIds.isEmpty()) ? null : Set.copyOf(networkIds);

        List<NearbyStationItem> items = candidates.stream()
                .filter(row -> networkIdFilter == null || networkIdFilter.contains(row.networkId()))
                .map(row -> toItem(
                        row,
                        connectorRowsByStation.getOrDefault(row.stationId(), List.of()),
                        latestStatusByChargerId,
                        effectiveConnectorCodes,
                        primarySourceByStation.get(row.stationId())))
                .filter(item -> !availableOnly || item.availableCompatibleChargerCount() > 0)
                .toList();

        return paginate(center, radiusMeters, items, page, size);
    }

    // vehicleTrimId·connectorCodes가 둘 다 있으면 교집합, 하나만 있으면 그것을 그대로 사용한다 (REST 명세서 7.1)
    private Set<String> resolveEffectiveConnectorCodes(Long vehicleTrimId, List<String> connectorCodes) {
        Set<String> requested = (connectorCodes == null || connectorCodes.isEmpty())
                ? null
                : new LinkedHashSet<>(connectorCodes);
        if (vehicleTrimId != null) {
            Set<String> trimCodes = new LinkedHashSet<>(
                    vehicleConnectorJpaRepository.findConnectorCodesByTrimId(vehicleTrimId));
            if (requested != null) {
                trimCodes.retainAll(requested);
            }
            return trimCodes;
        }
        return requested == null ? Set.of() : requested;
    }

    private NearbyStationItem toItem(
            StationCandidateRow row,
            List<ChargerConnectorRow> connectorRows,
            Map<Long, LatestStatusRow> latestStatusByChargerId,
            Set<String> effectiveConnectorCodes,
            StationSourceLink sourceLink) {

        Map<Long, List<String>> connectorCodesByCharger = connectorRows.stream()
                .collect(Collectors.groupingBy(
                        ChargerConnectorRow::chargerId,
                        Collectors.mapping(ChargerConnectorRow::connectorCode, Collectors.toList())));

        Set<String> compatibleConnectorCodes = new TreeSet<>();
        Set<Long> compatibleChargerIds = new LinkedHashSet<>();
        for (Map.Entry<Long, List<String>> entry : connectorCodesByCharger.entrySet()) {
            List<String> matched = entry.getValue().stream().filter(effectiveConnectorCodes::contains).toList();
            if (!matched.isEmpty()) {
                compatibleChargerIds.add(entry.getKey());
                compatibleConnectorCodes.addAll(matched);
            }
        }

        int availableCompatibleChargerCount = (int) compatibleChargerIds.stream()
                .map(latestStatusByChargerId::get)
                .filter(status -> status != null && ChargerStatus.fromDb(status.status()).isAvailable())
                .count();

        // 충전기 상태 이력 기준 station 단위 최신 수집 시각 — data_source/station_source_link엔
        // 자체 타임스탬프가 없어서 charger_status_history의 값으로 대신한다.
        Instant latestStatusUpdatedAt = maxTimestamp(
                connectorCodesByCharger.keySet(), latestStatusByChargerId, LatestStatusRow::sourceUpdatedAt);
        Instant latestCollectedAt = maxTimestamp(
                connectorCodesByCharger.keySet(), latestStatusByChargerId, LatestStatusRow::collectedAt);

        String sourceName = sourceLink != null ? sourceLink.getDataSource().getSourceName() : null;

        return new NearbyStationItem(
                row.stationId(),
                row.stationName(),
                row.address(),
                new GeoPoint(row.latitude(), row.longitude()),
                row.straightDistanceMeters(),
                row.operatingHours(),
                new NetworkInfo(row.networkId(), row.networkName(), row.operatorLegalName()),
                !compatibleChargerIds.isEmpty(),
                List.copyOf(compatibleConnectorCodes),
                availableCompatibleChargerCount,
                compatibleChargerIds.size(),
                connectorCodesByCharger.size(),
                latestStatusUpdatedAt,
                new SourceInfo(sourceName, latestCollectedAt));
    }

    private static Instant maxTimestamp(
            Set<Long> chargerIds,
            Map<Long, LatestStatusRow> latestStatusByChargerId,
            Function<LatestStatusRow, Instant> extractor) {
        return chargerIds.stream()
                .map(latestStatusByChargerId::get)
                .filter(Objects::nonNull)
                .map(extractor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static StationSourceLink preferHigherMatchScore(StationSourceLink a, StationSourceLink b) {
        if (a.getMatchScore() == null) {
            return b;
        }
        if (b.getMatchScore() == null) {
            return a;
        }
        return a.getMatchScore().compareTo(b.getMatchScore()) >= 0 ? a : b;
    }

    private NearbyStationsResponse paginate(
            GeoPoint center, int radiusMeters, List<NearbyStationItem> items, int page, int size) {
        int totalElements = items.size();
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<NearbyStationItem> pageItems = items.subList(fromIndex, toIndex);
        boolean hasNext = toIndex < totalElements;
        return new NearbyStationsResponse(
                center, radiusMeters, pageItems, new PaginationInfo(page, size, totalElements, totalPages, hasNext));
    }

    private NearbyStationsResponse emptyResponse(GeoPoint center, int radiusMeters, int page, int size) {
        return new NearbyStationsResponse(center, radiusMeters, List.of(), new PaginationInfo(page, size, 0, 0, false));
    }
}
