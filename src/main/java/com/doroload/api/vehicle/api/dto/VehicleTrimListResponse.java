package com.doroload.api.vehicle.api.dto;

import java.math.BigDecimal;
import java.util.List;

// GET /vehicle-trims 응답. 프론트(doro-load-web)가 페이지네이션 없이 flat 배열을 그대로 소비하므로
// items만 컨트롤러가 꺼내 응답 바디(data)로 내려주고, page/size/totalElements/totalPages는
// 서버 내부 로깅/향후 페이지네이션 대응 검토용으로만 유지한다
// (DORO_Load_Backend_API_명세서_v0.2_Frontend구현기준.md §2.1 참고).
public record VehicleTrimListResponse(
        List<VehicleTrimItem> items, int page, int size, long totalElements, int totalPages) {

    // batteryKwh/normalRangeKm/maxAcKw/maxDcKw: Priority 2 예약 필드, 화면엔 아직 안 쓰지만 프론트 타입엔 이미 있어 유지.
    // coldRangeKm: RDS에 대응 컬럼이 없어 항상 null — Priority 2에서 데이터 출처가 정해지면 채운다.
    public record VehicleTrimItem(
            Long vehicleTrimId,
            String brand,
            String modelName,
            String trimName,
            BigDecimal batteryKwh,
            Integer normalRangeKm,
            Integer coldRangeKm,
            BigDecimal maxAcKw,
            BigDecimal maxDcKw,
            List<String> connectorCodes) {
    }
}
