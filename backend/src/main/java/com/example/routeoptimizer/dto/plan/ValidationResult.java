package com.example.routeoptimizer.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationResult {
    private boolean valid;

    @JsonProperty("broken_rule")
    @JsonAlias("brokenRule")
    private String brokenRule;

    private String reason;

    public ValidationResult() {
    }

    public ValidationResult(boolean valid, String brokenRule, String reason) {
        this.valid = valid;
        this.brokenRule = brokenRule;
        this.reason = reason;
    }

    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static ValidationResult ok() {
        return ValidationResult.builder()
                .valid(true)
                .brokenRule(null)
                .reason(null)
                .build();
    }

    public static ValidationResult fail(String brokenRule, String reason) {
        return ValidationResult.builder()
                .valid(false)
                .brokenRule(brokenRule)
                .reason(reason)
                .build();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getBrokenRule() {
        return brokenRule;
    }

    public void setBrokenRule(String brokenRule) {
        this.brokenRule = brokenRule;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static class ValidationResultBuilder {
        private boolean valid;
        private String brokenRule;
        private String reason;

        public ValidationResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public ValidationResultBuilder brokenRule(String brokenRule) {
            this.brokenRule = brokenRule;
            return this;
        }

        public ValidationResultBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(valid, brokenRule, reason);
        }
    }
}
