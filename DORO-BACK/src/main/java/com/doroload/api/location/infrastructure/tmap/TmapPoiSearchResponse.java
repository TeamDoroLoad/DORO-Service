package com.doroload.api.location.infrastructure.tmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// TMAP 통합 POI 검색(GET /tmap/pois) 원본 응답 구조. 실제 배포 전 운영 App Key로 실 응답 필드를 재검증해야 한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmapPoiSearchResponse(SearchPoiInfo searchPoiInfo) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchPoiInfo(Pois pois) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pois(List<Poi> poi) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Poi(
            String id,
            String name,
            String frontLat,
            String frontLon,
            String upperAddrName,
            String middleAddrName,
            String lowerAddrName,
            String detailAddrName,
            NewAddressList newAddressList) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewAddressList(List<NewAddress> newAddress) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewAddress(String fullAddressRoad, String centerLat, String centerLon) {
    }
}
