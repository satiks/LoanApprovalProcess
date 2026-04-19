# Loan Approval Process

Spring Boot service for collecting loan applications, generating payment schedules, and supporting a simple approve/reject workflow.

## Purpose

The application provides:
- Loan application submission with field-level validation.
- Estonian personal ID code validation (format, checksum, birth date extraction).
- Automatic age-based rejection for applicants over a configurable threshold.
- Payment schedule generation for eligible applications.
- Decision endpoints to approve or reject applications in review.

## Architecture Overview

Main layers:
- `controller`: HTTP endpoints for loan application workflow.
- `service`: business logic and orchestration.
- `repository`: Spring Data JPA persistence.
- `domain`: JPA entities and enums.
- `dto`: API request/response models.
- `exception`: business and not-found exceptions + global error handler.
- `config`: OpenAPI metadata configuration.

Data flow summary:
1. Client submits loan application to `POST /api/loans`.
2. `LoanApplicationService` checks for active application by personal ID.
3. `EstonianIdCodeService` validates ID code and derives age.
4. If applicant is too old, status is set to `REJECTED` with reason `CUSTOMER_TOO_OLD`.
5. Otherwise `PaymentScheduleService` generates and saves schedule entries, and application moves to `IN_REVIEW`.
6. `POST /api/loans/{id}/approve` or `POST /api/loans/{id}/reject` finalizes the decision.

Database:
- Managed by Flyway migration `V1__init.sql`.
- Main tables: `loan_application`, `payment_schedule_entry`.
- Includes partial unique index to allow one active (`STARTED` or `IN_REVIEW`) application per personal ID code.

## Build And Run Locally

### Prerequisites
- Java 25
- Maven (or use Maven Wrapper)
- PostgreSQL 16 (or Docker)

### Option 1: Run With Docker Compose

```bash
docker compose up --build
```

This starts:
- PostgreSQL on `localhost:5433` by default (override with `POSTGRES_HOST_PORT`)
- Application on `localhost:8080`

### Option 2: Run Database In Docker, App Locally

Start database:

```bash
docker compose up postgres
```

If you run the app locally against the Compose database, use:

```text
DB_URL=jdbc:postgresql://localhost:5433/loanapp
```

Run application from project root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### Package JAR

```bash
./mvnw clean package
```

## Configuration

Configuration is defined in `src/main/resources/application.yml`.

### Environment Variables

- `DB_URL` (default: `jdbc:postgresql://localhost:5432/loanapp`)
- `DB_USERNAME` (default: `loanuser`)
- `DB_PASSWORD` (default: `loanpass`)
- `SPRING_DATASOURCE_URL` (optional direct Spring property override; used by `docker-compose.yaml` for container-to-container DB access)

### Application Properties

- `server.port` (default: `8080`)
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.flyway.enabled=true`
- `springdoc.api-docs.path=/api-docs`
- `springdoc.swagger-ui.path=/swagger-ui.html`
- `loan.max-customer-age` (default: `70`)
- `loan.base-interest-rate` (configured default: `3.5000`)

## API Endpoint Summary

Base path: `/api/loans`

- `POST /api/loans`
  - Creates a loan application.
  - Returns `201 Created` with application ID.
- `GET /api/loans/{id}`
  - Returns application details including payment schedule.
  - Returns `404` when not found.
- `POST /api/loans/{id}/approve`
  - Approves an application in `IN_REVIEW` status.
  - Returns `400` when status transition is invalid.
- `POST /api/loans/{id}/reject`
  - Rejects an application in `IN_REVIEW` status using request reason.
  - Returns `400` when status transition is invalid or request is invalid.

Swagger/OpenAPI:
- JSON spec: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Ambiguities To Clarify

The following points are observable in the current code and may need product clarification:

1. `loan.base-interest-rate` is present in configuration but is not injected in services; API clients currently provide `baseInterestRate` in each request.
2. `PaymentScheduleService` uses `LocalDate.now()` as the first payment date reference. Time zone/business-day adjustment rules are not specified.
3. Rejection endpoint accepts any `ERejectionReason`, including `CUSTOMER_TOO_OLD`, while age-based rejection is also applied automatically during creation.
