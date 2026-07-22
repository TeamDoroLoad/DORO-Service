package com.doroload.api.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

// REST 명세서 4.4 공통 오류 응답 Body
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        List<FieldViolation> details) {

    public record FieldViolation(String field, String reason) {
    }

    public static ErrorResponse of(
            int status, String code, String message, String path, String requestId, List<FieldViolation> details) {
        return new ErrorResponse(Instant.now(), status, code, message, path, requestId, details);
    }
}
