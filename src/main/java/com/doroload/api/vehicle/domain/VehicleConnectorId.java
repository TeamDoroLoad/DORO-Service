package com.doroload.api.vehicle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// vehicle_connector 복합 PK (vehicle_trim_id, connector_code)
@Embeddable
public class VehicleConnectorId implements Serializable {

    @Column(name = "vehicle_trim_id")
    private Long trimId;

    @Column(name = "connector_code")
    private String connectorCode;

    protected VehicleConnectorId() {
    }

    public VehicleConnectorId(Long trimId, String connectorCode) {
        this.trimId = trimId;
        this.connectorCode = connectorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VehicleConnectorId that)) {
            return false;
        }
        return Objects.equals(trimId, that.trimId) && Objects.equals(connectorCode, that.connectorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trimId, connectorCode);
    }
}
