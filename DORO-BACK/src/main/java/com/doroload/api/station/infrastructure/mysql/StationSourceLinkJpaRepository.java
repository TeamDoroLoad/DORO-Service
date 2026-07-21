package com.doroload.api.station.infrastructure.mysql;

import com.doroload.api.station.domain.StationSourceLink;
import com.doroload.api.station.domain.StationSourceLinkId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationSourceLinkJpaRepository extends JpaRepository<StationSourceLink, StationSourceLinkId> {

    // 충전소 상세 조회용: DataSource까지 함께 Join Fetch
    @Query("""
            SELECT l FROM StationSourceLink l
            JOIN FETCH l.dataSource
            WHERE l.id.stationId = :stationId
            """)
    List<StationSourceLink> findByStationId(@Param("stationId") Long stationId);

    // 추천 API가 선택한 상위 후보들의 원천 매칭 정보를 한 번에 조회 (N+1 방지)
    @Query("""
            SELECT l FROM StationSourceLink l
            JOIN FETCH l.dataSource
            WHERE l.id.stationId IN :stationIds
            """)
    List<StationSourceLink> findByStationIds(@Param("stationIds") List<Long> stationIds);
}
