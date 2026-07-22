package com.doroload.api.common.freshness;

import com.doroload.api.common.enums.Freshness;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

// source_updated_at을 우선 사용하고 없으면 collected_at으로 대체해 최신성을 계산 (구현 가이드 8.3)
@Component
public class FreshnessCalculator {

    private final FreshnessProperties properties;

    public FreshnessCalculator(FreshnessProperties properties) {
        this.properties = properties;
    }

    public Freshness classify(Instant sourceUpdatedAt, Instant collectedAt, Instant now) {
        Instant reference = sourceUpdatedAt != null ? sourceUpdatedAt : collectedAt;
        if (reference == null) {
            return Freshness.UNKNOWN;
        }
        long ageMinutes = Duration.between(reference, now).toMinutes();
        if (ageMinutes <= properties.freshMinutes()) {
            return Freshness.FRESH;
        }
        if (ageMinutes <= properties.delayedMinutes()) {
            return Freshness.DELAYED;
        }
        return Freshness.STALE;
    }
}
