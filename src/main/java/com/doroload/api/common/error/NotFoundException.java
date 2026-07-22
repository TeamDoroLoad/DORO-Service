package com.doroload.api.common.error;

// 차량 트림·충전소 등 요청한 자원이 존재하지 않을 때 사용
public class NotFoundException extends DoroLoadException {

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
