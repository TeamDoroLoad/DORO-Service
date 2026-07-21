package com.doroload.api.common.enums;

// REST 명세서 11.3 StationType
public enum StationType {
    PUBLIC,
    PRIVATE,
    UNKNOWN;

    public static StationType fromDb(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return StationType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
