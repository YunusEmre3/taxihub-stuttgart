package com.stuttgarttaxi.taxihub.entity;

/**
 * Manually maintained by staff (set to MAINTENANCE when a car goes to the
 * shop, back to AVAILABLE when it's out). "In Service" is deliberately NOT
 * a third value here - it's a display-only state computed from whether the
 * vehicle's assigned driver currently has an active booking (see
 * VehicleService), so it can never drift out of sync with real trip data.
 */
public enum VehicleStatus {
    AVAILABLE,
    MAINTENANCE
}
