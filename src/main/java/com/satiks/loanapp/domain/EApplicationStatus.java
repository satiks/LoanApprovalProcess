package com.satiks.loanapp.domain;

/**
 * Enumeration of possible states for a loan application throughout its lifecycle.
 * Represents the progression from initial submission through review to final approval or rejection.
 */
public enum EApplicationStatus {
    STARTED,
    IN_REVIEW,
    APPROVED,
    REJECTED
}