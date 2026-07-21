package com.doroload.api.location.infrastructure.tmap;

// TMAP 응답을 DORO 표준 위치 모델로 정규화하기 전의 중간 결과
public record TmapPlace(
        String externalId, String name, String roadAddress, String jibunAddress, double latitude, double longitude) {
}
