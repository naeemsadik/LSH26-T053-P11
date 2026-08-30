package com.example.routeoptimizer.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Score {
    @JsonProperty("totalTravelMinutes")
    @JsonAlias("total_travel_minutes")
    private int totalTravelMinutes;

    @JsonProperty("jobsScheduledCount")
    @JsonAlias("jobs_scheduled_count")
    private int jobsScheduledCount;

    @JsonProperty("jobsUnassignedCount")
    @JsonAlias("jobs_unassigned_count")
    private int jobsUnassignedCount;

    @JsonProperty("jobsAtRiskCount")
    @JsonAlias("jobs_at_risk_count")
    private int jobsAtRiskCount;

    public Score() {
    }

    public Score(int totalTravelMinutes, int jobsScheduledCount, int jobsUnassignedCount, int jobsAtRiskCount) {
        this.totalTravelMinutes = totalTravelMinutes;
        this.jobsScheduledCount = jobsScheduledCount;
        this.jobsUnassignedCount = jobsUnassignedCount;
        this.jobsAtRiskCount = jobsAtRiskCount;
    }

    public static ScoreBuilder builder() {
        return new ScoreBuilder();
    }

    public int getTotalTravelMinutes() {
        return totalTravelMinutes;
    }

    public void setTotalTravelMinutes(int totalTravelMinutes) {
        this.totalTravelMinutes = totalTravelMinutes;
    }

    public int getJobsScheduledCount() {
        return jobsScheduledCount;
    }

    public void setJobsScheduledCount(int jobsScheduledCount) {
        this.jobsScheduledCount = jobsScheduledCount;
    }

    public int getJobsUnassignedCount() {
        return jobsUnassignedCount;
    }

    public void setJobsUnassignedCount(int jobsUnassignedCount) {
        this.jobsUnassignedCount = jobsUnassignedCount;
    }

    public int getJobsAtRiskCount() {
        return jobsAtRiskCount;
    }

    public void setJobsAtRiskCount(int jobsAtRiskCount) {
        this.jobsAtRiskCount = jobsAtRiskCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return totalTravelMinutes == score.totalTravelMinutes &&
                jobsScheduledCount == score.jobsScheduledCount &&
                jobsUnassignedCount == score.jobsUnassignedCount &&
                jobsAtRiskCount == score.jobsAtRiskCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalTravelMinutes, jobsScheduledCount, jobsUnassignedCount, jobsAtRiskCount);
    }

    public static class ScoreBuilder {
        private int totalTravelMinutes;
        private int jobsScheduledCount;
        private int jobsUnassignedCount;
        private int jobsAtRiskCount;

        public ScoreBuilder totalTravelMinutes(int totalTravelMinutes) {
            this.totalTravelMinutes = totalTravelMinutes;
            return this;
        }

        public ScoreBuilder jobsScheduledCount(int jobsScheduledCount) {
            this.jobsScheduledCount = jobsScheduledCount;
            return this;
        }

        public ScoreBuilder jobsUnassignedCount(int jobsUnassignedCount) {
            this.jobsUnassignedCount = jobsUnassignedCount;
            return this;
        }

        public ScoreBuilder jobsAtRiskCount(int jobsAtRiskCount) {
            this.jobsAtRiskCount = jobsAtRiskCount;
            return this;
        }

        public Score build() {
            return new Score(totalTravelMinutes, jobsScheduledCount, jobsUnassignedCount, jobsAtRiskCount);
        }
    }
}
