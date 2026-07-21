package com.doroload.api.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;

// 충전소 ↔ 원천 매칭 이력 (N:M 해소, 매칭 방식·신뢰도 속성을 가지므로 명시적 Entity로 구현)
@Entity
@Table(name = "station_source_link")
public class StationSourceLink {

    @EmbeddedId
    private StationSourceLinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stationId")
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sourceId")
    @JoinColumn(name = "source_id")
    private DataSource dataSource;

    @Column(name = "source_station_id")
    private String sourceStationId;

    @Column(name = "match_method")
    private String matchMethod;

    @Column(name = "match_score")
    private BigDecimal matchScore;

    protected StationSourceLink() {
    }

    public StationSourceLinkId getId() {
        return id;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getSourceStationId() {
        return sourceStationId;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }
}
