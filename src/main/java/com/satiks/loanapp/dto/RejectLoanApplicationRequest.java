package com.satiks.loanapp.dto;

import com.satiks.loanapp.domain.ERejectionReason;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for rejecting a pending loan application.
 */
public record RejectLoanApplicationRequest(
        @NotNull ERejectionReason reason
) {
}