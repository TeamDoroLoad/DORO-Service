package com.doroload.api.vehicle.infrastructure.mysql;

import com.doroload.api.vehicle.domain.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<Brand, Long> {

    // 트림 Pagination과 무관하게 brand Table 전체를 이름순으로 조회 (브랜드 드롭다운용)
    List<Brand> findAllByOrderByBrandNameAsc();
}
