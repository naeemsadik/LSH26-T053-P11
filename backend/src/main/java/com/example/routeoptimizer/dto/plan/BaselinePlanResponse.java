package com.example.routeoptimizer.dto.plan;

import com.example.routeoptimizer.model.Plan;
import com.example.routeoptimizer.model.Score;
import com.example.routeoptimizer.model.UnassignedEntry;

import java.util.ArrayList;
import java.util.List;

public class BaselinePlanResponse {
    private Plan plan;
    private List<UnassignedEntry> unassigned = new ArrayList<>();
    private Score score;

    public BaselinePlanResponse() {
    }

    public BaselinePlanResponse(Plan plan, List<UnassignedEntry> unassigned, Score score) {
        this.plan = plan;
        this.unassigned = unassigned != null ? unassigned : new ArrayList<>();
        this.score = score;
    }

    public static BaselinePlanResponseBuilder builder() {
        return new BaselinePlanResponseBuilder();
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

    public static class BaselinePlanResponseBuilder {
        private Plan plan;
        private List<UnassignedEntry> unassigned = new ArrayList<>();
        private Score score;

        public BaselinePlanResponseBuilder plan(Plan plan) {
            this.plan = plan;
            return this;
        }

        public BaselinePlanResponseBuilder unassigned(List<UnassignedEntry> unassigned) {
            this.unassigned = unassigned;
            return this;
        }

        public BaselinePlanResponseBuilder score(Score score) {
            this.score = score;
            return this;
        }

        public BaselinePlanResponse build() {
            return new BaselinePlanResponse(plan, unassigned, score);
        }
    }
}
