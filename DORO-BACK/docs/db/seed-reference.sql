-- DORO Load 로컬 개발용 참조·샘플 데이터
-- schema.sql 적용 이후 실행한다. 운영 환경에는 그대로 사용하지 않는다.

SET NAMES utf8mb4;

-- 표준 커넥터 (API 명세서 11.1 ConnectorCode)
INSERT INTO connector_type (connector_code, connector_name, charge_mode) VALUES
    ('AC_5PIN',    'AC 완속 5핀',   'AC'),
    ('DC_CHADEMO', 'DC 차데모',     'DC'),
    ('DC_COMBO_1', 'DC 콤보 1',     'DC'),
    ('DC_COMBO_2', 'DC 콤보 2',     'DC'),
    ('AC_3PHASE',  'AC 3상',        'AC'),
    ('NACS',       '북미 충전 규격', 'DC'),
    ('UNKNOWN',    '매핑 불가',     'UNKNOWN');

-- 차량 제조사·모델·트림 샘플
INSERT INTO brand (brand_id, brand_name) VALUES (1, '현대'), (2, '기아'), (3, '테슬라');

INSERT INTO vehicle_model (model_id, brand_id, model_name) VALUES
    (21, 1, '아이오닉 5'),
    (22, 2, 'EV6'),
    (23, 3, '모델 3');

INSERT INTO vehicle_trim (vehicle_trim_id, model_id, trim_name) VALUES
    (101, 21, '롱레인지 2WD'),
    (102, 21, '롱레인지 4WD'),
    (201, 22, '스탠다드 2WD'),
    (301, 23, '스탠다드'),
    (302, 23, '롱레인지');

INSERT INTO vehicle_connector (trim_id, connector_code, charge_mode, is_standard) VALUES
    (101, 'DC_COMBO_1', 'DC', TRUE),
    (101, 'AC_5PIN',    'AC', TRUE),
    (102, 'DC_COMBO_1', 'DC', TRUE),
    (102, 'AC_5PIN',    'AC', TRUE),
    (201, 'DC_COMBO_1', 'DC', TRUE),
    (201, 'AC_5PIN',    'AC', TRUE),
    (301, 'NACS',       'DC', TRUE),
    (302, 'NACS',       'DC', TRUE);

-- 사업자·충전 네트워크 샘플
INSERT INTO operator (operator_id, operator_code, legal_name) VALUES (3, 'ME', '한국환경공단');
INSERT INTO charging_network (network_id, operator_id, network_name) VALUES (12, 3, '환경부 공공충전 인프라');

-- 데이터 원천 메타데이터
INSERT INTO data_source (source_id, source_name, source_type, endpoint) VALUES
    (1, '한국환경공단_전기자동차 충전소 정보', 'PUBLIC_API', 'http://apis.data.go.kr/B552584/EvCharger');

-- 충전소·충전기 샘플 (API 명세서 9.1 예시 좌표: 서울시청 인근)
INSERT INTO station (station_id, network_id, station_name, address, station_type, location, operating_hours, created_at) VALUES
    (501, 12, '서울시청 인근문청사 충전소', '서울특별시 중구 덕수궁길 15', 'PUBLIC',
        ST_GeomFromText('POINT(126.9752 37.5639)', 4326, 'axis-order=long-lat'), '24시간', '2026-07-01 03:00:00');

INSERT INTO charger (charger_id, station_id, external_charger_id, charger_name, charger_type, max_power_kw) VALUES
    (9001, 501, '01', '급속 01', 'DC_FAST', 100.00),
    (9002, 501, '02', '급속 02', 'DC_FAST', 100.00),
    (9003, 501, '03', '완속 03', 'AC_SLOW', 7.00);

INSERT INTO charger_connector (charger_id, connector_code, charge_mode) VALUES
    (9001, 'DC_COMBO_1', 'DC'),
    (9002, 'DC_COMBO_1', 'DC'),
    (9003, 'AC_5PIN', 'AC');

INSERT INTO charger_status_history (charger_id, status, source_updated_at, collected_at) VALUES
    (9001, 'AVAILABLE', '2026-07-21 07:28:30', '2026-07-21 07:29:00'),
    (9002, 'CHARGING',  '2026-07-21 07:20:00', '2026-07-21 07:29:00'),
    (9003, 'AVAILABLE', '2026-07-21 07:25:00', '2026-07-21 07:29:00');

INSERT INTO station_source_link (station_id, source_id, source_station_id, match_method, match_score) VALUES
    (501, 1, 'ME000001', 'SOURCE_ID', 1.0000);
