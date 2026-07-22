package com.doroload.api.vehicle.application;

import com.doroload.api.vehicle.api.dto.VehicleTrimListResponse;
import com.doroload.api.vehicle.api.dto.VehicleTrimListResponse.VehicleConnectorItem;
import com.doroload.api.vehicle.api.dto.VehicleTrimListResponse.VehicleTrimItem;
import com.doroload.api.vehicle.domain.VehicleConnector;
import com.doroload.api.vehicle.domain.VehicleTrim;
import com.doroload.api.vehicle.infrastructure.mysql.VehicleTrimJpaRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 차량 브랜드·모델·트림 검색 (MySQL만 사용, Redis 조회·갱신 없음)
@Service
@Transactional(readOnly = true)
public class VehicleTrimService {

    private final VehicleTrimJpaRepository vehicleTrimJpaRepository;

    public VehicleTrimService(VehicleTrimJpaRepository vehicleTrimJpaRepository) {
        this.vehicleTrimJpaRepository = vehicleTrimJpaRepository;
    }

    // 검색어를 정규화한 뒤 Id Page 조회 → 상세 Join Fetch 2단계로 N+1 없이 목록을 구성
    public VehicleTrimListResponse search(String brandName, String modelName, String keyword, int page, int size) {
        String brandPattern = toLikePattern(brandName);
        String modelPattern = toLikePattern(modelName);
        String keywordPattern = toLikePattern(keyword);

        Page<Long> idPage = vehicleTrimJpaRepository.findIdsByFilter(
                brandPattern, modelPattern, keywordPattern, PageRequest.of(page, size));

        List<Long> orderedIds = idPage.getContent();
        if (orderedIds.isEmpty()) {
            return new VehicleTrimListResponse(List.of(), page, size, idPage.getTotalElements(), idPage.getTotalPages());
        }

        Map<Long, VehicleTrim> byId = new LinkedHashMap<>();
        for (VehicleTrim trim : vehicleTrimJpaRepository.findDetailByIds(orderedIds)) {
            byId.put(trim.getVehicleTrimId(), trim);
        }

        List<VehicleTrimItem> items = orderedIds.stream()
                .map(byId::get)
                .filter(trim -> trim != null)
                .map(this::toItem)
                .toList();

        return new VehicleTrimListResponse(items, page, size, idPage.getTotalElements(), idPage.getTotalPages());
    }

    private VehicleTrimItem toItem(VehicleTrim trim) {
        List<VehicleConnectorItem> connectors = trim.getConnectors().stream()
                .sorted(Comparator.comparing(vc -> vc.getConnectorType().getConnectorCode()))
                .map(this::toConnectorItem)
                .toList();
        return new VehicleTrimItem(
                trim.getVehicleTrimId(),
                trim.getModel().getBrand().getBrandId(),
                trim.getModel().getBrand().getBrandName(),
                trim.getModel().getModelId(),
                trim.getModel().getModelName(),
                trim.getTrimName(),
                connectors);
    }

    private VehicleConnectorItem toConnectorItem(VehicleConnector connector) {
        return new VehicleConnectorItem(
                connector.getConnectorType().getConnectorCode(),
                connector.getConnectorType().getConnectorName(),
                connector.isStandard());
    }

    // 검색어를 정규화하고 LIKE Wildcard(%)를 Java에서 미리 조립한다
    private String toLikePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim() + "%";
    }
}
