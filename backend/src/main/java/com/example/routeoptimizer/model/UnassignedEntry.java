package com.example.routeoptimizer.model;

import java.util.Objects;

public class UnassignedEntry {
    private String jobId;
    private UnassignedReasonCode reasonCode;
    private String reasonText;

    public UnassignedEntry() {
    }

    public UnassignedEntry(String jobId, UnassignedReasonCode reasonCode, String reasonText) {
        this.jobId = jobId;
        this.reasonCode = reasonCode;
        this.reasonText = reasonText;
    }

    public static UnassignedEntryBuilder builder() {
        return new UnassignedEntryBuilder();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public UnassignedReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(UnassignedReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnassignedEntry that = (UnassignedEntry) o;
        return Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    public static class UnassignedEntryBuilder {
        private String jobId;
        private UnassignedReasonCode reasonCode;
        private String reasonText;

        public UnassignedEntryBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public UnassignedEntryBuilder reasonCode(UnassignedReasonCode reasonCode) {
            this.reasonCode = reasonCode;
            return this;
        }

        public UnassignedEntryBuilder reasonText(String reasonText) {
            this.reasonText = reasonText;
            return this;
        }

        public UnassignedEntry build() {
            return new UnassignedEntry(jobId, reasonCode, reasonText);
        }
    }
}
