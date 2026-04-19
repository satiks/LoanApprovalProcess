package com.satiks.loanapp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates application exceptions into structured HTTP error responses.
 * Handles business rule violations, validation failures, not-found errors,
 * type mismatches, and unexpected technical errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<SimpleErrorResponse> handleBusiness(BusinessException ex) {
        return simpleError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        return validationError(errors);
    }

    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<SimpleErrorResponse> handleEntityNotFound(RuntimeException ex) {
                return simpleError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<SimpleErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
                return simpleError(HttpStatus.BAD_REQUEST, "Invalid application ID format");
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<SimpleErrorResponse> handleBadRequest(RuntimeException ex) {
                return simpleError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SimpleErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
                return simpleError(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<SimpleErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
                return simpleError(HttpStatus.CONFLICT, "A conflicting record already exists");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleErrorResponse> handleUnexpected(Exception ex) {
                return simpleError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

        private ResponseEntity<SimpleErrorResponse> simpleError(HttpStatus status, String message) {
                return ResponseEntity.status(status).body(new SimpleErrorResponse(status.getReasonPhrase(), message));
        }

        private ResponseEntity<ValidationErrorResponse> validationError(List<FieldValidationError> errors) {
                HttpStatus status = HttpStatus.BAD_REQUEST;
                return ResponseEntity.status(status).body(new ValidationErrorResponse(status.getReasonPhrase(), errors));
        }

    /**
     * Represents a single field-level validation error in a request.
     */
    public record FieldValidationError(
            String field,
            String message
    ) {
    }

    /**
     * Represents a validation error response containing all field violations.
     */
    public record ValidationErrorResponse(
            String error,
            List<FieldValidationError> errors
    ) {
    }

    /**
     * Represents a generic error response with a title and message.
     */
    public record SimpleErrorResponse(
            String error,
            String message
    ) {
    }
}
