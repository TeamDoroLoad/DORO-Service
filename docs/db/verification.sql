-- 개발 DB 수동 스모크 테스트 (읽기 전용). 구현 가이드 22.3 참조.

-- 1) 14개 Table 존재 확인
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;

-- 2) location Column이 POINT SRID 4326 인지 확인
SELECT table_name, column_name, data_type, srs_id
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'station' AND column_name = 'location';

-- 3) 좌표 축 순서 확인 (longitude=126.97xx, latitude=37.5xxx 이어야 정상)
SELECT station_id, ST_Longitude(location) AS longitude, ST_Latitude(location) AS latitude
FROM station;

-- 4) Spatial Index 사용 여부 확인 (Query Plan에 spx_station_location 포함되어야 함)
EXPLAIN
SELECT station_id
FROM station
WHERE ST_Distance_Sphere(
    location,
    ST_GeomFromText('POINT(126.9780 37.5665)', 4326, 'axis-order=long-lat')
) <= 3000;

-- 5) 최신 상태 View의 동점자 정렬(tie-breaker) 확인
SELECT * FROM v_charger_latest_status ORDER BY charger_id;

-- 6) tariff Table 존재 여부만 확인 (공개 API가 조회하지 않는지는 애플리케이션 코드로 별도 확인)
SELECT COUNT(*) AS tariff_row_count FROM tariff;

-- 7) FK 무결성: 고아 FK 0건 기대
SELECT COUNT(*) AS orphan_stations FROM station s
LEFT JOIN charging_network n ON s.network_id = n.network_id
WHERE n.network_id IS NULL;

SELECT COUNT(*) AS orphan_chargers FROM charger c
LEFT JOIN station s ON c.station_id = s.station_id
WHERE s.station_id IS NULL;
