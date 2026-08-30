package com.example.routeoptimizer.dto.job;

import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.JobStatus;
import com.example.routeoptimizer.model.Skill;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class CreateJobRequest {
    @NotBlank(message = "id is required")
    private String id;

    @NotNull(message = "area is required")
    private Area area;

    @NotNull(message = "requiredSkill is required")
    @JsonProperty("requiredSkill")
    @JsonAlias("required_skill")
    private Skill requiredSkill;

    @Min(value = 1, message = "durationMinutes must be > 0")
    @JsonProperty("durationMinutes")
    @JsonAlias("duration_minutes")
    private int durationMinutes;

    @NotNull(message = "windowStart is required")
    @JsonProperty("windowStart")
    @JsonAlias("window_start")
    private LocalTime windowStart;

    @NotNull(message = "windowEnd is required")
    @JsonProperty("windowEnd")
    @JsonAlias("window_end")
    private LocalTime windowEnd;

    private JobStatus status;

    public CreateJobRequest() {
    }

    public CreateJobRequest(String id, Area area, Skill requiredSkill, int durationMinutes, LocalTime windowStart, LocalTime windowEnd, JobStatus status) {
        this.id = id;
        this.area = area;
        this.requiredSkill = requiredSkill;
        this.durationMinutes = durationMinutes;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = status;
    }

    public static CreateJobRequestBuilder builder() {
        return new CreateJobRequestBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Skill getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(Skill requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public static class CreateJobRequestBuilder {
        private String id;
        private Area area;
        private Skill requiredSkill;
        private int durationMinutes;
        private LocalTime windowStart;
        private LocalTime windowEnd;
        private JobStatus status;

        public CreateJobRequestBuilder id(String id) {
            this.id = id;
            return this;
        }

        public CreateJobRequestBuilder area(Area area) {
            this.area = area;
            return this;
        }

        public CreateJobRequestBuilder requiredSkill(Skill requiredSkill) {
            this.requiredSkill = requiredSkill;
            return this;
        }

        public CreateJobRequestBuilder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public CreateJobRequestBuilder windowStart(LocalTime windowStart) {
            this.windowStart = windowStart;
            return this;
        }

        public CreateJobRequestBuilder windowEnd(LocalTime windowEnd) {
            this.windowEnd = windowEnd;
            return this;
        }

        public CreateJobRequestBuilder status(JobStatus status) {
            this.status = status;
            return this;
        }

        public CreateJobRequest build() {
            return new CreateJobRequest(id, area, requiredSkill, durationMinutes, windowStart, windowEnd, status);
        }
    }
}
