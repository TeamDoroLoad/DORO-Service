package com.doroload.api.station.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.station.api.dto.NearbyStationsResponse;
import com.doroload.api.station.api.dto.StationDetailResponse;
import com.doroload.api.station.application.NearbyStationService;
import com.doroload.api.station.application.StationDetailService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// 충전소·충전기 상세 조회 (REST 명세서 7·10장)
@RestController
@Validated
public class StationController {

    private final StationDetailService stationDetailService;
    private final NearbyStationService nearbyStationService;

    public StationController(StationDetailService stationDetailService, NearbyStationService nearbyStationService) {
        this.stationDetailService = stationDetailService;
        this.nearbyStationService = nearbyStationService;
    }

    // 좌표 주변 충전소 검색 (REST 명세서 7.1) — 결과 0건도 정상 200 OK
    @GetMapping("/api/v1/stations/nearby")
    public ResponseEntity<ApiResponse<NearbyStationsResponse>> searchNearby(
            @RequestParam @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @RequestParam @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @RequestParam(defaultValue = "3000") @Min(100) @Max(10000) int radiusMeters,
            @RequestParam(required = false) Long vehicleTrimId,
            @RequestParam(required = false) List<String> connectorCodes,
            @RequestParam(required = false) List<Long> networkIds,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        NearbyStationsResponse response = nearbyStationService.search(
                latitude, longitude, radiusMeters, vehicleTrimId, connectorCodes, networkIds, availableOnly, page,
                size);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.of(response, RequestContext.currentRequestId()));
    }

    // 추천 Card 또는 지도 Marker 선택 시 충전소·충전기 상세를 조회한다
    @GetMapping("/api/v1/stations/{stationId}")
    public ResponseEntity<ApiResponse<StationDetailResponse>> getDetail(@PathVariable @Min(1) Long stationId) {
        StationDetailResponse response = stationDetailService.getDetail(stationId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.of(response, RequestContext.currentRequestId()));
    }
}
