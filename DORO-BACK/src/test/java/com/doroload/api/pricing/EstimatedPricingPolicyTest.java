package com.doroload.api.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EstimatedPricingPolicyTest {

    @Test
    void current_reflectsConfiguredValue_withoutHardCoding() {
        PricingProperties properties = new PricingProperties(BigDecimal.valueOf(350), "KRW", "estimated-rate-2026-07-v1");
        EstimatedPricingPolicy policy = new EstimatedPricingPolicy(properties);

        EstimatedPricingResponse response = policy.current();

        assertThat(response.status()).isEqualTo("ESTIMATED");
        assertThat(response.estimatedPricePerKwh()).isEqualByComparingTo("350");
        assertThat(response.currency()).isEqualTo("KRW");
        assertThat(response.policyVersion()).isEqualTo("estimated-rate-2026-07-v1");
        assertThat(response.notice()).isNotBlank();
    }

    // 단가 설정이 바뀌면 응답도 그대로 반영되어야 한다 (Controller·Frontend Hard Coding 금지 검증)
    @Test
    void current_changesWhenConfigurationChanges() {
        PricingProperties properties = new PricingProperties(BigDecimal.valueOf(400), "KRW", "estimated-rate-2027-01-v2");
        EstimatedPricingPolicy policy = new EstimatedPricingPolicy(properties);

        EstimatedPricingResponse response = policy.current();

        assertThat(response.estimatedPricePerKwh()).isEqualByComparingTo("400");
        assertThat(response.policyVersion()).isEqualTo("estimated-rate-2027-01-v2");
    }
}
