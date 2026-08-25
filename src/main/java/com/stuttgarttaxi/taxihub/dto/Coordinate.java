package com.stuttgarttaxi.taxihub.dto;

/**
 * A plain lat/lon pair, always in Leaflet's [lat, lon] order (GeoJSON, which
 * uses [lon, lat], gets flipped to this as soon as it enters RouteService).
 */
public record Coordinate(double lat, double lon) {
}
