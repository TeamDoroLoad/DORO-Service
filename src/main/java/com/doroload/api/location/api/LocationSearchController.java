package com.doroload.api.location.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.location.api.dto.LocationSearchResponse;
import com.doroload.api.location.application.LocationSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TMAP 주소·POI 통합 검색 Proxy (REST 명세서 8장)
@RestController
@Validated
public class LocationSearchController {

    private final LocationSearchService locationSearchService;

    public LocationSearchController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }

    // 검색어로 주소·장소를 검색해 지도 1차 이동 기준 위치를 제공한다
    @GetMapping("/api/v1/locations/search")
    public ResponseEntity<ApiResponse<LocationSearchResponse>> search(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String query,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit) {
        LocationSearchResponse response = locationSearchService.search(query.trim(), limit);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.of(response, RequestContext.currentRequestId()));
    }
}
