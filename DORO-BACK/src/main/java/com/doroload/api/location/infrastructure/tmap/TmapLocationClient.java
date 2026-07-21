package com.doroload.api.location.infrastructure.tmap;

import com.doroload.api.common.error.DependencyException;
import com.doroload.api.common.error.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// TMAP 주소·POI 통합 검색 Client. TMAP 원본 응답은 이 Package 밖으로 노출하지 않는다.
@Component
public class TmapLocationClient {

    private final RestClient tmapRestClient;

    public TmapLocationClient(RestClient tmapRestClient) {
        this.tmapRestClient = tmapRestClient;
    }

    // 검색어로 주소·장소를 조회한다. 실패 시 Fallback으로 즉시 DependencyException을 던진다.
    @CircuitBreaker(name = "tmapLocation", fallbackMethod = "searchFallback")
    @Retry(name = "tmapLocation")
    public List<TmapPlace> search(String query, int limit) {
        try {
            TmapPoiSearchResponse response = tmapRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tmap/pois")
                            .queryParam("version", 1)
                            .queryParam("searchKeyword", query)
                            .queryParam("count", limit)
                            .queryParam("reqCoordType", "WGS84GEO")
                            .queryParam("resCoordType", "WGS84GEO")
                            .build())
                    .retrieve()
                    .body(TmapPoiSearchResponse.class);
            return toPlaces(response);
        } catch (RestClientException e) {
            throw new DependencyException(ErrorCode.EXTERNAL_API_BAD_RESPONSE, "위치 검색 서비스 응답을 처리할 수 없습니다.");
        }
    }

    // Circuit Open·재시도 소진 시 호출되는 Fallback. 검색 결과가 없으면 서비스 핵심 기능을 수행할 수 없으므로 예외로 전달한다.
    private List<TmapPlace> searchFallback(String query, int limit, Throwable throwable) {
        throw new DependencyException(ErrorCode.SERVICE_TEMPORARILY_UNAVAILABLE, "위치 검색 서비스를 일시적으로 이용할 수 없습니다.");
    }

    private List<TmapPlace> toPlaces(TmapPoiSearchResponse response) {
        if (response == null || response.searchPoiInfo() == null || response.searchPoiInfo().pois() == null
                || response.searchPoiInfo().pois().poi() == null) {
            return List.of();
        }
        return response.searchPoiInfo().pois().poi().stream()
                .map(this::toPlace)
                .filter(place -> place != null)
                .toList();
    }

    private TmapPlace toPlace(TmapPoiSearchResponse.Poi poi) {
        try {
            String roadAddress = extractRoadAddress(poi);
            String jibunAddress = buildJibunAddress(poi);
            double latitude = Double.parseDouble(poi.frontLat());
            double longitude = Double.parseDouble(poi.frontLon());
            return new TmapPlace(poi.id(), poi.name(), roadAddress, jibunAddress, latitude, longitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractRoadAddress(TmapPoiSearchResponse.Poi poi) {
        if (poi.newAddressList() != null && poi.newAddressList().newAddress() != null
                && !poi.newAddressList().newAddress().isEmpty()) {
            return poi.newAddressList().newAddress().get(0).fullAddressRoad();
        }
        return null;
    }

    private String buildJibunAddress(TmapPoiSearchResponse.Poi poi) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, poi.upperAddrName());
        appendIfPresent(sb, poi.middleAddrName());
        appendIfPresent(sb, poi.lowerAddrName());
        appendIfPresent(sb, poi.detailAddrName());
        return sb.isEmpty() ? null : sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(value.trim());
        }
    }
}
