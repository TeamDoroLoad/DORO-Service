package com.doroload.api.network.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.network.api.dto.ChargingNetworkListResponse;
import com.doroload.api.network.application.ChargingNetworkService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 충전 네트워크·사업자 조회 (REST 명세서 7장)
@RestController
public class ChargingNetworkController {

    private final ChargingNetworkService chargingNetworkService;

    public ChargingNetworkController(ChargingNetworkService chargingNetworkService) {
        this.chargingNetworkService = chargingNetworkService;
    }

    // 네트워크·브랜드 이름으로 충전 네트워크 목록을 조회한다
    @GetMapping("/api/v1/charging-networks")
    public ResponseEntity<ApiResponse<ChargingNetworkListResponse>> search(
            @RequestParam(required = false) String keyword) {
        ChargingNetworkListResponse response = chargingNetworkService.search(keyword);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic().staleWhileRevalidate(1, TimeUnit.DAYS))
                .body(ApiResponse.of(response, RequestContext.currentRequestId()));
    }
}
