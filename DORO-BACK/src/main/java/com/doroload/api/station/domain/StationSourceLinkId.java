package com.doroload.api.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// station_source_link 복합 PK (station_id, source_id)
@Embeddable
public class StationSourceLinkId implements Serializable {

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "source_id")
    private Long sourceId;

    protected StationSourceLinkId() {
    }

    public StationSourceLinkId(Long stationId, Long sourceId) {
        this.stationId = stationId;
        this.sourceId = sourceId;
    }

    public Long getStationId() {
        return stationId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StationSourceLinkId that)) {
            return false;
        }
        return Objects.equals(stationId, that.stationId) && Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationId, sourceId);
    }
}
