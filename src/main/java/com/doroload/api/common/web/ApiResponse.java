package com.doroload.api.common.web;

import java.util.List;

// 모든 공개 API가 공유하는 성공 응답 Envelope
public record ApiResponse<T>(T data, ApiMeta meta) {

    // 단순 조회 성공 응답 생성
    public static <T> ApiResponse<T> of(T data, String requestId) {
        return new ApiResponse<>(data, ApiMeta.of(requestId));
    }

    // 부분 실패(partial) 정보를 포함하는 추천 API 성공 응답 생성
    public static <T> ApiResponse<T> ofPartial(T data, String requestId, boolean partial, List<String> warnings) {
        return new ApiResponse<>(data, ApiMeta.ofPartial(requestId, partial, warnings));
    }
}
