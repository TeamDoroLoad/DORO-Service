package com.doroload.api.common.error;

import java.util.List;

// Controller 진입 이전 단순 형식 검증을 넘어서는 도메인 수준 검증 실패
public class ValidationException extends DoroLoadException {

    private final List<ErrorResponse.FieldViolation> details;

    public ValidationException(ErrorCode errorCode, String message, List<ErrorResponse.FieldViolation> details) {
        super(errorCode, message);
        this.details = details;
    }

    public List<ErrorResponse.FieldViolation> details() {
        return details;
    }
}
