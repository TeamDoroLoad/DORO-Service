package com.doroload.api.recommendation.scoring;

import com.doroload.api.recommendation.RecommendationProperties;
import org.springframework.stereotype.Component;

// 호환 충전기 수 점수 (구현 가이드 10.4). 이용 가능 수가 지나치게 점수를 독식하지 않도록 상한을 둔다.
@Component
public class ChargerCountScorePolicy {

    private final int max;

    public ChargerCountScorePolicy(RecommendationProperties properties) {
        this.max = properties.weights().chargerCountMax();
    }

    public double score(int availableCompatibleCount) {
        double perCharger = max / 4.0;
        return Math.min(availableCompatibleCount * perCharger, max);
    }
}
