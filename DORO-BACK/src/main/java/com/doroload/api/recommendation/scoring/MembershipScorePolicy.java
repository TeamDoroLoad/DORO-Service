package com.doroload.api.recommendation.scoring;

import com.doroload.api.recommendation.RecommendationProperties;
import org.springframework.stereotype.Component;

// 회원 네트워크 일치 점수 (구현 가이드 10.5). Roaming·비회원 가능 여부는 ERD에 없어 단순 일치만 판단한다.
@Component
public class MembershipScorePolicy {

    private final int score;

    public MembershipScorePolicy(RecommendationProperties properties) {
        this.score = properties.weights().membership();
    }

    public double score(boolean membershipMatched) {
        return membershipMatched ? score : 0;
    }
}
