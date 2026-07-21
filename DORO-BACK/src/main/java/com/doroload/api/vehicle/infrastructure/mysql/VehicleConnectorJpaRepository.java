package com.doroload.api.vehicle.infrastructure.mysql;

import com.doroload.api.vehicle.domain.VehicleConnector;
import com.doroload.api.vehicle.domain.VehicleConnectorId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleConnectorJpaRepository extends JpaRepository<VehicleConnector, VehicleConnectorId> {

    // 추천 API의 차량 호환 커넥터 Hard Filter에 사용할 코드 목록만 가볍게 조회
    @Query("SELECT vc.id.connectorCode FROM VehicleConnector vc WHERE vc.id.trimId = :trimId")
    List<String> findConnectorCodesByTrimId(@Param("trimId") Long trimId);
}
