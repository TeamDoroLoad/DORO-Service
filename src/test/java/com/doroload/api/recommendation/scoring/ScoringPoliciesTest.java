package com.doroload.api.recommendation.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.doroload.api.common.enums.Freshness;
import com.doroload.api.recommendation.RecommendationProperties;
import com.doroload.api.recommendation.RecommendationProperties.Weights;
import com.doroload.api.recommendation.SearchProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// 구현 가이드 10.3~10.8 기본 배점 표를 검증
class ScoringPoliciesTest {

    private final RecommendationProperties properties = new RecommendationProperties("v1", new Weights(40, 20, 15, 10, 10, 5));

    @Test
    void availabilityScore_matchesDocumentedDefaults() {
        AvailabilityScorePolicy policy = new AvailabilityScorePolicy(properties);

        assertThat(policy.score(2, 0, false)).isEqualTo(40);
        assertThat(policy.score(1, 0, false)).isEqualTo(30);
        assertThat(policy.score(0, 1, false)).isEqualTo(10);
        assertThat(policy.score(0, 0, true)).isEqualTo(5);
        assertThat(policy.score(0, 0, false)).isZero();
    }

    @Test
    void chargerCountScore_isCappedAtMax() {
        ChargerCountScorePolicy policy = new ChargerCountScorePolicy(properties);

        assertThat(policy.score(1)).isCloseTo(5, within(0.01));
        assertThat(policy.score(10)).isEqualTo(20);
    }

    @Test
    void membershipScore_onlyAwardedOnMatch() {
        MembershipScorePolicy policy = new MembershipScorePolicy(properties);

        assertThat(policy.score(true)).isEqualTo(15);
        assertThat(policy.score(false)).isZero();
    }

    @Test
    void powerScore_risesWithChargerOutput() {
        PowerScorePolicy policy = new PowerScorePolicy(properties);

        assertThat(policy.score(BigDecimal.valueOf(100))).isEqualTo(10);
        assertThat(policy.score(BigDecimal.valueOf(50))).isCloseTo(6, within(0.01));
        assertThat(policy.score(null)).isZero();
    }

    @Test
    void freshnessScore_matchesDocumentedDefaults() {
        FreshnessScorePolicy policy = new FreshnessScorePolicy(properties);

        assertThat(policy.score(Freshness.FRESH)).isEqualTo(10);
        assertThat(policy.score(Freshness.DELAYED)).isCloseTo(6, within(0.01));
        assertThat(policy.score(Freshness.STALE)).isCloseTo(2, within(0.01));
        assertThat(policy.score(Freshness.UNKNOWN)).isZero();
    }

    @Test
    void distanceScore_favorsCloserStations() {
        DistanceScorePolicy policy = new DistanceScorePolicy(properties, new SearchProperties(3000, 3, 200));

        assertThat(policy.score(0)).isEqualTo(5);
        assertThat(policy.score(3000)).isZero();
        assertThat(policy.score(1500)).isCloseTo(2.5, within(0.01));
    }
}
