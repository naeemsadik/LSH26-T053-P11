package com.example.routeoptimizer.entity;

import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.JobStatus;
import com.example.routeoptimizer.model.Skill;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private Area area;

    @Enumerated(EnumType.STRING)
    private Skill requiredSkill;

    private int durationMinutes;

    private LocalTime windowStart;

    private LocalTime windowEnd;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    public JobEntity() {
    }

    public JobEntity(String id, Area area, Skill requiredSkill, int durationMinutes, LocalTime windowStart, LocalTime windowEnd, JobStatus status) {
        this.id = id;
        this.area = area;
        this.requiredSkill = requiredSkill;
        this.durationMinutes = durationMinutes;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = status;
    }

    public static JobEntityBuilder builder() {
        return new JobEntityBuilder();
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
        JobEntity jobEntity = (JobEntity) o;
        return Objects.equals(id, jobEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class JobEntityBuilder {
        private String id;
        private Area area;
        private Skill requiredSkill;
        private int durationMinutes;
        private LocalTime windowStart;
        private LocalTime windowEnd;
        private JobStatus status;

        public JobEntityBuilder id(String id) {
            this.id = id;
            return this;
        }

        public JobEntityBuilder area(Area area) {
            this.area = area;
            return this;
        }

        public JobEntityBuilder requiredSkill(Skill requiredSkill) {
            this.requiredSkill = requiredSkill;
            return this;
        }

        public JobEntityBuilder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public JobEntityBuilder windowStart(LocalTime windowStart) {
            this.windowStart = windowStart;
            return this;
        }

        public JobEntityBuilder windowEnd(LocalTime windowEnd) {
            this.windowEnd = windowEnd;
            return this;
        }

        public JobEntityBuilder status(JobStatus status) {
            this.status = status;
            return this;
        }

        public JobEntity build() {
            return new JobEntity(id, area, requiredSkill, durationMinutes, windowStart, windowEnd, status);
        }
    }
}
