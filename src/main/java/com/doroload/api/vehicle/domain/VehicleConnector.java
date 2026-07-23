package com.doroload.api.vehicle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

// 차량 Trim이 지원하는 커넥터 (N:M 해소, 관계 이외 is_standard 속성을 가지므로 명시적 Entity로 구현)
@Entity
@Table(name = "vehicle_connector")
public class VehicleConnector {

    @EmbeddedId
    private VehicleConnectorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("trimId")
    @JoinColumn(name = "trim_id")
    private VehicleTrim trim;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("connectorCode")
    @JoinColumn(name = "connector_code")
    private ConnectorTypeEntity connectorType;

    @Column(name = "is_standard", nullable = false)
    private boolean standard;

    protected VehicleConnector() {
    }

    public VehicleTrim getTrim() {
        return trim;
    }

    public ConnectorTypeEntity getConnectorType() {
        return connectorType;
    }

    public boolean isStandard() {
        return standard;
    }
}
