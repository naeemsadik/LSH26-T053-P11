package com.example.routeoptimizer.dto.plan;

import com.example.routeoptimizer.model.Plan;
import com.example.routeoptimizer.model.Score;
import com.example.routeoptimizer.model.UnassignedEntry;

import java.util.ArrayList;
import java.util.List;

public class GeneratePlanResponse {
    private Plan plan;
    private List<UnassignedEntry> unassigned = new ArrayList<>();
    private Score score;

    public GeneratePlanResponse() {
    }

    public GeneratePlanResponse(Plan plan, List<UnassignedEntry> unassigned, Score score) {
        this.plan = plan;
        this.unassigned = unassigned != null ? unassigned : new ArrayList<>();
        this.score = score;
    }

    public static GeneratePlanResponseBuilder builder() {
        return new GeneratePlanResponseBuilder();
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public List<UnassignedEntry> getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(List<UnassignedEntry> unassigned) {
        this.unassigned = unassigned;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    public static class GeneratePlanResponseBuilder {
        private Plan plan;
        private List<UnassignedEntry> unassigned = new ArrayList<>();
        private Score score;

        public GeneratePlanResponseBuilder plan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public GeneratePlanResponseBuilder unassigned(List<UnassignedEntry> unassigned) {
            this.unassigned = unassigned;
            return this;
        }

        public GeneratePlanResponseBuilder score(Score score) {
            this.score = score;
            return this;
        }

        public GeneratePlanResponse build() {
            return new GeneratePlanResponse(plan, unassigned, score);
        }
    }
}
