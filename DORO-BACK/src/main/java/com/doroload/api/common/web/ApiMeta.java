package com.doroload.api.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

// 공통 성공 응답의 meta Block (REST 명세서 4.3)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(String requestId, Instant generatedAt, Boolean partial, List<String> warnings) {

    // 단순 조회 API용 meta 생성
    public static ApiMeta of(String requestId) {
        return new ApiMeta(requestId, Instant.now(), null, null);
    }

    // 부분 실패 여부(partial)와 warnings가 필요한 추천 API용 meta 생성
    public static ApiMeta ofPartial(String requestId, boolean partial, List<String> warnings) {
        return new ApiMeta(requestId, Instant.now(), partial, warnings);
    }
}
