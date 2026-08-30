package com.example.routeoptimizer.dto.travel;

import com.example.routeoptimizer.model.Area;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateTravelMatrixRequest {
    @NotNull(message = "areaA is required")
    @JsonProperty("areaA")
    @JsonAlias({"area_a", "fromArea"})
    private Area areaA;

    @NotNull(message = "areaB is required")
    @JsonProperty("areaB")
    @JsonAlias({"area_b", "toArea"})
    private Area areaB;

    @Min(value = 0, message = "travelTimeMinutes must be >= 0")
    @JsonProperty("travelTimeMinutes")
    @JsonAlias({"travel_time_minutes", "minutes"})
    private int travelTimeMinutes;

    public UpdateTravelMatrixRequest() {
    }

    public UpdateTravelMatrixRequest(Area areaA, Area areaB, int travelTimeMinutes) {
        this.areaA = areaA;
        this.areaB = areaB;
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public static UpdateTravelMatrixRequestBuilder builder() {
        return new UpdateTravelMatrixRequestBuilder();
    }

    public Area getAreaA() {
        return areaA;
    }

    public void setAreaA(Area areaA) {
        this.areaA = areaA;
    }

    public Area getAreaB() {
        return areaB;
    }

    public void setAreaB(Area areaB) {
        this.areaB = areaB;
    }

    public int getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    public void setTravelTimeMinutes(int travelTimeMinutes) {
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public static class UpdateTravelMatrixRequestBuilder {
        private Area areaA;
        private Area areaB;
        private int travelTimeMinutes;

        public UpdateTravelMatrixRequestBuilder areaA(Area areaA) {
            this.areaA = areaA;
            return this;
        }

        public UpdateTravelMatrixRequestBuilder areaB(Area areaB) {
            this.areaB = areaB;
            return this;
        }

        public UpdateTravelMatrixRequestBuilder travelTimeMinutes(int travelTimeMinutes) {
            this.travelTimeMinutes = travelTimeMinutes;
            return this;
        }

        public UpdateTravelMatrixRequest build() {
            return new UpdateTravelMatrixRequest(areaA, areaB, travelTimeMinutes);
        }
    }
}
