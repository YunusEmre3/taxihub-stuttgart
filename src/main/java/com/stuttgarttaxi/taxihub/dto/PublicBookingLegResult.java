package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.LegType;

import java.time.LocalDate;
import java.time.LocalTime;

public record PublicBookingLegResult(
        String bookingCode,
        LegType legType,
        String pickupAddress,
        String dropoffAddress,
        LocalDate date,
        LocalTime time,
        Double estimatedPrice
) {
}
