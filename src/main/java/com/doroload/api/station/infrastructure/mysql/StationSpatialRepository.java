package com.doroload.api.station.infrastructure.mysql;

import com.doroload.api.common.geo.GeoPoint;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// MySQL Spatial 관련 Native Query 저장소 (반경 검색: 구현 가이드 7.5, 좌표 단건 조회 포함)
@Repository
public class StationSpatialRepository {

    // ST_X()/ST_Y()는 MySQL 8 SRID 4326의 SRS 축 순서(위도-경도)를 따라 값이 뒤바뀌어 반환될 수 있으므로
    // 축 순서에 안전한 ST_Latitude()/ST_Longitude()만 사용한다 (ev-charger-collector-result-report.md 참고)
    private static final String LAT_LNG_SQL = """
            SELECT ST_Latitude(location) AS latitude, ST_Longitude(location) AS longitude
            FROM station
            WHERE station_id = :stationId
            """;

    private static final String CANDIDATE_SQL = """
            SELECT
                s.station_id AS station_id,
                s.station_name AS station_name,
                s.address AS address,
                s.station_type AS station_type,
                s.operating_hours AS operating_hours,
                ST_Latitude(s.location) AS latitude,
                ST_Longitude(s.location) AS longitude,
                n.network_id AS network_id,
                n.network_name AS network_name,
                o.legal_name AS operator_legal_name,
                ST_Distance_Sphere(
                    s.location,
                    ST_GeomFromText(:centerWkt, 4326, 'axis-order=long-lat')
                ) AS straight_distance_meters
            FROM station s
            JOIN charging_network n ON n.network_id = s.network_id
            JOIN operator o ON o.operator_id = n.operator_id
            WHERE MBRContains(
                    ST_GeomFromText(:boundingWkt, 4326, 'axis-order=long-lat'),
                    s.location
                  )
              AND ST_Distance_Sphere(
                    s.location,
                    ST_GeomFromText(:centerWkt, 4326, 'axis-order=long-lat')
                  ) <= :radiusMeters
              AND EXISTS (
                    SELECT 1 FROM charger c
                    JOIN charger_connector cc ON cc.charger_id = c.charger_id
                    WHERE c.station_id = s.station_id
                      AND cc.connector_code IN (:connectorCodes)
                  )
            ORDER BY straight_distance_meters ASC, s.station_id ASC
            LIMIT :internalLimit
            """;

    private static final String CHARGER_CONNECTOR_SQL = """
            SELECT c.charger_id AS charger_id, c.station_id AS station_id, c.max_power_kw AS max_power_kw,
                   cc.connector_code AS connector_code
            FROM charger c
            JOIN charger_connector cc ON cc.charger_id = c.charger_id
            WHERE c.station_id IN (:stationIds)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StationSpatialRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 충전소 단건의 위도·경도 조회 (JPA Point.getX()/getY() 대신 사용 — 축 순서 안전)
    public GeoPoint findLatLng(Long stationId) {
        Map<String, Object> params = Map.of("stationId", stationId);
        return jdbcTemplate.queryForObject(LAT_LNG_SQL, params, (rs, rowNum) ->
                new GeoPoint(rs.getDouble("latitude"), rs.getDouble("longitude")));
    }

    // 중심 좌표 반경 이내이면서 요청 차량과 호환 커넥터를 1개 이상 보유한 충전소 후보를 거리순으로 조회
    public List<StationCandidateRow> findCandidates(
            String centerWkt, String boundingWkt, double radiusMeters, Collection<String> connectorCodes, int internalLimit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("centerWkt", centerWkt)
                .addValue("boundingWkt", boundingWkt)
                .addValue("radiusMeters", radiusMeters)
                .addValue("connectorCodes", connectorCodes)
                .addValue("internalLimit", internalLimit);
        return jdbcTemplate.query(CANDIDATE_SQL, params, (rs, rowNum) -> new StationCandidateRow(
                rs.getLong("station_id"),
                rs.getString("station_name"),
                rs.getString("address"),
                rs.getString("station_type"),
                rs.getString("operating_hours"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getLong("network_id"),
                rs.getString("network_name"),
                rs.getString("operator_legal_name"),
                rs.getDouble("straight_distance_meters")));
    }

    // 후보 충전소들이 보유한 충전기 × 제공 커넥터 조합을 한 번에 조회 (N+1 방지)
    public List<ChargerConnectorRow> findChargerConnectorRows(Collection<Long> stationIds) {
        if (stationIds.isEmpty()) {
            return List.of();
        }
        Map<String, Object> params = Map.of("stationIds", stationIds);
        return jdbcTemplate.query(CHARGER_CONNECTOR_SQL, params, (rs, rowNum) -> new ChargerConnectorRow(
                rs.getLong("charger_id"),
                rs.getLong("station_id"),
                rs.getBigDecimal("max_power_kw"),
                rs.getString("connector_code")));
    }
}
