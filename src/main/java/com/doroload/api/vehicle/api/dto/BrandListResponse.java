package com.doroload.api.vehicle.api.dto;

// GET /brands 응답 항목. 브랜드는 트림과 달리 수가 적어 Pagination 없이 전량 반환한다.
public record BrandListResponse(Long brandId, String brandName) {
}
