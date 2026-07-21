package com.doroload.api.location.api.dto;

import com.doroload.api.common.geo.GeoPoint;
import java.util.List;

// GET /locations/search 응답 (REST 명세서 8.1)
public record LocationSearchResponse(List<LocationItem> items) {

    public record LocationItem(
            String placeId,
            String type,
            String name,
            String formattedAddress,
            String roadAddress,
            String jibunAddress,
            GeoPoint location,
            String provider) {
    }
}
