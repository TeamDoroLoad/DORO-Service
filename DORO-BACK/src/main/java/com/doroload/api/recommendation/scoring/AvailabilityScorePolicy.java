package com.doroload.api.recommendation.scoring;

import com.doroload.api.recommendation.RecommendationProperties;
import org.springframework.stereotype.Component;

// 현재 이용 가능 여부 점수 (구현 가이드 10.3). 배점은 설정된 최댓값 기준으로 비례 산정한다.
@Component
public class AvailabilityScorePolicy {

    private final int max;

    public AvailabilityScorePolicy(RecommendationProperties properties) {
        this.max = properties.weights().availabilityMax();
    }

    public double score(int availableCompatibleCount, int busyCompatibleCount, boolean anyUnknown) {
        if (availableCompatibleCount >= 2) {
            return max;
        }
        if (availableCompatibleCount == 1) {
            return max * 0.75;
        }
        if (busyCompatibleCount > 0) {
            return max * 0.25;
        }
        if (anyUnknown) {
            return max * 0.125;
        }
        return 0;
    }
}
