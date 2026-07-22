package com.doroload.api.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 반경 검색 정책 (REST 명세서 19장). radiusMeters·resultLimit은 서버 정책으로 고정하며 Client가 재정의할 수 없다.
@ConfigurationProperties(prefix = "doro.search")
public record SearchProperties(int radiusMeters, int resultLimit, int internalCandidateLimit) {
}
