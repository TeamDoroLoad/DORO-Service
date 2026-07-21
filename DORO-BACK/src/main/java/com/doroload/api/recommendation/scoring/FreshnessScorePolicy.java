package com.doroload.api.recommendation.scoring;

import com.doroload.api.common.enums.Freshness;
import com.doroload.api.recommendation.RecommendationProperties;
import org.springframework.stereotype.Component;

// 데이터 최신성 점수 (구현 가이드 10.7)
@Component
public class FreshnessScorePolicy {

    private final int max;

    public FreshnessScorePolicy(RecommendationProperties properties) {
        this.max = properties.weights().freshnessMax();
    }

    public double score(Freshness freshness) {
        return switch (freshness) {
            case FRESH -> max;
            case DELAYED -> max * 0.6;
            case STALE -> max * 0.2;
            case UNKNOWN -> 0;
        };
    }
}
