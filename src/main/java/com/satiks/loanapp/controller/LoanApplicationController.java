package com.satiks.loanapp.controller;

import com.satiks.loanapp.dto.LoanApplicationRequest;
import com.satiks.loanapp.dto.LoanApplicationResponse;
import com.satiks.loanapp.dto.LoanApplicationSubmitResponse;
import com.satiks.loanapp.dto.RejectLoanApplicationRequest;
import com.satiks.loanapp.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes REST endpoints for submitting, querying, and deciding loan applications.
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    /**
     * Submits a new loan application and returns the created application identifier.
     *
     * @param request loan application payload
     * @return HTTP 201 response containing the new application ID
     * @throws com.satiks.loanapp.exception.BusinessException when business rules reject submission
     */
    @PostMapping
    @Operation(summary = "Submit new loan application")
    @ApiResponse(responseCode = "201", description = "Loan application submitted")
    @ApiResponse(responseCode = "400", description = "Validation or business rule error")
    @ApiResponse(responseCode = "409", description = "Active application already exists for this personal ID code")
    public ResponseEntity<LoanApplicationSubmitResponse> create(
            @Parameter(description = "Loan application payload")
            @Valid @RequestBody LoanApplicationRequest request) {
        UUID id = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoanApplicationSubmitResponse(id));
    }

    /**
     * Retrieves a loan application with its payment schedule entries.
     *
     * @param id loan application identifier
     * @return loan application details
     * @throws com.satiks.loanapp.exception.ResourceNotFoundException when the application does not exist
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get loan application with payment schedule")
    @ApiResponse(responseCode = "200", description = "Loan application found")
    @ApiResponse(responseCode = "404", description = "Loan application not found")
    public LoanApplicationResponse getById(@Parameter(description = "Loan application ID") @PathVariable UUID id) {
        return service.getById(id);
    }

    /**
     * Approves an in-review loan application.
     *
     * @param id loan application identifier
     * @return updated loan application details
     * @throws com.satiks.loanapp.exception.ResourceNotFoundException when the application does not exist
     * @throws com.satiks.loanapp.exception.BusinessException when the application is not in review status
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve loan application")
    @ApiResponse(responseCode = "200", description = "Loan application approved")
    @ApiResponse(responseCode = "404", description = "Loan application not found")
    @ApiResponse(responseCode = "409", description = "Application is not in IN_REVIEW status")
    public LoanApplicationResponse approve(@Parameter(description = "Loan application ID") @PathVariable UUID id) {
        return service.approve(id);
    }

    /**
     * Rejects an in-review loan application with a rejection reason.
     *
     * @param id loan application identifier
     * @param request rejection request containing the manual rejection reason
     * @return updated loan application details
     * @throws com.satiks.loanapp.exception.ResourceNotFoundException when the application does not exist
     * @throws com.satiks.loanapp.exception.BusinessException when the application is not in review status
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject loan application")
    @ApiResponse(responseCode = "200", description = "Loan application rejected")
    @ApiResponse(responseCode = "404", description = "Loan application not found")
    @ApiResponse(responseCode = "409", description = "State conflict")
    @ApiResponse(responseCode = "400", description = "Business rule violation")
    public LoanApplicationResponse reject(
            @Parameter(description = "Loan application ID")
            @PathVariable UUID id,
            @Parameter(description = "Rejection reason payload")
            @Valid @RequestBody RejectLoanApplicationRequest request) {
        return service.reject(id, request.reason());
    }
}
