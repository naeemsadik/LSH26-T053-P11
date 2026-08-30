package com.example.routeoptimizer.service;

import com.example.routeoptimizer.entity.PlanStateEntity;
import com.example.routeoptimizer.model.Plan;
import com.example.routeoptimizer.model.UnassignedEntry;
import com.example.routeoptimizer.repository.PlanStateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlanStateStore {

    private static final String CURRENT_PLAN_ID = "current";

    private final PlanStateRepository repository;
    private final ObjectMapper objectMapper;

    public PlanStateStore(PlanStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(Plan plan, List<UnassignedEntry> unassigned) {
        try {
            repository.save(new PlanStateEntity(
                    CURRENT_PLAN_ID,
                    objectMapper.writeValueAsString(plan),
                    objectMapper.writeValueAsString(unassigned)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save the current plan", exception);
        }
    }

    public Optional<SavedPlanState> load() {
        return repository.findById(CURRENT_PLAN_ID).map(entity -> {
            try {
                Plan plan = objectMapper.readValue(entity.getPlanJson(), Plan.class);
                List<UnassignedEntry> unassigned = objectMapper.readValue(
                        entity.getUnassignedJson(),
                        new TypeReference<>() {});
                return new SavedPlanState(plan, unassigned);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not restore the current plan", exception);
            }
        });
    }

    public record SavedPlanState(Plan plan, List<UnassignedEntry> unassigned) {
    }
}
