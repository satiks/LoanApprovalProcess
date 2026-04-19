package com.satiks.loanapp.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "loan_application")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 32)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 32)
    private String lastName;

    @Column(name = "personal_id_code", nullable = false, length = 11)
    private String personalIdCode;

    @Column(name = "loan_period_months", nullable = false)
    private Integer loanPeriodMonths;

    @Column(name = "interest_margin", nullable = false, precision = 10, scale = 4)
    private BigDecimal interestMargin;

    @Column(name = "base_interest_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal baseInterestRate;

    @Column(name = "loan_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 50)
    private ERejectionReason rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentScheduleEntry> paymentScheduleEntries = new ArrayList<>();
}
