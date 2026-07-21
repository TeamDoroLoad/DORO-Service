package com.doroload.api.recommendation.application;

import com.doroload.api.common.enums.Freshness;
import com.doroload.api.recommendation.CandidateAggregate;
import com.doroload.api.recommendation.scoring.AvailabilityScorePolicy;
import com.doroload.api.recommendation.scoring.ChargerCountScorePolicy;
import com.doroload.api.recommendation.scoring.DistanceScorePolicy;
import com.doroload.api.recommendation.scoring.FreshnessScorePolicy;
import com.doroload.api.recommendation.scoring.MembershipScorePolicy;
import com.doroload.api.recommendation.scoring.PowerScorePolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// 6개 독립 Score Policy를 합산하고 사용자 노출용 추천 근거·경고 문구를 조립 (구현 가이드 10.2, 10.9)
@Component
public class RecommendationScorer {

    private final AvailabilityScorePolicy availabilityScorePolicy;
    private final ChargerCountScorePolicy chargerCountScorePolicy;
    private final MembershipScorePolicy membershipScorePolicy;
    private final PowerScorePolicy powerScorePolicy;
    private final FreshnessScorePolicy freshnessScorePolicy;
    private final DistanceScorePolicy distanceScorePolicy;

    public RecommendationScorer(
            AvailabilityScorePolicy availabilityScorePolicy,
            ChargerCountScorePolicy chargerCountScorePolicy,
            MembershipScorePolicy membershipScorePolicy,
            PowerScorePolicy powerScorePolicy,
            FreshnessScorePolicy freshnessScorePolicy,
            DistanceScorePolicy distanceScorePolicy) {
        this.availabilityScorePolicy = availabilityScorePolicy;
        this.chargerCountScorePolicy = chargerCountScorePolicy;
        this.membershipScorePolicy = membershipScorePolicy;
        this.powerScorePolicy = powerScorePolicy;
        this.freshnessScorePolicy = freshnessScorePolicy;
        this.distanceScorePolicy = distanceScorePolicy;
    }

    public record Result(BigDecimal totalScore, List<String> reasons, List<String> warnings) {
    }

    public Result score(CandidateAggregate candidate) {
        double total = availabilityScorePolicy.score(candidate.availableCount(), candidate.busyCount(), candidate.unknownCount() > 0)
                + chargerCountScorePolicy.score(candidate.availableCount())
                + membershipScorePolicy.score(candidate.membershipMatched())
                + powerScorePolicy.score(candidate.bestCompatiblePowerKw())
                + freshnessScorePolicy.score(candidate.freshness())
                + distanceScorePolicy.score(candidate.row().straightDistanceMeters());

        BigDecimal roundedScore = BigDecimal.valueOf(total).setScale(1, RoundingMode.HALF_UP);
        return new Result(roundedScore, buildReasons(candidate), buildWarnings(candidate));
    }

    private List<String> buildReasons(CandidateAggregate candidate) {
        List<String> reasons = new ArrayList<>();
        reasons.add("선택 차량과 커넥터가 호환됩니다.");
        if (candidate.availableCount() > 0) {
            reasons.add("현재 이용 가능한 호환 충전기가 " + candidate.availableCount() + "기 있습니다.");
        }
        if (candidate.membershipMatched()) {
            reasons.add("회원 사업자에 일치합니다.");
        }
        if (candidate.freshness() == Freshness.FRESH) {
            reasons.add("충전기 상태 데이터가 최근에 갱신되었습니다.");
        }
        return reasons;
    }

    private List<String> buildWarnings(CandidateAggregate candidate) {
        List<String> warnings = new ArrayList<>();
        if (candidate.availableCount() == 0 && candidate.busyCount() > 0) {
            warnings.add("현재 이용 가능한 충전기가 없어 대기가 필요할 수 있습니다.");
        }
        if (candidate.freshness() == Freshness.STALE || candidate.freshness() == Freshness.UNKNOWN) {
            warnings.add("충전기 상태 정보가 오래되었거나 확인되지 않았습니다.");
        }
        return warnings;
    }
}
