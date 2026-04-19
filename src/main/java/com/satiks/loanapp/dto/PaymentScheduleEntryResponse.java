package com.satiks.loanapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO representing a single payment in the loan repayment schedule.
 * Includes payment number, due date, principal and interest breakdown,
 * total payment amount, and remaining loan balance.
 */
public record PaymentScheduleEntryResponse(
        Integer paymentNumber,
        LocalDate paymentDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal totalPayment,
        BigDecimal remainingBalance
) {
}