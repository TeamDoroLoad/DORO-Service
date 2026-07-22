package com.doroload.api.network.infrastructure.mysql;

import com.doroload.api.network.domain.ChargingNetwork;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargingNetworkJpaRepository extends JpaRepository<ChargingNetwork, Long> {

    // Operator까지 한 번에 Join Fetch해 N+1 없이 네트워크 목록을 조회. Wildcard는 Java에서 조립해 전달한다.
    @Query("""
            SELECT n FROM ChargingNetwork n
            JOIN FETCH n.operator o
            WHERE :pattern IS NULL
               OR n.networkName LIKE :pattern
               OR o.legalName LIKE :pattern
            ORDER BY n.networkName ASC, n.networkId ASC
            """)
    List<ChargingNetwork> search(@Param("pattern") String pattern);
}
