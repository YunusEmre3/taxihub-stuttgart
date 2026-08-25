package com.stuttgarttaxi.taxihub.exception;

/**
 * Thrown when an employee tries to assign a booking they can't have -
 * either because they already have an active job, or because someone
 * else just took the same booking. Both map to HTTP 409 in the controller.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
