package com.doroload.api.common.enums;

// REST 명세서 11.2 ChargeMode
public enum ChargeMode {
    AC,
    DC,
    UNKNOWN;

    // DB에 저장된 원시 값을 안전하게 변환. 알 수 없는 값은 예외 대신 UNKNOWN으로 대체한다.
    public static ChargeMode fromDb(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return ChargeMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
