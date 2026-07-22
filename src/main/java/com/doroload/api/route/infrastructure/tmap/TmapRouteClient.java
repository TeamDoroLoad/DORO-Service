package com.doroload.api.route.infrastructure.tmap;

import com.doroload.api.common.config.TmapProperties;
import com.doroload.api.common.geo.GeoPoint;
import com.doroload.api.route.domain.RouteSummary;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// TMAP 자동차 경로 요약 Client. 실패는 예외로 전달하지 않고 항상 RouteSummary(UNAVAILABLE)로 흡수한다.
@Component
public class TmapRouteClient {

    private final RestClient tmapRestClient;
    private final TmapProperties tmapProperties;

    public TmapRouteClient(RestClient tmapRestClient, TmapProperties tmapProperties) {
        this.tmapRestClient = tmapRestClient;
        this.tmapProperties = tmapProperties;
    }

    // 후보 1건에 대한 자동차 경로 요약을 조회한다 (Cache Miss일 때만 호출됨)
    @CircuitBreaker(name = "tmapRoute", fallbackMethod = "fallback")
    @Retry(name = "tmapRoute")
    @Bulkhead(name = "tmapRoute", fallbackMethod = "fallback")
    public RouteSummary getRouteSummary(GeoPoint origin, GeoPoint destination) {
        TmapRouteRequest request = new TmapRouteRequest(
                origin.longitude(), origin.latitude(),
                destination.longitude(), destination.latitude(),
                "WGS84GEO", "WGS84GEO",
                tmapProperties.searchOption(), tmapProperties.totalValue());
        try {
            TmapRouteResponse response = tmapRestClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/tmap/tmap/routes").queryParam("version", 1).build())
                    .body(request)
                    .retrieve()
                    .body(TmapRouteResponse.class);
            return toSummary(response);
        } catch (RestClientException e) {
            return RouteSummary.unavailable(classify(e));
        }
    }

    // Circuit Open·Bulkhead 포화 시 호출되는 Fallback
    private RouteSummary fallback(GeoPoint origin, GeoPoint destination, Throwable throwable) {
        return RouteSummary.unavailable(classify(throwable));
    }

    private RouteSummary toSummary(TmapRouteResponse response) {
        if (response == null || response.features() == null) {
            return RouteSummary.unavailable("TMAP_BAD_RESPONSE");
        }
        return response.features().stream()
                .map(TmapRouteResponse.Feature::properties)
                .filter(p -> p != null && p.totalDistance() != null && p.totalTime() != null)
                .findFirst()
                .map(p -> RouteSummary.fresh(p.totalDistance(), p.totalTime(), Instant.now()))
                .orElse(RouteSummary.unavailable("TMAP_BAD_RESPONSE"));
    }

    private String classify(Throwable throwable) {
        if (throwable instanceof HttpClientErrorException.TooManyRequests) {
            return "TMAP_RATE_LIMITED";
        }
        if (throwable instanceof HttpClientErrorException) {
            return "TMAP_CLIENT_ERROR";
        }
        if (throwable instanceof HttpServerErrorException) {
            return "TMAP_SERVER_ERROR";
        }
        if (throwable instanceof ResourceAccessException) {
            return "TMAP_TIMEOUT";
        }
        return "TMAP_UNAVAILABLE";
    }
}
