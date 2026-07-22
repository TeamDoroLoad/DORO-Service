package com.doroload.api.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 추천 점수 가중치. 실제 값은 환경설정으로 관리하며 Controller·SQL에 분산시키지 않는다 (구현 가이드 10.2).
@ConfigurationProperties(prefix = "doro.recommendation")
public record RecommendationProperties(String policyVersion, Weights weights) {

    public record Weights(
            int availabilityMax, int chargerCountMax, int membership, int powerMax, int freshnessMax, int distanceMax) {
    }
}
