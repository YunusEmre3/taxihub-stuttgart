package com.stuttgarttaxi.taxihub.dto;

import java.util.List;

/**
 * Everything the Reports page's charts need, computed live from the
 * bookings table for whatever date range / vehicle type filter is active.
 * No monetary figures - Booking doesn't persist a price, so a "revenue"
 * number here would just be made up. Every field is something we can
 * actually count.
 */
public record ReportData(
        List<String> monthlyLabels,
        List<Long> monthlyBookingVolume,
        List<String> hourLabels,
        List<Long> hourlyRideVolume,
        List<String> vehicleTypeLabels,
        List<Long> vehicleTypeCounts,
        List<String> topDriverNames,
        List<Long> topDriverCompletedRides,
        double avgPassengersPerRide,
        double completionRatePercent,
        long activeDriverCount
) {
}
