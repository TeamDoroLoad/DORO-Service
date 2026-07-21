package com.doroload.api.recommendation.api.dto;

import com.doroload.api.common.geo.GeoPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// POST /stations/recommendations 요청 (REST 명세서 9.1). radiusMeters·limit·connectorCodes는 Client가 보내지 않는다.
public record RecommendationRequest(
        @Valid @NotNull GeoPoint location,
        @NotNull Long vehicleTrimId,
        @Size(max = 20) List<Long> memberNetworkIds) {

    public RecommendationRequest {
        memberNetworkIds = memberNetworkIds == null ? List.of() : memberNetworkIds.stream().distinct().toList();
    }
}
