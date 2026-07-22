package com.doroload.api.pricing;

import java.math.BigDecimal;

// 추천·충전소 상세 API가 공통으로 반환하는 예상 요금 Block (REST 명세서 9.1, 10.1)
public record EstimatedPricingResponse(
        String status, BigDecimal estimatedPricePerKwh, String currency, String policyVersion, String notice) {
}
