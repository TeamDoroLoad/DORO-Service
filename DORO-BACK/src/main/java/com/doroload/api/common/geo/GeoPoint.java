package com.doroload.api.common.geo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

// 위경도 좌표 값 객체. API는 latitude/longitude(WGS84) 순서를 쓰지만
// MySQL ST_GeomFromText 입력 WKT는 longitude-latitude 순서(axis-order=long-lat)를 요구한다.
public record GeoPoint(
        @NotNull(message = "must not be null") @DecimalMin(value = "-90", message = "must be between -90 and 90")
                @DecimalMax(value = "90", message = "must be between -90 and 90") Double latitude,
        @NotNull(message = "must not be null") @DecimalMin(value = "-180", message = "must be between -180 and 180")
                @DecimalMax(value = "180", message = "must be between -180 and 180") Double longitude) {

    private static final double GRID_CELL_DEGREES = 0.0005; // 약 50m 격자
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    // MySQL ST_GeomFromText 입력용 WKT(POINT(longitude latitude)) 생성
    public String toWkt() {
        return String.format(Locale.ROOT, "POINT(%s %s)", longitude, latitude);
    }

    // 반경 검색용 Bounding Box(WKT Polygon) 생성. 실제 3km 포함 판정은 ST_Distance_Sphere로 별도 수행한다.
    public String boundingPolygonWkt(double radiusMeters) {
        double latDelta = radiusMeters / METERS_PER_DEGREE_LATITUDE;
        double lngDelta = radiusMeters / (METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latitude)));
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLng = longitude - lngDelta;
        double maxLng = longitude + lngDelta;
        return "POLYGON((" + minLng + " " + minLat + ", " + maxLng + " " + minLat + ", "
                + maxLng + " " + maxLat + ", " + minLng + " " + maxLat + ", " + minLng + " " + minLat + "))";
    }

    // Redis Route Cache Key에 사용할 원점 격자 좌표 (약 50m 단위로 반올림)
    public String toCellKey() {
        long latCell = Math.round(latitude / GRID_CELL_DEGREES);
        long lngCell = Math.round(longitude / GRID_CELL_DEGREES);
        return latCell + "_" + lngCell;
    }
}
