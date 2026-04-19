package com.satiks.loanapp.exception;

/**
 * Thrown when a requested resource (entity, document, etc.) cannot be found in the system.
 * Typically results in a 404 HTTP response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
