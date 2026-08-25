package com.stuttgarttaxi.taxihub.exception;

public class EmailDomainNotAllowedException extends RuntimeException {

    public EmailDomainNotAllowedException(String message) {
        super(message);
    }
}
