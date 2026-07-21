package com.doroload.api.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;

// 충전기 (station 1:N charger)
@Entity
@Table(name = "charger")
public class Charger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charger_id")
    private Long chargerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "external_charger_id")
    private String externalChargerId;

    @Column(name = "charger_name")
    private String chargerName;

    @Column(name = "charger_type")
    private String chargerType;

    @Column(name = "max_power_kw")
    private BigDecimal maxPowerKw;

    @OneToMany(mappedBy = "charger", fetch = FetchType.LAZY)
    private List<ChargerConnector> connectors;

    protected Charger() {
    }

    public Long getChargerId() {
        return chargerId;
    }

    public Station getStation() {
        return station;
    }

    public String getExternalChargerId() {
        return externalChargerId;
    }

    public String getChargerName() {
        return chargerName;
    }

    public String getChargerType() {
        return chargerType;
    }

    public BigDecimal getMaxPowerKw() {
        return maxPowerKw;
    }

    public List<ChargerConnector> getConnectors() {
        return connectors;
    }
}
