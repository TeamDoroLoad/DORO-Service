package com.doroload.api.station.infrastructure.mysql;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// v_charger_latest_status View 조회 전용 저장소 (구현 가이드 7.6)
@Repository
public class LatestChargerStatusRepository {

    private static final String SQL = """
            SELECT charger_id, status, source_updated_at, collected_at
            FROM v_charger_latest_status
            WHERE charger_id IN (:chargerIds)
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
