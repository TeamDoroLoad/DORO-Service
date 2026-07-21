package com.doroload.api.vehicle.infrastructure.mysql;

import com.doroload.api.vehicle.domain.VehicleTrim;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleTrimJpaRepository extends JpaRepository<VehicleTrim, Long> {

    // 1단계: 검색·정렬·Pagination을 SQL에서 그대로 수행해 대상 Trim Id만 확정한다 (Collection Fetch와 분리해 N+1·중복 페이지네이션 문제를 피함)
    // Wildcard(%)는 Java에서 미리 조립해 전달한다 (JPQL CONCAT 중첩이 일부 환경에서 Bind Parameter 재사용 시 예상과 다르게 평가되는 것을 피하기 위함)
    @Query(
            value = """
                    SELECT t.vehicleTrimId FROM VehicleTrim t
                    JOIN t.model m JOIN m.brand b
                    WHERE (:brandPattern IS NULL OR b.brandName LIKE :brandPattern)
                      AND (:modelPattern IS NULL OR m.modelName LIKE :modelPattern)
                      AND (:keywordPattern IS NULL
                           OR b.brandName LIKE :keywordPattern
                           OR m.modelName LIKE :keywordPattern
                           OR t.trimName LIKE :keywordPattern)
                    ORDER BY b.brandName ASC, m.modelName ASC, t.trimName ASC, t.vehicleTrimId ASC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM VehicleTrim t
                    JOIN t.model m JOIN m.brand b
                    WHERE (:brandPattern IS NULL OR b.brandName LIKE :brandPattern)
                      AND (:modelPattern IS NULL OR m.modelName LIKE :modelPattern)
                      AND (:keywordPattern IS NULL
                           OR b.brandName LIKE :keywordPattern
                           OR m.modelName LIKE :keywordPattern
                           OR t.trimName LIKE :keywordPattern)
                    """)
    Page<Long> findIdsByFilter(
            @Param("brandPattern") String brandPattern,
            @Param("modelPattern") String modelPattern,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);

    // 2단계: 확정된 Id 집합만 Model·Brand·Connector·ConnectorType까지 한 번에 Join Fetch
    @Query("""
            SELECT DISTINCT t FROM VehicleTrim t
            JOIN FETCH t.model m
            JOIN FETCH m.brand b
            LEFT JOIN FETCH t.connectors c
            LEFT JOIN FETCH c.connectorType
            WHERE t.vehicleTrimId IN :ids
            """)
    List<VehicleTrim> findDetailByIds(@Param("ids") List<Long> ids);
}
