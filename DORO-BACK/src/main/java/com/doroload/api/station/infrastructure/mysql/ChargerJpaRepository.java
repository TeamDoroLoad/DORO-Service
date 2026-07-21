package com.doroload.api.station.infrastructure.mysql;

import com.doroload.api.station.domain.Charger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargerJpaRepository extends JpaRepository<Charger, Long> {

    // 충전소 상세 조회용: Connector·ConnectorType까지 한 번에 Join Fetch
    @Query("""
            SELECT DISTINCT c FROM Charger c
            LEFT JOIN FETCH c.connectors cc
            LEFT JOIN FETCH cc.connectorType
            WHERE c.station.stationId = :stationId
            ORDER BY c.chargerId ASC
            """)
    List<Charger> findByStationIdWithConnectors(@Param("stationId") Long stationId);
}
