package com.doroload.api.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 운영 환경에서 Wildcard 없이 명시적으로 승인된 Origin만 관리하기 위한 설정
@ConfigurationProperties(prefix = "doro.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
