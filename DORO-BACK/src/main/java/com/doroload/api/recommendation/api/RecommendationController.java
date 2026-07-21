package com.doroload.api.recommendation.api;

import com.doroload.api.common.web.ApiResponse;
import com.doroload.api.common.web.RequestContext;
import com.doroload.api.recommendation.api.dto.RecommendationRequest;
import com.doroload.api.recommendation.api.dto.RecommendationResponse;
import com.doroload.api.recommendation.application.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 충전소 검색·평가·상위 3개 선정·TMAP 이동시간 결합 핵심 API (REST 명세서 9장)
@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // 확정 위치·차량·회원 네트워크를 기준으로 최대 3개의 충전소 후보를 추천한다
    @PostMapping("/api/v1/stations/recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> recommend(@Valid @RequestBody RecommendationRequest request) {
        RecommendationService.Result result = recommendationService.recommend(request);
        String requestId = RequestContext.currentRequestId();
        ApiResponse<RecommendationResponse> body =
                ApiResponse.ofPartial(result.response(), requestId, result.partial(), result.warnings());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
