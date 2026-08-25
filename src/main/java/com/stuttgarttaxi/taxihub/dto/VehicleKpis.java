package com.stuttgarttaxi.taxihub.dto;

/**
 * The 4 KPI cards on top of the Fleet Management page.
 */
public record VehicleKpis(
        long totalVehicles,
        long inServiceCount,
        long maintenanceCount,
        long unassignedCount
) {
}
