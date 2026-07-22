package com.doroload.api.pricing;

import org.springframework.stereotype.Component;

// 모든 사업자·충전소에 동일한 공통 예상 단가를 조립하는 단일 창구.
// 이 값을 조회하는 모든 API가 반드시 이 Class를 통해서만 가격을 응답에 포함해야 한다 (350원/kWh Hard Coding 금지).
@Component
public class EstimatedPricingPolicy {

    private static final String NOTICE = "사업자별 실제 결제 요금이 아닌 서비스 공통 예상 단가입니다.";
    private static final String STATUS = "ESTIMATED";

    private final PricingProperties properties;

    public EstimatedPricingPolicy(PricingProperties properties) {
        this.properties = properties;
    }

    // 현재 설정값 기준 공통 예상 요금 응답 조립
    public EstimatedPricingResponse current() {
        return new EstimatedPricingResponse(
                STATUS, properties.estimatedPricePerKwh(), properties.currency(), properties.policyVersion(), NOTICE);
    }
}
