package com.satiks.loanapp.exception;

/**
 * Thrown when a business rule or domain constraint is violated.
 * Represents errors that result from invalid business logic rather than technical issues.
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
}