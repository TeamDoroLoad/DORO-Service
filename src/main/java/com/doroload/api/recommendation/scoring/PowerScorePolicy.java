package com.doroload.api.recommendation.scoring;

import com.doroload.api.recommendation.RecommendationProperties;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// 충전기 출력 점수 (구현 가이드 10.6). 차량 최대 입력이 ERD에 없으므로 호환 충전기 자체의 max_power_kw만 비교한다.
@Component
public class PowerScorePolicy {

    private static final BigDecimal FAST_THRESHOLD_KW = BigDecimal.valueOf(100);
    private static final BigDecimal MID_THRESHOLD_KW = BigDecimal.valueOf(50);

    private final int max;

    public PowerScorePolicy(RecommendationProperties properties) {
        this.max = properties.weights().powerMax();
    }

    public double score(BigDecimal bestCompatiblePowerKw) {
        if (bestCompatiblePowerKw == null) {
            return 0;
        }
        if (bestCompatiblePowerKw.compareTo(FAST_THRESHOLD_KW) >= 0) {
            return max;
        }
        if (bestCompatiblePowerKw.compareTo(MID_THRESHOLD_KW) >= 0) {
            return max * 0.6;
        }
        if (bestCompatiblePowerKw.signum() > 0) {
            return max * 0.3;
        }
        return 0;
    }
}
