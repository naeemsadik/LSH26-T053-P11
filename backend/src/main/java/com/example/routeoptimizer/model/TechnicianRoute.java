package com.example.routeoptimizer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TechnicianRoute {
    private String technicianId;
    private List<Stop> orderedStops = new ArrayList<>();

    public TechnicianRoute() {
    }

    public TechnicianRoute(String technicianId, List<Stop> orderedStops) {
        this.technicianId = technicianId;
        this.orderedStops = orderedStops != null ? orderedStops : new ArrayList<>();
    }

    public static TechnicianRouteBuilder builder() {
        return new TechnicianRouteBuilder();
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        this.technicianId = technicianId;
    }

    public List<Stop> getOrderedStops() {
        return orderedStops;
    }

    public void setOrderedStops(List<Stop> orderedStops) {
        this.orderedStops = orderedStops;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicianRoute that = (TechnicianRoute) o;
        return Objects.equals(technicianId, that.technicianId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technicianId);
    }

    public static class TechnicianRouteBuilder {
        private String technicianId;
        private List<Stop> orderedStops = new ArrayList<>();

        public TechnicianRouteBuilder technicianId(String technicianId) {
            this.technicianId = technicianId;
            return this;
        }

        public TechnicianRouteBuilder orderedStops(List<Stop> orderedStops) {
            this.orderedStops = orderedStops;
            return this;
        }

        public TechnicianRoute build() {
            return new TechnicianRoute(technicianId, orderedStops);
        }
    }
}
