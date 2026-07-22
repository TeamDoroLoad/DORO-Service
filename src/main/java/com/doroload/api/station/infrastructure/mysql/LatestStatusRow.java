package com.doroload.api.station.infrastructure.mysql;

import java.time.Instant;

// v_charger_latest_status View 조회 결과 한 행
public record LatestStatusRow(Long chargerId, String status, Instant sourceUpdatedAt, Instant collectedAt) {
}
