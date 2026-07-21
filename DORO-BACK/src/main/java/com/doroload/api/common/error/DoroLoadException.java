package com.doroload.api.common.error;

// 모든 도메인 예외의 공통 상위 타입. 외부 Library 예외를 Controller까지 그대로 전달하지 않기 위한 경계.
public abstract class DoroLoadException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DoroLoadException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
