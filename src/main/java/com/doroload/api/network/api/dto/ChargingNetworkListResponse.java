package com.doroload.api.network.api.dto;

import java.util.List;

// GET /charging-networks 응답 (REST 명세서 7.1). 네트워크별 요금은 포함하지 않는다.
public record ChargingNetworkListResponse(List<ChargingNetworkItem> items) {

    public record ChargingNetworkItem(Long networkId, String networkName, OperatorItem operator) {
    }

    public record OperatorItem(Long operatorId, String operatorCode, String legalName) {
    }
}
