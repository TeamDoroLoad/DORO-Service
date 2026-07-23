package com.doroload.api.vehicle.application;

import com.doroload.api.vehicle.api.dto.BrandListResponse;
import com.doroload.api.vehicle.infrastructure.mysql.BrandJpaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 브랜드 전체 목록 조회 (트림 Pagination과 무관 — 드롭다운 초기화용)
@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandJpaRepository brandJpaRepository;

    public BrandService(BrandJpaRepository brandJpaRepository) {
        this.brandJpaRepository = brandJpaRepository;
    }

    public List<BrandListResponse> findAll() {
        return brandJpaRepository.findAllByOrderByBrandNameAsc().stream()
                .map(b -> new BrandListResponse(b.getBrandId(), b.getBrandName()))
                .toList();
    }
}
