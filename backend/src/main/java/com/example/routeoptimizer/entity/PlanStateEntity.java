package com.example.routeoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_state")
public class PlanStateEntity {

    @Id
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String planJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String unassignedJson;

    public PlanStateEntity() {
    }

    public PlanStateEntity(String id, String planJson, String unassignedJson) {
        this.id = id;
        this.planJson = planJson;
        this.unassignedJson = unassignedJson;
    }

    public String getId() {
        return id;
    }

    public String getPlanJson() {
        return planJson;
    }

    public String getUnassignedJson() {
        return unassignedJson;
    }
}
