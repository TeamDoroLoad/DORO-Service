package com.doroload.api.common.freshness;

import static org.assertj.core.api.Assertions.assertThat;

import com.doroload.api.common.enums.Freshness;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FreshnessCalculatorTest {

    private final FreshnessCalculator calculator = new FreshnessCalculator(new FreshnessProperties(10, 30));
    private final Instant now = Instant.parse("2026-07-21T07:30:00Z");

    @Test
    void classify_returnsFresh_withinThreshold() {
        Instant updatedAt = now.minusSeconds(9 * 60);

        assertThat(calculator.classify(updatedAt, null, now)).isEqualTo(Freshness.FRESH);
    }

    @Test
    void classify_returnsDelayed_betweenThresholds() {
        Instant updatedAt = now.minusSeconds(20 * 60);

        assertThat(calculator.classify(updatedAt, null, now)).isEqualTo(Freshness.DELAYED);
    }

    @Test
    void classify_returnsStale_beyondDelayedThreshold() {
        Instant updatedAt = now.minusSeconds(31 * 60);

        assertThat(calculator.classify(updatedAt, null, now)).isEqualTo(Freshness.STALE);
    }

    @Test
    void classify_fallsBackToCollectedAt_whenSourceUpdatedAtMissing() {
        Instant collectedAt = now.minusSeconds(5 * 60);

        assertThat(calculator.classify(null, collectedAt, now)).isEqualTo(Freshness.FRESH);
    }

    @Test
    void classify_returnsUnknown_whenBothTimestampsMissing() {
        assertThat(calculator.classify(null, null, now)).isEqualTo(Freshness.UNKNOWN);
    }
}
