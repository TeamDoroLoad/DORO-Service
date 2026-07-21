package com.doroload.api.common.enums;

// REST 명세서 11.1 ConnectorCode
public enum ConnectorCode {
    AC_5PIN,
    DC_CHADEMO,
    DC_COMBO_1,
    DC_COMBO_2,
    AC_3PHASE,
    NACS,
    UNKNOWN;

    // DB·요청에 저장된 원시 코드를 안전하게 변환. 매핑되지 않은 값은 UNKNOWN으로 대체한다.
    public static ConnectorCode fromDb(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        try {
            return ConnectorCode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
