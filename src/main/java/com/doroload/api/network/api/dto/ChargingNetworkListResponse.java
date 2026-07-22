package com.doroload.api.network.api.dto;

import java.util.List;

// GET /charging-networks 응답. 네트워크별 요금은 포함하지 않는다.
// 프론트(doro-load-web)는 operator를 중첩 객체가 아니라 flat operatorName 문자열로 소비한다
// (DORO_Load_Backend_API_명세서_v0.2_Frontend구현기준.md §2.3, §3).
public record ChargingNetworkListResponse(List<ChargingNetworkItem> items) {

    public record ChargingNetworkItem(Long networkId, String networkName, String operatorName) {
    }
}
