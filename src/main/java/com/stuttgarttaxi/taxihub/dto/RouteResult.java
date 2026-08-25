package com.stuttgarttaxi.taxihub.dto;

import java.util.List;

/**
 * Response body for POST /api/bookings/calculate-route. On failure, only
 * success/errorMessage are set - the frontend shows the message and simply
 * leaves the map/price blank rather than blocking booking creation.
 */
public record RouteResult(
        boolean success,
        String errorMessage,
        Coordinate pickup,
        Coordinate dropoff,
        List<Coordinate> path,
        Double distanceKm,
        Double durationMinutes,
        Double estimatedPrice
) {
    public static RouteResult failure(String errorMessage) {
        return new RouteResult(false, errorMessage, null, null, null, null, null, null);
    }
}
