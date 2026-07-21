package com.doroload.api.route.infrastructure.tmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// TMAP 자동차 경로안내 응답 원본 구조. totalDistance·totalTime만 서비스 계약에 반영한다 (구현 가이드 14.2).
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmapRouteResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(Integer totalDistance, Integer totalTime) {
    }
}
