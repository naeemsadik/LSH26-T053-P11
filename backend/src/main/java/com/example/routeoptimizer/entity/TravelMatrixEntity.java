package com.example.routeoptimizer.entity;

import com.example.routeoptimizer.model.Area;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "travel_matrix")
public class TravelMatrixEntity {

    @Id
    private String areaKey;

    @Enumerated(EnumType.STRING)
    private Area areaA;

    @Enumerated(EnumType.STRING)
    private Area areaB;

    private int travelTimeMinutes;

    public TravelMatrixEntity() {
    }

    public TravelMatrixEntity(String areaKey, Area areaA, Area areaB, int travelTimeMinutes) {
        this.areaKey = areaKey;
        this.areaA = areaA;
        this.areaB = areaB;
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public static TravelMatrixEntityBuilder builder() {
        return new TravelMatrixEntityBuilder();
    }

    public String getAreaKey() {
        return areaKey;
    }

    public void setAreaKey(String areaKey) {
        this.areaKey = areaKey;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TravelMatrixEntity that = (TravelMatrixEntity) o;
        return Objects.equals(areaKey, that.areaKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaKey);
    }

    public static class TravelMatrixEntityBuilder {
        private String areaKey;
        private Area areaA;
        private Area areaB;
        private int travelTimeMinutes;

        public TravelMatrixEntityBuilder areaKey(String areaKey) {
            this.areaKey = areaKey;
            return this;
        }

        public TravelMatrixEntityBuilder areaA(Area areaA) {
            this.areaA = areaA;
            return this;
        }

        public TravelMatrixEntityBuilder areaB(Area areaB) {
            this.areaB = areaB;
            return this;
        }

        public TravelMatrixEntityBuilder travelTimeMinutes(int travelTimeMinutes) {
            this.travelTimeMinutes = travelTimeMinutes;
            return this;
        }

        public TravelMatrixEntity build() {
            return new TravelMatrixEntity(areaKey, areaA, areaB, travelTimeMinutes);
        }
    }
}
