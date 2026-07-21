package com.doroload.api.common.error;

// Application 자체 호출 제한(429) 초과
public class RateLimitException extends DoroLoadException {

    public RateLimitException(String message) {
        super(ErrorCode.RATE_LIMITED, message);
    }
}
