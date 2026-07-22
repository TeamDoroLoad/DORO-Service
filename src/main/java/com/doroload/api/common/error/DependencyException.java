package com.doroload.api.common.error;

// DB·TMAP 등 외부 의존성 장애로 요청 자체를 처리할 수 없을 때 사용 (502·503)
public class DependencyException extends DoroLoadException {

    public DependencyException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
