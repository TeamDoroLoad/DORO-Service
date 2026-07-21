package com.doroload.api.station.domain;

import com.doroload.api.network.domain.ChargingNetwork;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.locationtech.jts.geom.Point;

// 충전소 (charging_network 1:N station). 원본 비즈니스 데이터의 유일한 출처는 MySQL이며 이 Entity는 조회 전용이다.
@Entity
@Table(name = "station")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Long stationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private ChargingNetwork network;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "station_type", nullable = false)
    private String stationType;

    @Column(name = "location", nullable = false, columnDefinition = "POINT SRID 4326")
    private Point location;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Station() {
    }

    public Long getStationId() {
        return stationId;
    }

    public ChargingNetwork getNetwork() {
        return network;
    }

    public String getStationName() {
        return stationName;
    }

    public String getAddress() {
        return address;
    }

    public String getStationType() {
        return stationType;
    }

    public Point getLocation() {
        return location;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
