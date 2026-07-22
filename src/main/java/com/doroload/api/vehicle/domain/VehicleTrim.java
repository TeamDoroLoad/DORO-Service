package com.doroload.api.vehicle.domain;

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
import java.util.List;

// 차량 Trim (vehicle_model 1:N vehicle_trim)
@Entity
@Table(name = "vehicle_trim")
public class VehicleTrim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_trim_id")
    private Long vehicleTrimId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private VehicleModel model;

    @Column(name = "trim_name", nullable = false)
    private String trimName;

    @OneToMany(mappedBy = "trim", fetch = FetchType.LAZY)
    private List<VehicleConnector> connectors;

    protected VehicleTrim() {
    }

    public Long getVehicleTrimId() {
        return vehicleTrimId;
    }

    public VehicleModel getModel() {
        return model;
    }

    public String getTrimName() {
        return trimName;
    }

    public List<VehicleConnector> getConnectors() {
        return connectors;
    }
}
