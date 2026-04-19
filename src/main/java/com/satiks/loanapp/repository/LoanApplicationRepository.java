package com.satiks.loanapp.repository;

import com.satiks.loanapp.domain.LoanApplication;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for loan applications.
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    /**
     * Finds active loan applications for a personal ID code.
     *
     * @param code applicant personal ID code
     * @return optional active loan application (status STARTED or IN_REVIEW)
     */
    @Query("""
        select la
        from LoanApplication la
        where la.personalIdCode = :code
              and la.status in (
                  com.satiks.loanapp.domain.EApplicationStatus.STARTED,
                  com.satiks.loanapp.domain.EApplicationStatus.IN_REVIEW
              )
        """)
    Optional<LoanApplication> findActiveApplicationByPersonalIdCode(@Param("code") String code);
}
