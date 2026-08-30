package com.example.routeoptimizer.exception;

import com.example.routeoptimizer.dto.plan.ValidationResult;

public class InvalidMoveException extends RuntimeException {
    private final ValidationResult validationResult;

    public InvalidMoveException(String message, ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
