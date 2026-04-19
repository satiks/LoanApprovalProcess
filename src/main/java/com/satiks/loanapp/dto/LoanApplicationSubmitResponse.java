package com.satiks.loanapp.dto;

import java.util.UUID;

/**
 * Response DTO returned immediately after submitting a new loan application.
 * Contains the newly assigned application ID for tracking and retrieving the application status.
 */
public record LoanApplicationSubmitResponse(
        UUID applicationId
) {
}