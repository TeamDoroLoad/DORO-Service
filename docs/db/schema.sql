-- DORO Load Backend 승인 스키마 (14개 Table)
-- 근거: DORO_Load_Backend_API_Specification_v1.0.md, DORO_Load_Backend_Application_Implementation_Guide_v1.0.md
-- 이 파일은 애플리케이션이 자동 실행하지 않는 검토·수동 적용 전용 산출물이다 (spring.jpa.hibernate.ddl-auto=validate).
-- Character Set: utf8mb4, 시간: UTC 저장, 좌표: SRID 4326 (longitude-latitude axis-order)

SET NAMES utf8mb4;

-- 1. 사업자
CREATE TABLE operator (
    operator_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_code   VARCHAR(50)  NOT NULL,
    legal_name      VARCHAR(255) NOT NULL,
    PRIMARY KEY (operator_id),
    UNIQUE KEY uk_operator_code (operator_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 차량 제조사 Master
CREATE TABLE brand (
    brand_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    brand_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (brand_id),
    UNIQUE KEY uk_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 표준 커넥터 Master
CREATE TABLE connector_type (
    connector_code  VARCHAR(20)  NOT NULL,
    connector_name  VARCHAR(100) NOT NULL,
    charge_mode     VARCHAR(10)  NOT NULL,
    PRIMARY KEY (connector_code),
    CONSTRAINT ck_connector_type_charge_mode CHECK (charge_mode IN ('AC', 'DC', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 외부 원천 Metadata
CREATE TABLE data_source (
    source_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_name  VARCHAR(255) NOT NULL,
    source_type  VARCHAR(50)  NULL,
    endpoint     VARCHAR(500) NULL,
    PRIMARY KEY (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 충전 네트워크 (Operator 1:N)
CREATE TABLE charging_network (
    network_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_id   BIGINT UNSIGNED NOT NULL,
    network_name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (network_id),
    CONSTRAINT fk_network_operator FOREIGN KEY (operator_id) REFERENCES operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 차량 Model (Brand 1:N)
CREATE TABLE vehicle_model (
    model_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    brand_id    BIGINT UNSIGNED NOT NULL,
    model_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (model_id),
    CONSTRAINT fk_model_brand FOREIGN KEY (brand_id) REFERENCES brand (brand_id),
    KEY idx_model_brand (brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 충전소 (공간 조회 기준 Column 포함)
CREATE TABLE station (
    station_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network_id       BIGINT UNSIGNED NOT NULL,
    station_name     VARCHAR(255) NOT NULL,
    address          VARCHAR(500) NOT NULL,
    station_type     VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    location         POINT NOT NULL SRID 4326,
    operating_hours  VARCHAR(255) NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NULL,
    PRIMARY KEY (station_id),
    CONSTRAINT fk_station_network FOREIGN KEY (network_id) REFERENCES charging_network (network_id),
    CONSTRAINT ck_station_type CHECK (station_type IN ('PUBLIC', 'PRIVATE', 'UNKNOWN')),
    KEY idx_station_network (network_id),
    SPATIAL INDEX spx_station_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 사업자별 유효기간 요금 (현재 공개 API는 조회하지 않음, 향후 정산 기능 대비 스키마만 유지)
CREATE TABLE tariff (
    tariff_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network_id      BIGINT UNSIGNED NOT NULL,
    customer_type   VARCHAR(50) NULL,
    speed_class     VARCHAR(50) NULL,
    price_per_kwh   DECIMAL(10,2) NULL,
    effective_from  DATE NULL,
    effective_to    DATE NULL,
    PRIMARY KEY (tariff_id),
    CONSTRAINT fk_tariff_network FOREIGN KEY (network_id) REFERENCES charging_network (network_id),
    CONSTRAINT ck_tariff_price CHECK (price_per_kwh IS NULL OR price_per_kwh >= 0),
    CONSTRAINT ck_tariff_effective_range CHECK (effective_to IS NULL OR effective_to >= effective_from),
    KEY idx_tariff_network_period (network_id, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 차량 Trim (Model 1:N)
CREATE TABLE vehicle_trim (
    vehicle_trim_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    model_id         BIGINT UNSIGNED NOT NULL,
    trim_name        VARCHAR(100) NOT NULL,
    PRIMARY KEY (vehicle_trim_id),
    CONSTRAINT fk_trim_model FOREIGN KEY (model_id) REFERENCES vehicle_model (model_id),
    KEY idx_trim_model (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 충전기 (Station 1:N)
CREATE TABLE charger (
    charger_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    station_id            BIGINT UNSIGNED NOT NULL,
    external_charger_id   VARCHAR(100) NULL,
    charger_name          VARCHAR(100) NULL,
    charger_type          VARCHAR(50)  NULL,
    max_power_kw          DECIMAL(6,2) NULL,
    PRIMARY KEY (charger_id),
    CONSTRAINT fk_charger_station FOREIGN KEY (station_id) REFERENCES station (station_id),
    CONSTRAINT ck_charger_power CHECK (max_power_kw IS NULL OR max_power_kw >= 0),
    UNIQUE KEY uk_charger_external_id (station_id, external_charger_id),
    KEY idx_charger_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 충전소 ↔ 원천 매칭 (N:M 해소, 복합 PK)
CREATE TABLE station_source_link (
    station_id          BIGINT UNSIGNED NOT NULL,
    source_id           BIGINT UNSIGNED NOT NULL,
    source_station_id   VARCHAR(100) NULL,
    match_method        VARCHAR(50)  NULL,
    match_score         DECIMAL(5,4) NULL,
    PRIMARY KEY (station_id, source_id),
    CONSTRAINT fk_link_station FOREIGN KEY (station_id) REFERENCES station (station_id),
    CONSTRAINT fk_link_source FOREIGN KEY (source_id) REFERENCES data_source (source_id),
    CONSTRAINT ck_link_match_score CHECK (match_score IS NULL OR (match_score >= 0 AND match_score <= 1)),
    UNIQUE KEY uk_link_source_station (source_id, source_station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 차량 Trim ↔ 지원 커넥터 (N:M 해소, 복합 PK)
CREATE TABLE vehicle_connector (
    trim_id          BIGINT UNSIGNED NOT NULL,
    connector_code   VARCHAR(20) NOT NULL,
    charge_mode      VARCHAR(10) NOT NULL,
    is_standard      TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (trim_id, connector_code),
    CONSTRAINT fk_vconn_trim FOREIGN KEY (trim_id) REFERENCES vehicle_trim (vehicle_trim_id),
    CONSTRAINT fk_vconn_connector_type FOREIGN KEY (connector_code) REFERENCES connector_type (connector_code),
    CONSTRAINT ck_vconn_charge_mode CHECK (charge_mode IN ('AC', 'DC', 'UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 충전기 ↔ 제공 커넥터 (N:M 해소, 복합 PK)
CREATE TABLE charger_connector (
    charger_id       BIGINT UNSIGNED NOT NULL,
    connector_code   VARCHAR(20) NOT NULL,
    charge_mode      VARCHAR(10) NOT NULL,
    PRIMARY KEY (charger_id, connector_code),
    CONSTRAINT fk_cconn_charger FOREIGN KEY (charger_id) REFERENCES charger (charger_id),
    CONSTRAINT fk_cconn_connector_type FOREIGN KEY (connector_code) REFERENCES connector_type (connector_code),
    CONSTRAINT ck_cconn_charge_mode CHECK (charge_mode IN ('AC', 'DC', 'UNKNOWN')),
    KEY idx_cconn_connector_code (connector_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 충전기 상태 이력 (Append-only, 덮어쓰기 금지)
CREATE TABLE charger_status_history (
    status_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    charger_id           BIGINT UNSIGNED NOT NULL,
    status               VARCHAR(50) NOT NULL,
    source_updated_at    DATETIME NULL,
    collected_at         DATETIME NULL,
    PRIMARY KEY (status_id),
    CONSTRAINT fk_status_charger FOREIGN KEY (charger_id) REFERENCES charger (charger_id),
    KEY idx_status_charger_time (charger_id, source_updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 최신 충전기 상태 View: source_updated_at → collected_at → status_id 순으로 최신 판정
CREATE VIEW v_charger_latest_status AS
SELECT charger_id, status, source_updated_at, collected_at
FROM (
    SELECT
        h.*,
        ROW_NUMBER() OVER (
            PARTITION BY h.charger_id
            ORDER BY
                h.source_updated_at DESC,
                h.collected_at DESC,
                h.status_id DESC
        ) AS rn
    FROM charger_status_history h
) ranked
WHERE ranked.rn = 1;
