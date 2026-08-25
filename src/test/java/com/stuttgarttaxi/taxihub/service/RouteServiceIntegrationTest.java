package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.RouteResult;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises RouteService against the real Nominatim + OpenRouteService APIs.
 *
 * Disabled by default - it needs network access and a working ORS_API_KEY in
 * .env, so it doesn't run as part of the normal build/CI. Run it manually
 * (remove @Disabled, or run with -Dtest=RouteServiceIntegrationTest) whenever:
 *   - ors.base-url changes (ORS has already moved domains once - see
 *     api.openrouteservice.org -> api.heigit.org)
 *   - route calculation starts failing in the app and you need to confirm
 *     whether it's the ORS API itself vs. something in our code
 *
 * A passing run proves: the configured ors.base-url is reachable, the API
 * key is valid, and the request/response shape our parsing code expects
 * hasn't changed upstream.
 */
@SpringBootTest
class RouteServiceIntegrationTest {

    @Autowired
    private RouteService routeService;

    @Test
    @Disabled("Hits real external APIs - run manually to verify ors.base-url / ORS_API_KEY still work")
    void calculateRoute_returnsARealRouteBetweenTwoStuttgartAddresses() {
        RouteResult result = routeService.calculateRoute("Stuttgart Hauptbahnhof", "Schlossplatz Stuttgart", VehicleType.STANDARD);

        assertThat(result.success())
                .as("Route calculation failed: %s", result.errorMessage())
                .isTrue();
        assertThat(result.distanceKm()).isGreaterThan(0);
        assertThat(result.durationMinutes()).isGreaterThan(0);
        assertThat(result.estimatedPrice()).isGreaterThan(0);
        assertThat(result.path()).isNotEmpty();
    }
}
