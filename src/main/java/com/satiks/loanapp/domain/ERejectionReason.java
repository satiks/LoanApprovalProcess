package com.satiks.loanapp.domain;

/**
 * Enumeration of possible reasons for rejecting a loan application.
 * Distinguishes between policy-driven rejections (age limits) and manual business decisions.
 */
public enum ERejectionReason {
    CUSTOMER_TOO_OLD,
    MANUAL_REJECTION
}