package com.stuttgarttaxi.taxihub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stuttgarttaxi.taxihub.dto.Coordinate;
import com.stuttgarttaxi.taxihub.dto.RouteResult;
import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns two free-text addresses into a driving route: geocodes both via
 * Nominatim (OpenStreetMap), then asks OpenRouteService for the driving
 * directions between them. Both are free, keyless-for-Nominatim public
 * APIs - the ORS API key is the only secret involved, and it never leaves
 * the backend.
 */
@Slf4j
@Service
public class RouteService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    // Nominatim's usage policy requires a descriptive User-Agent and no more
    // than ~1 request/second - see sleepBetweenNominatimCalls() below.
    private static final String NOMINATIM_USER_AGENT = "TaxiHubStuttgart/1.0 (dispatch@stuttgart-taxi.com)";

    // Geocoding bias: booking addresses are typically just a Stuttgart street/
    // district name without the city, which Nominatim can otherwise match to
    // a same-named street anywhere in the world.
    private static final String GEOCODING_REGION_HINT = ", Stuttgart, Germany";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PricingService pricingService;
    private final String orsBaseUrl;
    private final String orsApiKey;

    public RouteService(ObjectMapper objectMapper,
                         PricingService pricingService,
                         @Value("${ors.base-url}") String orsBaseUrl,
                         @Value("${ors.api-key}") String orsApiKey) {
        this.objectMapper = objectMapper;
        this.pricingService = pricingService;
        this.orsBaseUrl = orsBaseUrl;
        this.orsApiKey = orsApiKey;
        this.restClient = RestClient.create();
    }

    public RouteResult calculateRoute(String pickupAddress, String dropoffAddress, VehicleType vehicleType) {
        if (orsApiKey == null || orsApiKey.isBlank()) {
            log.warn("ORS_API_KEY tanımlı değil; rota hesaplanamıyor.");
            return RouteResult.failure("Rota servisi yapılandırılmamış.");
        }

        Coordinate pickupCoordinate = geocode(pickupAddress);
        if (pickupCoordinate == null) {
            return RouteResult.failure("Alış adresi bulunamadı, lütfen adresi kontrol edin.");
        }

        sleepBetweenNominatimCalls();

        Coordinate dropoffCoordinate = geocode(dropoffAddress);
        if (dropoffCoordinate == null) {
            return RouteResult.failure("Varış adresi bulunamadı, lütfen adresi kontrol edin.");
        }

        PricingRule pricingRule = pricingService.getRuleFor(vehicleType != null ? vehicleType : VehicleType.STANDARD);
        return fetchDirections(pickupCoordinate, dropoffCoordinate, pricingRule);
    }

    private Coordinate geocode(String address) {
        try {
            // Built via UriBuilder (not string concatenation) so the query value is
            // percent-encoded correctly - manual URLEncoder + RestClient's own
            // encoding pass double-encode "+" into a literal plus sign, which makes
            // Nominatim return zero matches instead of raising an error.
            String responseBody = restClient.get()
                    .uri(NOMINATIM_URL, uriBuilder -> uriBuilder
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .queryParam("q", address + GEOCODING_REGION_HINT)
                            .build())
                    .header(HttpHeaders.USER_AGENT, NOMINATIM_USER_AGENT)
                    .retrieve()
                    .body(String.class);

            JsonNode results = objectMapper.readTree(responseBody);
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }

            JsonNode firstMatch = results.get(0);
            return new Coordinate(firstMatch.get("lat").asDouble(), firstMatch.get("lon").asDouble());
        } catch (Exception ex) {
            log.error("Geocoding başarısız oldu ('{}'): {}", address, ex.getMessage());
            return null;
        }
    }

    private RouteResult fetchDirections(Coordinate pickup, Coordinate dropoff, PricingRule pricingRule) {
        try {
            // ORS expects [longitude, latitude] pairs, the opposite of our Coordinate order.
            Map<String, Object> requestBody = Map.of("coordinates", List.of(
                    List.of(pickup.lon(), pickup.lat()),
                    List.of(dropoff.lon(), dropoff.lat())
            ));

            // The plain /driving-car endpoint returns ORS's own JSON shape
            // (routes[].geometry as an encoded polyline string). Our parsing
            // below expects a GeoJSON FeatureCollection instead, which only
            // the /geojson variant of the endpoint returns.
            String directionsUrl = orsBaseUrl + "/v2/directions/driving-car/geojson";

            // Diagnostic: confirm @Value actually resolved ORS_API_KEY from .env before
            // firing the request - masked, but enough to compare against .env by eye.
            log.info("ORS isteği gönderiliyor. URL: {}, Authorization header (maskelenmiş): {}",
                    directionsUrl, maskKey(orsApiKey));

            String responseBody = restClient.post()
                    .uri(directionsUrl)
                    .header(HttpHeaders.AUTHORIZATION, orsApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode feature = objectMapper.readTree(responseBody).path("features").get(0);
            JsonNode segment = feature.path("properties").path("segments").get(0);
            JsonNode geometryCoordinates = feature.path("geometry").path("coordinates");

            double distanceKm = segment.path("distance").asDouble() / 1000.0;
            double durationMinutes = segment.path("duration").asDouble() / 60.0;
            double estimatedPrice = pricingRule.getBaseFare() + (distanceKm * pricingRule.getPricePerKm());

            List<Coordinate> path = new ArrayList<>();
            for (JsonNode point : geometryCoordinates) {
                // GeoJSON gives [lon, lat] - flip to Leaflet's [lat, lon].
                path.add(new Coordinate(point.get(1).asDouble(), point.get(0).asDouble()));
            }

            return new RouteResult(true, null, pickup, dropoff, path,
                    roundToTwoDecimals(distanceKm), roundToTwoDecimals(durationMinutes), roundToTwoDecimals(estimatedPrice));
        } catch (Exception ex) {
            log.error("OpenRouteService rota isteği başarısız oldu: {}", ex.getMessage());
            return RouteResult.failure("Rota hesaplanamadı, lütfen adresleri kontrol edin.");
        }
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100) / 100.0;
    }

    /**
     * Shows a short prefix and suffix with the middle hidden, e.g.
     * "eyJvcmci...murmur64In0=". Enough to eyeball-compare against .env
     * without printing the full, usable key to the console.
     */
    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "(boş)";
        }
        if (key.length() <= 16) {
            return "*".repeat(key.length());
        }
        return key.substring(0, 8) + "..." + key.substring(key.length() - 6);
    }

    private void sleepBetweenNominatimCalls() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
