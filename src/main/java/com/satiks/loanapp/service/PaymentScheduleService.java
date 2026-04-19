package com.satiks.loanapp.service;

import com.satiks.loanapp.domain.LoanApplication;
import com.satiks.loanapp.domain.PaymentScheduleEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates and persists annuity-style payment schedules for loan applications.
 */
@Service
public class PaymentScheduleService {

    private static final BigDecimal MONTHLY_RATE_DIVISOR = BigDecimal.valueOf(1200);
    private static final int MONTHLY_RATE_SCALE = 10;
    private static final int AMORTIZATION_SCALE = 12;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Builds payment schedule entries and attaches them to a loan.
     *
     * @param loan loan application receiving schedule entries
     * @param amount principal amount
     * @param months loan period in months
     * @param annualRate total annual interest rate in percent
     * @return generated payment schedule entries
     * @throws IllegalArgumentException when any input is invalid
     */
    @Transactional
    public List<PaymentScheduleEntry> generateAndSaveSchedule(
            LoanApplication loan,
            BigDecimal amount,
            int months,
            BigDecimal annualRate) {

        validateInputs(loan, amount, months, annualRate);

        BigDecimal monthlyRate = annualRate.divide(MONTHLY_RATE_DIVISOR, MONTHLY_RATE_SCALE, ROUNDING);
        BigDecimal monthlyPayment = calculateMonthlyPayment(amount, monthlyRate, months);
        BigDecimal balance = round(amount);
        LocalDate firstPaymentDate = LocalDate.now();
        List<PaymentScheduleEntry> entries = new ArrayList<>(months);

        for (int i = 1; i <= months; i++) {
            BigDecimal interest = round(balance.multiply(monthlyRate));
            BigDecimal principal = resolvePrincipal(monthlyPayment, interest, balance, i, months);
            balance = round(balance.subtract(principal)).max(BigDecimal.ZERO);

            entries.add(buildEntry(loan, i, firstPaymentDate.plusMonths(i - 1), principal, interest, balance));
        }

        replaceScheduleEntries(loan, entries);
        return entries;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal rate, int months) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), SCALE, ROUNDING);
        }
        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal denominator = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(onePlusRate.pow(months), AMORTIZATION_SCALE, ROUNDING)
        );
        return principal.multiply(rate).divide(denominator, SCALE, ROUNDING);
    }

    private BigDecimal resolvePrincipal(
            BigDecimal monthlyPayment,
            BigDecimal interest,
            BigDecimal balance,
            int installmentNumber,
            int totalInstallments) {
        if (installmentNumber == totalInstallments) {
            return balance;
        }
        return round(monthlyPayment.subtract(interest));
    }

    private PaymentScheduleEntry buildEntry(
            LoanApplication loan,
            int paymentNumber,
            LocalDate paymentDate,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal remainingBalance) {
        return PaymentScheduleEntry.builder()
                .loanApplication(loan)
                .paymentNumber(paymentNumber)
                .paymentDate(paymentDate)
                .principalAmount(principal)
                .interestAmount(interest)
                .totalPayment(round(principal.add(interest)))
                .remainingBalance(remainingBalance)
                .build();
    }

    private void replaceScheduleEntries(LoanApplication loan, List<PaymentScheduleEntry> entries) {
        loan.getPaymentScheduleEntries().clear();
        loan.getPaymentScheduleEntries().addAll(entries);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private void validateInputs(LoanApplication loan, BigDecimal amount, int months, BigDecimal rate) {
        if (loan == null) throw new IllegalArgumentException("Loan application is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Loan amount must be positive");
        if (months <= 0) throw new IllegalArgumentException("Period months must be positive");
        if (rate == null || rate.signum() < 0) throw new IllegalArgumentException("Annual interest rate must be non-negative");
    }
}
