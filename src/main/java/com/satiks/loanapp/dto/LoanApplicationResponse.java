package com.satiks.loanapp.dto;

import com.satiks.loanapp.domain.EApplicationStatus;
import com.satiks.loanapp.domain.ERejectionReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO providing complete details of a loan application.
 * Includes applicant information, loan parameters, current application status,
 * rejection reason if applicable, and the calculated payment schedule.
 */
public record LoanApplicationResponse(
        UUID id,
        String firstName,
        String lastName,
        String personalIdCode,
        Integer loanPeriodMonths,
        BigDecimal loanAmount,
        BigDecimal interestMargin,
        BigDecimal baseInterestRate,
        EApplicationStatus status,
        ERejectionReason rejectionReason,
        LocalDateTime createdAt,
        List<PaymentScheduleEntryResponse> paymentSchedule
) {
}
