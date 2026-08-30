package com.example.routeoptimizer.exception;

import com.example.routeoptimizer.dto.error.StructuredErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StructuredErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .details(Map.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<StructuredErrorResponse> handleInvalidMove(InvalidMoveException ex) {
        Map<String, Object> details = new HashMap<>();
        if (ex.getValidationResult() != null) {
            details.put("broken_rule", ex.getValidationResult().getBrokenRule());
            details.put("reason", ex.getValidationResult().getReason());
        }
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("INVALID_MOVE")
                .message(ex.getMessage())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<StructuredErrorResponse> handleValidation(ValidationException ex) {
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message(ex.getMessage())
                .details(Map.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StructuredErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed for input arguments")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StructuredErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("INVALID_REQUEST")
                .message("Malformed JSON or invalid request payload: " + ex.getMostSpecificCause().getMessage())
                .details(Map.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StructuredErrorResponse> handleGeneralException(Exception ex) {
        StructuredErrorResponse response = StructuredErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .details(Map.of())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
