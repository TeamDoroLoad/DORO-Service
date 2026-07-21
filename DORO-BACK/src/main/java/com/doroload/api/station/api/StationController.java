package com.doroload.api.station.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.station.api.dto.StationDetailResponse;
import com.doroload.api.station.application.StationDetailService;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// 충전소·충전기 상세 조회 (REST 명세서 10장)
@RestController
@Validated
public class StationController {

    private final StationDetailService stationDetailService;

    public StationController(StationDetailService stationDetailService) {
        this.stationDetailService = stationDetailService;
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
