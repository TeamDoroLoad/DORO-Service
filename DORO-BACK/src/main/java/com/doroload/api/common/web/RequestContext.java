package com.doroload.api.common.web;

import org.slf4j.MDC;

// 요청 전역에서 Request Id를 조회하기 위한 헬퍼 (MDC 기반)
public final class RequestContext {

    public static final String REQUEST_ID_KEY = "requestId";

    private RequestContext() {
    }

    // 현재 요청의 Request Id를 MDC에서 조회, 없으면 "unknown" 반환
    public static String currentRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        return requestId != null ? requestId : "unknown";
    }
}
