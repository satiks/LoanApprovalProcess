CREATE TABLE loan_application (
    id UUID PRIMARY KEY,
    first_name VARCHAR(32) NOT NULL,
    last_name VARCHAR(32) NOT NULL,
    personal_id_code VARCHAR(11) NOT NULL,
    loan_period_months INTEGER NOT NULL,
    interest_margin NUMERIC(10, 4) NOT NULL,
    base_interest_rate NUMERIC(10, 4) NOT NULL,
    loan_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_loan_period_months_positive CHECK (loan_period_months > 0),
    CONSTRAINT chk_interest_margin_non_negative CHECK (interest_margin >= 0),
    CONSTRAINT chk_base_interest_rate_non_negative CHECK (base_interest_rate >= 0),
    CONSTRAINT chk_loan_amount_positive CHECK (loan_amount > 0),
    CONSTRAINT chk_loan_application_status CHECK (status IN ('STARTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_rejection_reason CHECK (
        rejection_reason IS NULL
        OR rejection_reason IN ('CUSTOMER_TOO_OLD', 'MANUAL_REJECTION')
    ),
    CONSTRAINT chk_personal_id_code_estonian_format CHECK (personal_id_code ~ '^[0-9]{11}$')
);

CREATE TABLE payment_schedule_entry (
    id UUID PRIMARY KEY,
    loan_application_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    payment_number INTEGER NOT NULL,
    principal_amount NUMERIC(19, 2) NOT NULL,
    interest_amount NUMERIC(19, 2) NOT NULL,
    total_payment NUMERIC(19, 2) NOT NULL,
    remaining_balance NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_payment_schedule_entry_loan_application
        FOREIGN KEY (loan_application_id)
        REFERENCES loan_application (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_payment_number_positive CHECK (payment_number > 0),
    CONSTRAINT chk_principal_amount_non_negative CHECK (principal_amount >= 0),
    CONSTRAINT chk_interest_amount_non_negative CHECK (interest_amount >= 0),
    CONSTRAINT chk_total_payment_non_negative CHECK (total_payment >= 0),
    CONSTRAINT chk_remaining_balance_non_negative CHECK (remaining_balance >= 0)
);

CREATE UNIQUE INDEX uq_active_loan_application_per_personal_id_code
    ON loan_application (personal_id_code)
    WHERE status IN ('STARTED', 'IN_REVIEW');
