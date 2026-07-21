package com.doroload.api.recommendation.scoring;

import com.doroload.api.recommendation.RecommendationProperties;
import com.doroload.api.recommendation.SearchProperties;
import org.springframework.stereotype.Component;

// 직선거리 점수 (구현 가이드 10.8). 0~3km를 선형으로 5~0점에 대응한다. TMAP 경로 결과는 정렬에 사용하지 않는다.
@Component
public class DistanceScorePolicy {

    private final int max;
    private final int radiusMeters;

    public DistanceScorePolicy(RecommendationProperties recommendationProperties, SearchProperties searchProperties) {
        this.max = recommendationProperties.weights().distanceMax();
        this.radiusMeters = searchProperties.radiusMeters();
    }

    public double score(double straightDistanceMeters) {
        double ratio = 1 - (straightDistanceMeters / radiusMeters);
        double clamped = Math.max(0, Math.min(1, ratio));
        return max * clamped;
    }
}
