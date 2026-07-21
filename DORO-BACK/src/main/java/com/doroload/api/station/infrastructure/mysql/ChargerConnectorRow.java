package com.doroload.api.station.infrastructure.mysql;

import java.math.BigDecimal;

// 후보 충전소들의 충전기 1대 × 제공 커넥터 1종 조합 한 행
public record ChargerConnectorRow(Long chargerId, Long stationId, BigDecimal maxPowerKw, String connectorCode) {
}
