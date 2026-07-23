package com.doroload.api.station.application;

import com.doroload.api.common.enums.ChargerStatus;
import com.doroload.api.common.enums.Freshness;
import com.doroload.api.common.error.ErrorCode;
import com.doroload.api.common.error.NotFoundException;
import com.doroload.api.common.freshness.FreshnessCalculator;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.pricing.EstimatedPricingPolicy;
import com.doroload.api.station.api.dto.StationDetailResponse;
import com.doroload.api.station.api.dto.StationDetailResponse.ChargerItem;
import com.doroload.api.station.api.dto.StationDetailResponse.NetworkInfo;
import com.doroload.api.station.api.dto.StationDetailResponse.SourceLinkItem;
import com.doroload.api.station.domain.Charger;
import com.doroload.api.station.domain.Station;
import com.doroload.api.station.infrastructure.mysql.ChargerJpaRepository;
import com.doroload.api.station.infrastructure.mysql.LatestStatusRow;
import com.doroload.api.station.infrastructure.mysql.StationJpaRepository;
import com.doroload.api.station.infrastructure.mysql.StationSourceLinkJpaRepository;
import com.doroload.api.station.infrastructure.mysql.StationSpatialRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 충전소·충전기 상세 조회. 충전기 상태는 ChargerStatusResolver를 거쳐 Redis 우선·MySQL 폴백으로 조회하고,
// 이동시간은 추천 API에서 계산한 값을 재계산하지 않음
@Service
@Transactional(readOnly = true)
public class StationDetailService {

    private final StationJpaRepository stationJpaRepository;
    private final ChargerJpaRepository chargerJpaRepository;
    private final ChargerStatusResolver chargerStatusResolver;
    private final StationSourceLinkJpaRepository stationSourceLinkJpaRepository;
    private final StationSpatialRepository stationSpatialRepository;
    private final EstimatedPricingPolicy estimatedPricingPolicy;
    private final FreshnessCalculator freshnessCalculator;

    public StationDetailService(
            StationJpaRepository stationJpaRepository,
            ChargerJpaRepository chargerJpaRepository,
            ChargerStatusResolver chargerStatusResolver,
            StationSourceLinkJpaRepository stationSourceLinkJpaRepository,
            StationSpatialRepository stationSpatialRepository,
            EstimatedPricingPolicy estimatedPricingPolicy,
            FreshnessCalculator freshnessCalculator) {
        this.stationJpaRepository = stationJpaRepository;
        this.chargerJpaRepository = chargerJpaRepository;
        this.chargerStatusResolver = chargerStatusResolver;
        this.stationSourceLinkJpaRepository = stationSourceLinkJpaRepository;
        this.stationSpatialRepository = stationSpatialRepository;
        this.estimatedPricingPolicy = estimatedPricingPolicy;
        this.freshnessCalculator = freshnessCalculator;
    }

    public StationDetailResponse getDetail(Long stationId) {
        Station station = stationJpaRepository.findDetailById(stationId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.STATION_NOT_FOUND, "요청한 충전소를 찾을 수 없습니다."));

        List<Charger> chargers = chargerJpaRepository.findByStationIdWithConnectors(stationId);
        List<Long> chargerIds = chargers.stream().map(Charger::getChargerId).toList();
        Map<Long, LatestStatusRow> latestStatusByChargerId = chargerStatusResolver.resolve(chargerIds);

        Instant now = Instant.now();
        List<ChargerItem> chargerItems = chargers.stream()
                .map(charger -> toChargerItem(charger, latestStatusByChargerId.get(charger.getChargerId()), now))
                .toList();

        List<SourceLinkItem> sourceLinks = stationSourceLinkJpaRepository.findByStationId(stationId).stream()
                .map(link -> new SourceLinkItem(
                        link.getDataSource().getSourceId(),
                        link.getDataSource().getSourceName(),
                        link.getDataSource().getSourceType(),
                        link.getSourceStationId(),
                        link.getMatchMethod(),
                        link.getMatchScore()))
                .toList();

        NetworkInfo networkInfo = new NetworkInfo(
                station.getNetwork().getNetworkId(),
                station.getNetwork().getNetworkName(),
                station.getNetwork().getOperator().getLegalName());

        // ST_Latitude()/ST_Longitude()로 조회 — JPA Point.getX()/getY()는 MySQL 8 SRID 4326 축 순서 함정에 취약해 사용하지 않는다
        GeoPoint location = stationSpatialRepository.findLatLng(stationId);

        return new StationDetailResponse(
                station.getStationId(),
                station.getStationName(),
                station.getAddress(),
                station.getStationType(),
                location,
                station.getOperatingHours(),
                networkInfo,
                chargerItems,
                estimatedPricingPolicy.current(),
                sourceLinks,
                station.getCreatedAt());
    }

    private ChargerItem toChargerItem(Charger charger, LatestStatusRow latestStatus, Instant now) {
        List<String> connectorCodes = charger.getConnectors().stream()
                .map(cc -> cc.getConnectorType().getConnectorCode())
                .sorted()
                .toList();
        String status = latestStatus != null ? ChargerStatus.fromDb(latestStatus.status()).name() : ChargerStatus.UNKNOWN.name();
        Instant sourceUpdatedAt = latestStatus != null ? latestStatus.sourceUpdatedAt() : null;
        Instant collectedAt = latestStatus != null ? latestStatus.collectedAt() : null;
        Freshness freshness = freshnessCalculator.classify(sourceUpdatedAt, collectedAt, now);

        return new ChargerItem(
                charger.getChargerId(),
                charger.getExternalChargerId(),
                charger.getChargerName(),
                charger.getChargerType(),
                connectorCodes,
                charger.getMaxPowerKw(),
                status,
                sourceUpdatedAt,
                collectedAt,
                freshness.name());
    }
}
