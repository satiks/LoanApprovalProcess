package com.satiks.loanapp.service;

import com.satiks.loanapp.domain.EApplicationStatus;
import com.satiks.loanapp.domain.ERejectionReason;
import com.satiks.loanapp.domain.LoanApplication;
import com.satiks.loanapp.domain.PaymentScheduleEntry;
import com.satiks.loanapp.dto.LoanApplicationRequest;
import com.satiks.loanapp.dto.LoanApplicationResponse;
import com.satiks.loanapp.dto.PaymentScheduleEntryResponse;
import com.satiks.loanapp.exception.BusinessException;
import com.satiks.loanapp.exception.ResourceNotFoundException;
import com.satiks.loanapp.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates loan application lifecycle operations and domain validations.
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final EstonianIdCodeService estonianIdCodeService;
    private final AgeValidationService ageValidationService;
    private final PaymentScheduleService paymentScheduleService;
    private final BigDecimal defaultBaseInterestRate;

    public LoanApplicationService(
            LoanApplicationRepository loanApplicationRepository,
            EstonianIdCodeService estonianIdCodeService,
            AgeValidationService ageValidationService,
            PaymentScheduleService paymentScheduleService,
            @Value("${loan.base-interest-rate}") BigDecimal defaultBaseInterestRate) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.estonianIdCodeService = estonianIdCodeService;
        this.ageValidationService = ageValidationService;
        this.paymentScheduleService = paymentScheduleService;
        this.defaultBaseInterestRate = defaultBaseInterestRate;
    }

    /**
     * Creates a loan application and prepares payment schedule for in-review applications.
     *
     * @param request loan application request data
     * @return identifier of the persisted loan application
     * @throws BusinessException when an active application already exists or ID code is invalid
     */
    @Transactional
    public UUID create(LoanApplicationRequest request) {
        validateNoActiveApplication(request.personalIdCode());

        estonianIdCodeService.validate(request.personalIdCode());
        int age = estonianIdCodeService.calculateAgeInYears(request.personalIdCode());

        LoanApplication loanApplication = buildLoanApplication(
            request,
            ageValidationService.isTooOld(age),
            resolveBaseInterestRate(request.baseInterestRate())
        );

        if (loanApplication.getStatus() == EApplicationStatus.REJECTED) {
            return loanApplicationRepository.save(loanApplication).getId();
        }

        BigDecimal annualRate = loanApplication.getBaseInterestRate().add(loanApplication.getInterestMargin());
        paymentScheduleService.generateAndSaveSchedule(
                loanApplication,
                loanApplication.getLoanAmount(),
                loanApplication.getLoanPeriodMonths(),
                annualRate
        );

        loanApplication.setStatus(EApplicationStatus.IN_REVIEW);
        return loanApplicationRepository.save(loanApplication).getId();
    }

    /**
     * Finds a loan application by ID and maps it to API response format.
     *
     * @param id loan application identifier
     * @return loan application response with payment schedule
     * @throws ResourceNotFoundException when the application is not found
     */
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(UUID id) {
        LoanApplication loanApplication = findById(id);
        return mapToResponse(loanApplication);
    }

    /**
     * Approves a loan application that is currently in review.
     *
     * @param id loan application identifier
     * @return updated loan application response
     * @throws ResourceNotFoundException when the application is not found
     * @throws BusinessException when the application status is not in review
     */
    @Transactional
    public LoanApplicationResponse approve(UUID id) {
        return changeDecision(id, EApplicationStatus.APPROVED, null);
    }

    /**
     * Rejects a loan application that is currently in review.
     *
     * @param id loan application identifier
     * @param reason rejection reason code
     * @return updated loan application response
     * @throws ResourceNotFoundException when the application is not found
     * @throws BusinessException when the application status is not in review
     */
    @Transactional
    public LoanApplicationResponse reject(UUID id, ERejectionReason reason) {
        return changeDecision(id, EApplicationStatus.REJECTED, reason);
    }

    private LoanApplication findById(UUID id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + id));
    }

    private void validateNoActiveApplication(String personalIdCode) {
        loanApplicationRepository.findActiveApplicationByPersonalIdCode(personalIdCode)
                .ifPresent(existing -> {
                    throw new BusinessException("Active loan application already exists for this personal ID code");
                });
    }

    private BigDecimal resolveBaseInterestRate(BigDecimal requestedBaseInterestRate) {
        return requestedBaseInterestRate != null ? requestedBaseInterestRate : defaultBaseInterestRate;
    }

    private LoanApplication buildLoanApplication(
            LoanApplicationRequest request,
            boolean customerTooOld,
            BigDecimal baseInterestRate) {
        EApplicationStatus status = customerTooOld ? EApplicationStatus.REJECTED : EApplicationStatus.STARTED;
        ERejectionReason rejectionReason = customerTooOld ? ERejectionReason.CUSTOMER_TOO_OLD : null;

        return LoanApplication.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .personalIdCode(request.personalIdCode())
                .loanPeriodMonths(request.loanPeriodMonths())
                .interestMargin(request.interestMargin())
                .baseInterestRate(baseInterestRate)
                .loanAmount(request.loanAmount())
                .status(status)
                .rejectionReason(rejectionReason)
                .build();
    }

    private LoanApplicationResponse changeDecision(
            UUID id,
            EApplicationStatus newStatus,
            ERejectionReason rejectionReason) {
        LoanApplication loanApplication = findById(id);
        ensureInReviewStatus(loanApplication.getStatus());

        loanApplication.setStatus(newStatus);
        loanApplication.setRejectionReason(rejectionReason);

        return mapToResponse(loanApplicationRepository.save(loanApplication));
    }

    private void ensureInReviewStatus(EApplicationStatus status) {
        if (status != EApplicationStatus.IN_REVIEW) {
            throw new BusinessException("Only in-review applications can be approved or rejected");
        }
    }

    private LoanApplicationResponse mapToResponse(LoanApplication loanApplication) {
        List<PaymentScheduleEntryResponse> schedule = loanApplication.getPaymentScheduleEntries() == null
            ? List.of()
            : loanApplication.getPaymentScheduleEntries().stream()
                .map(this::mapEntry)
                .toList();

        return new LoanApplicationResponse(
                loanApplication.getId(),
                loanApplication.getFirstName(),
                loanApplication.getLastName(),
                loanApplication.getPersonalIdCode(),
                loanApplication.getLoanPeriodMonths(),
                loanApplication.getLoanAmount(),
                loanApplication.getInterestMargin(),
                loanApplication.getBaseInterestRate(),
                loanApplication.getStatus(),
                loanApplication.getRejectionReason(),
                loanApplication.getCreatedAt(),
                schedule
        );
    }

    private PaymentScheduleEntryResponse mapEntry(PaymentScheduleEntry entry) {
        return new PaymentScheduleEntryResponse(
                entry.getPaymentNumber(),
                entry.getPaymentDate(),
                entry.getPrincipalAmount(),
                entry.getInterestAmount(),
                entry.getTotalPayment(),
                entry.getRemainingBalance()
        );
    }
}