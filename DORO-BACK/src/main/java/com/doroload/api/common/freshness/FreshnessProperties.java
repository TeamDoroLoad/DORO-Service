package com.doroload.api.common.freshness;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 최신 상태 판정 경계값. 운영 중 조정 가능하도록 설정으로 분리한다 (구현 가이드 8.3).
@ConfigurationProperties(prefix = "doro.freshness")
public record FreshnessProperties(long freshMinutes, long delayedMinutes) {
}
