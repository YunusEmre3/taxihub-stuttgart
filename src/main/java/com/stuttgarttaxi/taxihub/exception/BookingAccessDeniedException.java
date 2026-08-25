package com.stuttgarttaxi.taxihub.exception;

/**
 * Thrown when an employee tries to act on a booking assigned to someone else.
 * Maps to HTTP 403 in the controller.
 */
public class BookingAccessDeniedException extends RuntimeException {

    public BookingAccessDeniedException(String message) {
        super(message);
    }
}
