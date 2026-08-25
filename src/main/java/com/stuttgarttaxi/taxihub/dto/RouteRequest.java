package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;

public record RouteRequest(
        @NotBlank(message = "Alış adresi zorunludur") String pickupAddress,
        @NotBlank(message = "Varış adresi zorunludur") String dropoffAddress,
        VehicleType vehicleType
) {
}
