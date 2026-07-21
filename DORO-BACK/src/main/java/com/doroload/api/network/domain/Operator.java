package com.doroload.api.network.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 법인·사업자 (operator Table)
@Entity
@Table(name = "operator")
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    protected Operator() {
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public String getLegalName() {
        return legalName;
    }
}
