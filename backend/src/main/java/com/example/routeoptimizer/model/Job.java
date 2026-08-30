package com.example.routeoptimizer.model;

import java.time.LocalTime;
import java.util.Objects;

public class Job {
    private String id;
    private Area area;
    private Skill requiredSkill;
    private int durationMinutes;
    private LocalTime windowStart;
    private LocalTime windowEnd;
    private JobStatus status;

    public Job() {
    }

    public Job(String id, Area area, Skill requiredSkill, int durationMinutes, LocalTime windowStart, LocalTime windowEnd, JobStatus status) {
        this.id = id;
        this.area = area;
        this.requiredSkill = requiredSkill;
        this.durationMinutes = durationMinutes;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = status;
    }

    public static JobBuilder builder() {
        return new JobBuilder();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class JobBuilder {
        private String id;
        private Area area;
        private Skill requiredSkill;
        private int durationMinutes;
        private LocalTime windowStart;
        private LocalTime windowEnd;
        private JobStatus status;

        public JobBuilder id(String id) {
            this.id = id;
            return this;
        }

        public JobBuilder area(Area area) {
            this.area = area;
            return this;
        }

        public JobBuilder requiredSkill(Skill requiredSkill) {
            this.requiredSkill = requiredSkill;
            return this;
        }

        public JobBuilder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public JobBuilder windowStart(LocalTime windowStart) {
            this.windowStart = windowStart;
            return this;
        }

        public JobBuilder windowEnd(LocalTime windowEnd) {
            this.windowEnd = windowEnd;
            return this;
        }

        public JobBuilder status(JobStatus status) {
            this.status = status;
            return this;
        }

        public Job build() {
            return new Job(id, area, requiredSkill, durationMinutes, windowStart, windowEnd, status);
        }
    }
}
