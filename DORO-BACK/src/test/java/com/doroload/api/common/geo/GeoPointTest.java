package com.doroload.api.common.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoPointTest {

    // MySQL 입력 WKT는 longitude를 먼저 써야 한다 (axis-order=long-lat)
    @Test
    void toWkt_putsLongitudeBeforeLatitude() {
        GeoPoint point = new GeoPoint(37.5665, 126.9780);

        assertThat(point.toWkt()).isEqualTo("POINT(126.978 37.5665)");
    }

    // Bounding Box는 중심 좌표를 포함하고 위경도 순서를 뒤집지 않는다
    @Test
    void boundingPolygonWkt_containsCenterPoint() {
        GeoPoint point = new GeoPoint(37.5665, 126.9780);

        String polygon = point.boundingPolygonWkt(3000);

        assertThat(polygon).startsWith("POLYGON((");
        assertThat(polygon).contains("126.9");
        assertThat(polygon).contains("37.5");
    }

    // 같은 약 50m 격자 내 좌표는 동일한 Cell Key를 가져야 Route Cache가 재사용된다
    @Test
    void toCellKey_isStableForNearbyPoints() {
        GeoPoint a = new GeoPoint(37.56650, 126.97800);
        GeoPoint b = new GeoPoint(37.56652, 126.97803);

        assertThat(a.toCellKey()).isEqualTo(b.toCellKey());
    }
}
