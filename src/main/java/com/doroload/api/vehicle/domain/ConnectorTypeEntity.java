package com.doroload.api.vehicle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 표준 커넥터 Master (connector_type Table, PK=connector_code)
@Entity
@Table(name = "connector_type")
public class ConnectorTypeEntity {

    @Id
    @Column(name = "connector_code")
    private String connectorCode;

    @Column(name = "connector_name", nullable = false)
    private String connectorName;

    protected ConnectorTypeEntity() {
    }

    public String getConnectorCode() {
        return connectorCode;
    }

    public String getConnectorName() {
        return connectorName;
    }
}
