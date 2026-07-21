package com.doroload.api.station.infrastructure.mysql;

import com.doroload.api.station.domain.Station;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationJpaRepository extends JpaRepository<Station, Long> {

    // 충전소 상세 조회 시 Network·Operator를 함께 Join Fetch (N+1 방지)
    @Query("""
            SELECT s FROM Station s
            JOIN FETCH s.network n
            JOIN FETCH n.operator o
            WHERE s.stationId = :stationId
            """)
    Optional<Station> findDetailById(@Param("stationId") Long stationId);
}
