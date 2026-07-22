package com.doroload.api.network.application;

import com.doroload.api.network.api.dto.ChargingNetworkListResponse;
import com.doroload.api.network.api.dto.ChargingNetworkListResponse.ChargingNetworkItem;
import com.doroload.api.network.domain.ChargingNetwork;
import com.doroload.api.network.infrastructure.mysql.ChargingNetworkJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 충전 네트워크·사업자 기준 정보 조회 (MySQL만 사용, 사업자별 요금은 반환하지 않음)
@Service
@Transactional(readOnly = true)
public class ChargingNetworkService {

    private final ChargingNetworkJpaRepository chargingNetworkJpaRepository;

    public ChargingNetworkService(ChargingNetworkJpaRepository chargingNetworkJpaRepository) {
        this.chargingNetworkJpaRepository = chargingNetworkJpaRepository;
    }

    public ChargingNetworkListResponse search(String keyword) {
        String pattern = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";
        var items = chargingNetworkJpaRepository.search(pattern).stream()
                .map(this::toItem)
                .toList();
        return new ChargingNetworkListResponse(items);
    }

    private ChargingNetworkItem toItem(ChargingNetwork network) {
        return new ChargingNetworkItem(
                network.getNetworkId(), network.getNetworkName(), network.getOperator().getLegalName());
    }
}
