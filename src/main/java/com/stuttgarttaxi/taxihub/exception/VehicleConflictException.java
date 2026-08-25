package com.stuttgarttaxi.taxihub.exception;

/**
 * Covers all "this vehicle form can't be saved as-is" cases: duplicate
 * plate/VIN, or a driver who's already assigned to a different vehicle.
 */
public class VehicleConflictException extends RuntimeException {

    public VehicleConflictException(String message) {
        super(message);
    }
}
