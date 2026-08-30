package com.example.routeoptimizer.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class MoveRequest {
    @NotBlank(message = "job_id is required")
    @JsonProperty("job_id")
    @JsonAlias("jobId")
    private String jobId;

    @NotBlank(message = "target_technician_id is required")
    @JsonProperty("target_technician_id")
    @JsonAlias("targetTechnicianId")
    private String targetTechnicianId;

    @Min(value = 0, message = "position must be >= 0")
    private int position;

    public MoveRequest() {
    }

    public MoveRequest(String jobId, String targetTechnicianId, int position) {
        this.jobId = jobId;
        this.targetTechnicianId = targetTechnicianId;
        this.position = position;
    }

    public static MoveRequestBuilder builder() {
        return new MoveRequestBuilder();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTargetTechnicianId() {
        return targetTechnicianId;
    }

    public void setTargetTechnicianId(String targetTechnicianId) {
        this.targetTechnicianId = targetTechnicianId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public static class MoveRequestBuilder {
        private String jobId;
        private String targetTechnicianId;
        private int position;

        public MoveRequestBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public MoveRequestBuilder targetTechnicianId(String targetTechnicianId) {
            this.targetTechnicianId = targetTechnicianId;
            return this;
        }

        public MoveRequestBuilder position(int position) {
            this.position = position;
            return this;
        }

        public MoveRequest build() {
            return new MoveRequest(jobId, targetTechnicianId, position);
        }
    }
}
