package com.doroload.api.vehicle.api.dto;

import java.util.List;

// GET /vehicle-trims 응답 (REST 명세서 6.1)
public record VehicleTrimListResponse(
        List<VehicleTrimItem> items, int page, int size, long totalElements, int totalPages) {

    public record VehicleTrimItem(
            Long vehicleTrimId,
            Long brandId,
            String brandName,
            Long modelId,
            String modelName,
            String trimName,
            List<VehicleConnectorItem> connectors) {
    }

    public record VehicleConnectorItem(String connectorCode, String connectorName, boolean isStandard) {
    }
}
