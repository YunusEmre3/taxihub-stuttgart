package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.VehicleType;

/**
 * One row of the Vehicle List table. displayStatus/displayStatusCssClass are
 * computed by VehicleService - "In Service" isn't stored anywhere, it's
 * derived from whether assignedDriver currently has an active booking.
 */
public record VehicleRow(
        Long id,
        String plateNumber,
        String model,
        Integer year,
        VehicleType vehicleType,
        String vin,
        Long assignedDriverId,
        String assignedDriverName,
        String displayStatus,
        String displayStatusCssClass
) {
}
