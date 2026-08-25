package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.VehicleType;

public record VehiclePricingRow(
        VehicleType vehicleType,
        Double baseFare,
        Double pricePerKm
) {
}
