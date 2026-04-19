package com.satiks.loanapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Applies configurable age limits for loan applicants.
 */
@Service
public class AgeValidationService {

    private final int maxCustomerAge;

    /**
     * Creates age validation service with configurable maximum customer age.
     *
     * @param maxCustomerAge maximum allowed customer age in years
     */
    public AgeValidationService(@Value("${loan.max-customer-age}") int maxCustomerAge) {
        this.maxCustomerAge = maxCustomerAge;
    }

    /**
     * Checks whether applicant age exceeds configured maximum age.
     *
     * @param applicantAge applicant age in years
     * @return true when applicant is older than allowed threshold
     */
    public boolean isTooOld(int applicantAge) {
        return applicantAge > maxCustomerAge;
    }
}