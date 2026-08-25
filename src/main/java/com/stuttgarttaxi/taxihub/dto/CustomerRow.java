package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.AccountType;

import java.time.LocalDateTime;

/**
 * One row of the Customer Database table: the stored Customer profile plus
 * ride stats computed live from the bookings table (see CustomerService).
 */
public record CustomerRow(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        AccountType accountType,
        long totalRides,
        LocalDateTime lastRideAt
) {
}
