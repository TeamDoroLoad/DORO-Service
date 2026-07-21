package com.doroload.api.pricing;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 모든 사업자에 동일하게 적용되는 공통 예상 단가 설정 (구현 가이드 11.2)
@ConfigurationProperties(prefix = "doro.pricing")
public record PricingProperties(BigDecimal estimatedPricePerKwh, String currency, String policyVersion) {
}
