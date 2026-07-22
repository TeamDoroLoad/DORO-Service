package com.doroload.api.vehicle.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.vehicle.api.dto.VehicleTrimListResponse;
import com.doroload.api.vehicle.api.dto.VehicleTrimListResponse.VehicleTrimItem;
import com.doroload.api.vehicle.application.VehicleTrimService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

// 차량 트림 검색·선택 (REST 명세서 6장)
@RestController
@Validated
public class VehicleTrimController {

    private final VehicleTrimService vehicleTrimService;

    public VehicleTrimController(VehicleTrimService vehicleTrimService) {
        this.vehicleTrimService = vehicleTrimService;
    }

    // 브랜드·모델·통합 검색어로 차량 트림 목록을 조회한다
    @GetMapping("/api/v1/vehicle-trims")
    public ResponseEntity<ApiResponse<List<VehicleTrimItem>>> search(
            @RequestParam(required = false) @Size(min = 1, max = 100) String brandName,
            @RequestParam(required = false) @Size(min = 1, max = 100) String modelName,
            @RequestParam(required = false) @Size(min = 2, max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        VehicleTrimListResponse response = vehicleTrimService.search(brandName, modelName, keyword, page, size);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic().staleWhileRevalidate(1, TimeUnit.DAYS))
                .body(ApiResponse.of(response.items(), RequestContext.currentRequestId()));
    }
}
