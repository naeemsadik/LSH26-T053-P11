package com.example.routeoptimizer.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredErrorResponse {
    @JsonProperty("error_code")
    private String errorCode;

    private String message;

    private Map<String, Object> details;

    public StructuredErrorResponse() {
    }

    public StructuredErrorResponse(String errorCode, String message, Map<String, Object> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public static StructuredErrorResponseBuilder builder() {
        return new StructuredErrorResponseBuilder();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public static class StructuredErrorResponseBuilder {
        private String errorCode;
        private String message;
        private Map<String, Object> details;

        public StructuredErrorResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public StructuredErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public StructuredErrorResponseBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public StructuredErrorResponse build() {
            return new StructuredErrorResponse(errorCode, message, details);
        }
    }
}
