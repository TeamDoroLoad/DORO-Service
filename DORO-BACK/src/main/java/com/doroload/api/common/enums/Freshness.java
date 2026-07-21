package com.doroload.api.common.enums;

// REST 명세서 11.6 Freshness. source_updated_at(없으면 collected_at) 기준으로 산출한다.
public enum Freshness {
    FRESH,
    DELAYED,
    STALE,
    UNKNOWN
}
