package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.GeneratePlanResponse;
import com.example.routeoptimizer.model.*;
import com.example.routeoptimizer.repository.TravelMatrixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentEngineTest {

    private TravelMatrixService travelMatrixService;
    private ScheduleValidator scheduleValidator;
    private LocalSearchOptimizer localSearchOptimizer;
    private UnassignedReasonService unassignedReasonService;
    private ScoringService scoringService;
    private AssignmentEngine assignmentEngine;

    @BeforeEach
    void setUp() {
        TravelMatrixRepository travelMatrixRepository = Mockito.mock(TravelMatrixRepository.class);
        travelMatrixService = new TravelMatrixService(travelMatrixRepository, 10);
        travelMatrixService.initializeDefaultDhakaMatrix();
        scheduleValidator = new ScheduleValidator(travelMatrixService);
        scoringService = new ScoringService(10);
        localSearchOptimizer = new LocalSearchOptimizer(scheduleValidator, 50);
        unassignedReasonService = new UnassignedReasonService(scheduleValidator, travelMatrixService);
        assignmentEngine = new AssignmentEngine(scheduleValidator, localSearchOptimizer, unassignedReasonService, scoringService);
    }

    @Test
    @DisplayName("Assignment Engine - Deterministic plan generation with minimal travel time")
    void testAssignmentEngineDeterminismAndFeasibility() {
        Technician t1 = Technician.builder()
                .id("T01")
                .name("Tech 1")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(16, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Technician t2 = Technician.builder()
                .id("T02")
                .name("Tech 2")
                .skills(Set.of(Skill.PLUMBING))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(16, 0))
                .homeArea(Area.DHANMONDI)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job j1 = Job.builder()
                .id("J01")
                .area(Area.BANANI)
                .requiredSkill(Skill.AC)
                .durationMinutes(60)
                .windowStart(LocalTime.of(8, 30))
                .windowEnd(LocalTime.of(11, 0))
                .status(JobStatus.PENDING)
                .build();

        Job j2 = Job.builder()
                .id("J02")
                .area(Area.MOHAMMADPUR)
                .requiredSkill(Skill.PLUMBING)
                .durationMinutes(60)
                .windowStart(LocalTime.of(9, 0))
                .windowEnd(LocalTime.of(12, 0))
                .status(JobStatus.PENDING)
                .build();

        List<Technician> techs = List.of(t1, t2);
        List<Job> jobs = List.of(j1, j2);

        GeneratePlanResponse res1 = assignmentEngine.generatePlan(techs, jobs, jobs);
        GeneratePlanResponse res2 = assignmentEngine.generatePlan(techs, jobs, jobs);

        assertEquals(res1.getScore().getTotalTravelMinutes(), res2.getScore().getTotalTravelMinutes());
        assertEquals(res1.getPlan().getTechnicianRoutes().size(), res2.getPlan().getTechnicianRoutes().size());
        assertEquals(2, res1.getScore().getJobsScheduledCount());
        assertEquals(0, res1.getScore().getJobsUnassignedCount());
    }

    @Test
    @DisplayName("Unassigned Job Reasoning - No skilled technician should yield NO_SKILLED_TECH")
    void testUnassignedReasonNoSkill() {
        Technician t1 = Technician.builder()
                .id("T01")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(16, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job plumbingJob = Job.builder()
                .id("J01")
                .area(Area.UTTARA)
                .requiredSkill(Skill.PLUMBING)
                .durationMinutes(60)
                .windowStart(LocalTime.of(9, 0))
                .windowEnd(LocalTime.of(12, 0))
                .status(JobStatus.PENDING)
                .build();

        GeneratePlanResponse res = assignmentEngine.generatePlan(List.of(t1), List.of(plumbingJob), List.of(plumbingJob));
        assertEquals(1, res.getUnassigned().size());
        assertEquals(UnassignedReasonCode.NO_SKILLED_TECH, res.getUnassigned().get(0).getReasonCode());
    }
}
