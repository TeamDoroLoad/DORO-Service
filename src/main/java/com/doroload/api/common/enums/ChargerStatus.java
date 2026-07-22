package com.doroload.api.common.enums;

// REST 명세서 11.5 ChargerStatus
public enum ChargerStatus {
    AVAILABLE,
    CHARGING,
    RESERVED,
    OUT_OF_SERVICE,
    MAINTENANCE,
    COMMUNICATION_ERROR,
    UNKNOWN;

    public static ChargerStatus fromDb(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return ChargerStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    // 이용 가능·대기 판단에 쓰이는 "사용 가능" 상태 여부
    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    // Hard Filter에서 제외해야 하는 영구 이용 불가 상태 여부
    public boolean isOutOfService() {
        return this == OUT_OF_SERVICE || this == MAINTENANCE;
    }
}
