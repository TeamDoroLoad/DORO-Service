package com.doroload.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// TMAP 연동 설정. 실제 App Key는 doro/tmap Secret에서 환경변수로 주입한다 (Repository에 값 저장 금지)
@ConfigurationProperties(prefix = "doro.tmap")
public record TmapProperties(
        String baseUrl,
        String appKey,
        int connectTimeoutSeconds,
        int responseTimeoutSeconds,
        int searchOption,
        int totalValue,
        int routeCacheTtlSeconds,
        long routeLockTtlMillis,
        RateLimit rateLimit) {

    public record RateLimit(int routeLimitPerMinute, int locationLimitPerMinute) {
    }
}
