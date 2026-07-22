package com.doroload.api.station.domain;

import com.doroload.api.vehicle.domain.ConnectorTypeEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

// 충전기가 제공하는 커넥터 (N:M 해소)
@Entity
@Table(name = "charger_connector")
public class ChargerConnector {

    @EmbeddedId
    private ChargerConnectorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chargerId")
    @JoinColumn(name = "charger_id")
    private Charger charger;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("connectorCode")
    @JoinColumn(name = "connector_code")
    private ConnectorTypeEntity connectorType;

    protected ChargerConnector() {
    }

    public Charger getCharger() {
        return charger;
    }

    public ConnectorTypeEntity getConnectorType() {
        return connectorType;
    }
}
