package com.doroload.api.station.infrastructure.mysql;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// 충전기별 최신 상태 조회 (구현 가이드 7.6 로직 기준).
// 원래 설계는 v_charger_latest_status DB VIEW를 두는 것이지만(다른 Schema 객체처럼 담당자가
// 수동 생성), 그 VIEW가 아직 RDS에 없고 "RDS 구조는 변경하지 않는다"는 원칙상 VIEW 생성도
// 보류 중이라 동일 로직을 charger_status_history에 직접 인라인했다. charger_id 필터를 윈도우
// 함수 이전(가장 안쪽 서브쿼리)에 걸어 VIEW 경유보다 스캔 범위를 확실히 좁힌다.
@Repository
public class LatestChargerStatusRepository {

    private static final String SQL = """
            SELECT charger_id, status, source_updated_at, collected_at
            FROM (
                SELECT
                    h.*,
                    ROW_NUMBER() OVER (
                        PARTITION BY h.charger_id
                        ORDER BY h.source_updated_at DESC, h.collected_at DESC, h.status_id DESC
                    ) AS rn
                FROM charger_status_history h
                WHERE h.charger_id IN (:chargerIds)
            ) ranked
            WHERE ranked.rn = 1
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LatestChargerStatusRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 주어진 충전기 Id들의 최신 상태를 한 번에 조회 (N+1 방지)
    public List<LatestStatusRow> findLatestByChargerIds(Collection<Long> chargerIds) {
        if (chargerIds.isEmpty()) {
            return List.of();
        }
        Map<String, Object> params = Map.of("chargerIds", chargerIds);
        return jdbcTemplate.query(SQL, params, (rs, rowNum) -> new LatestStatusRow(
                rs.getLong("charger_id"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("source_updated_at")),
                toInstant(rs.getTimestamp("collected_at"))));
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
