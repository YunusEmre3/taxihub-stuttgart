package com.stuttgarttaxi.taxihub.dto;

/**
 * The 4 KPI cards shown at the top of the admin dashboard.
 */
public record AdminKpis(
        long totalBookingsToday,
        long activeTrips,
        int employeesOnline,
        long pendingApprovals
) {
}
