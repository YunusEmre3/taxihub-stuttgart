package com.stuttgarttaxi.taxihub.exception;

public class CustomerEmailAlreadyExistsException extends RuntimeException {

    public CustomerEmailAlreadyExistsException(String message) {
        super(message);
    }
}
