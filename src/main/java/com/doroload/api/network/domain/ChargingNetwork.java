package com.doroload.api.network.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// 충전 네트워크 (operator 1:N charging_network)
@Entity
@Table(name = "charging_network")
public class ChargingNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "network_id")
    private Long networkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(name = "network_name", nullable = false)
    private String networkName;

    protected ChargingNetwork() {
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getNetworkName() {
        return networkName;
    }
}
