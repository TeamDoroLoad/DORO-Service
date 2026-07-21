package com.doroload.api.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// charger_connector 복합 PK (charger_id, connector_code)
@Embeddable
public class ChargerConnectorId implements Serializable {

    @Column(name = "charger_id")
    private Long chargerId;

    @Column(name = "connector_code")
    private String connectorCode;

    protected ChargerConnectorId() {
    }

    public ChargerConnectorId(Long chargerId, String connectorCode) {
        this.chargerId = chargerId;
        this.connectorCode = connectorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChargerConnectorId that)) {
            return false;
        }
        return Objects.equals(chargerId, that.chargerId) && Objects.equals(connectorCode, that.connectorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chargerId, connectorCode);
    }
}
