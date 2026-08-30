package com.example.routeoptimizer.model;

import java.util.ArrayList;
import java.util.List;

public class Plan {
    private List<TechnicianRoute> technicianRoutes = new ArrayList<>();

    public Plan() {
    }

    public Plan(List<TechnicianRoute> technicianRoutes) {
        this.technicianRoutes = technicianRoutes != null ? technicianRoutes : new ArrayList<>();
    }

    public static PlanBuilder builder() {
        return new PlanBuilder();
    }

    public List<TechnicianRoute> getTechnicianRoutes() {
        return technicianRoutes;
    }

    public void setTechnicianRoutes(List<TechnicianRoute> technicianRoutes) {
        this.technicianRoutes = technicianRoutes;
    }

    public static class PlanBuilder {
        private List<TechnicianRoute> technicianRoutes = new ArrayList<>();

        public PlanBuilder technicianRoutes(List<TechnicianRoute> technicianRoutes) {
            this.technicianRoutes = technicianRoutes;
            return this;
        }

        public Plan build() {
            return new Plan(technicianRoutes);
        }
    }
}
