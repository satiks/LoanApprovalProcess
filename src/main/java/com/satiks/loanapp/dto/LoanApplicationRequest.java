package com.satiks.loanapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for creating a new loan application.
 * Contains applicant personal information, loan parameters (amount and period),
 * and interest rate details (base rate and margin).
 */
public record LoanApplicationRequest(
        @NotBlank @Size(max = 32) String firstName,
        @NotBlank @Size(max = 32) String lastName,
        @NotBlank @Pattern(regexp = "^\\d{11}$") String personalIdCode,
        @NotNull @Min(6) @Max(360) Integer loanPeriodMonths,
        @NotNull @DecimalMin(value = "5000.00") BigDecimal loanAmount,
        @DecimalMin(value = "0.00") BigDecimal baseInterestRate,
        @NotNull @DecimalMin(value = "0.00") BigDecimal interestMargin
) {
}
