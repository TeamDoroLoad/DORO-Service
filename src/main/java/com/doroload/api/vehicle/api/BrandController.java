package com.doroload.api.vehicle.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.vehicle.api.dto.BrandListResponse;
import com.doroload.api.vehicle.application.BrandService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 차량 브랜드 목록 조회 — 브랜드 드롭다운은 트림 Pagination에 의존하지 않고 이 Endpoint로 전량 채운다
@RestController
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/api/v1/brands")
    public ResponseEntity<ApiResponse<List<BrandListResponse>>> list() {
        List<BrandListResponse> brands = brandService.findAll();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic().staleWhileRevalidate(1, TimeUnit.DAYS))
                .body(ApiResponse.of(brands, RequestContext.currentRequestId()));
    }
}
