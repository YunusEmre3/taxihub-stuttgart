package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.RouteRequest;
import com.stuttgarttaxi.taxihub.dto.RouteResult;
import com.stuttgarttaxi.taxihub.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the map on the create-booking page. Kept separate from
 * BookingController since this is a JSON API endpoint, not a page route.
 */
@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/api/bookings/calculate-route")
    public RouteResult calculateRoute(@Valid @RequestBody RouteRequest request) {
        return routeService.calculateRoute(request.pickupAddress(), request.dropoffAddress(), request.vehicleType());
    }
}
