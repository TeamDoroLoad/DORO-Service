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
import jakarta.persistence.Transient;
import java.math.BigDecimal;
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

    // Priority 2(주행가능거리·충전시간 계산)용 예약 필드 — DB 컬럼이 아직 없어 @Transient로 보류 (0번 규칙: 스키마는 그대로, 컬럼 생기면 @Column으로 전환)
    @Transient
    private BigDecimal batteryKwh;

    @Transient
    private Integer normalRangeKm;

    @Transient
    private BigDecimal maxAcKw;

    @Transient
    private BigDecimal maxDcKw;

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

    public BigDecimal getBatteryKwh() {
        return batteryKwh;
    }

    public Integer getNormalRangeKm() {
        return normalRangeKm;
    }

    public BigDecimal getMaxAcKw() {
        return maxAcKw;
    }

    public BigDecimal getMaxDcKw() {
        return maxDcKw;
    }

    public List<VehicleConnector> getConnectors() {
        return connectors;
    }
}
