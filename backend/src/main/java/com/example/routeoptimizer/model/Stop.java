package com.example.routeoptimizer.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.Objects;

public class Stop {
    private String jobId;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime computedArrival;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime computedDeparture;

    private int travelFromPrevious;

    public Stop() {
    }

    public Stop(String jobId, LocalTime computedArrival, LocalTime computedDeparture, int travelFromPrevious) {
        this.jobId = jobId;
        this.computedArrival = computedArrival;
        this.computedDeparture = computedDeparture;
        this.travelFromPrevious = travelFromPrevious;
    }

    public static StopBuilder builder() {
        return new StopBuilder();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public LocalTime getComputedArrival() {
        return computedArrival;
    }

    public void setComputedArrival(LocalTime computedArrival) {
        this.computedArrival = computedArrival;
    }

    public LocalTime getComputedDeparture() {
        return computedDeparture;
    }

    public void setComputedDeparture(LocalTime computedDeparture) {
        this.computedDeparture = computedDeparture;
    }

    public int getTravelFromPrevious() {
        return travelFromPrevious;
    }

    public void setTravelFromPrevious(int travelFromPrevious) {
        this.travelFromPrevious = travelFromPrevious;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return travelFromPrevious == stop.travelFromPrevious &&
                Objects.equals(jobId, stop.jobId) &&
                Objects.equals(computedArrival, stop.computedArrival) &&
                Objects.equals(computedDeparture, stop.computedDeparture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, computedArrival, computedDeparture, travelFromPrevious);
    }

    public static class StopBuilder {
        private String jobId;
        private LocalTime computedArrival;
        private LocalTime computedDeparture;
        private int travelFromPrevious;

        public StopBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public StopBuilder computedArrival(LocalTime computedArrival) {
            this.computedArrival = computedArrival;
            return this;
        }

        public StopBuilder computedDeparture(LocalTime computedDeparture) {
            this.computedDeparture = computedDeparture;
            return this;
        }

        public StopBuilder travelFromPrevious(int travelFromPrevious) {
            this.travelFromPrevious = travelFromPrevious;
            return this;
        }

        public Stop build() {
            return new Stop(jobId, computedArrival, computedDeparture, travelFromPrevious);
        }
    }
}
