package com.doroload.api.common.error;

import org.springframework.http.HttpStatus;

// REST 명세서 12장 표준 오류 코드와 대응 HTTP 상태
public enum ErrorCode {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST),
    VEHICLE_TRIM_NOT_FOUND(HttpStatus.NOT_FOUND),
    STATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    EXTERNAL_API_BAD_RESPONSE(HttpStatus.BAD_GATEWAY),
    DATABASE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    SERVICE_TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
