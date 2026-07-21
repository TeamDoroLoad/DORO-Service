package com.doroload.api.recommendation;

import com.doroload.api.common.enums.Freshness;
import com.doroload.api.station.infrastructure.mysql.StationCandidateRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// 점수 계산 직전까지 한 충전소 후보에 대해 모은 중간 집계 결과 (영속화하지 않는 계산용 값)
public record CandidateAggregate(
        StationCandidateRow row,
        List<String> matchedConnectorCodes,
        int availableCount,
        int busyCount,
        int unknownCount,
        int totalCount,
        BigDecimal bestCompatiblePowerKw,
        Instant latestStatusUpdatedAt,
        Instant latestCollectedAt,
        Freshness freshness,
        boolean membershipMatched) {

    // 모든 호환 충전기가 점검·고장으로 판정된 경우 (Hard Filter 대상)
    public boolean allOutOfService() {
        return availableCount == 0 && busyCount == 0 && unknownCount == 0 && totalCount > 0;
    }
}
