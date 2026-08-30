package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.BaselinePlanResponse;
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

class OptimizationTest {

    private TravelMatrixService travelMatrixService;
    private ScheduleValidator scheduleValidator;
    private LocalSearchOptimizer localSearchOptimizer;
    private UnassignedReasonService unassignedReasonService;
    private ScoringService scoringService;
    private AssignmentEngine assignmentEngine;
    private BaselinePlanner baselinePlanner;

    @BeforeEach
    void setUp() {
        TravelMatrixRepository travelMatrixRepository = Mockito.mock(TravelMatrixRepository.class);
        travelMatrixService = new TravelMatrixService(travelMatrixRepository, 10);
        travelMatrixService.initializeDefaultDhakaMatrix();
        scheduleValidator = new ScheduleValidator(travelMatrixService);
        scoringService = new ScoringService(10);
        localSearchOptimizer = new LocalSearchOptimizer(scheduleValidator, 100);
        unassignedReasonService = new UnassignedReasonService(scheduleValidator, travelMatrixService);
        assignmentEngine = new AssignmentEngine(scheduleValidator, localSearchOptimizer, unassignedReasonService, scoringService);
        baselinePlanner = new BaselinePlanner(scheduleValidator, unassignedReasonService, scoringService);
    }

    @Test
    @DisplayName("Optimization Pass - Optimized plan total travel minutes must be strictly lower or equal to baseline")
    void testOptimizationImprovesTravelTime() {
        Technician t1 = Technician.builder()
                .id("T01")
                .name("Tech 1")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Technician t2 = Technician.builder()
                .id("T02")
                .name("Tech 2")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .homeArea(Area.MIRPUR)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job j1 = Job.builder().id("J01").area(Area.BANANI).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(8, 30)).windowEnd(LocalTime.of(12, 0)).status(JobStatus.PENDING).build();
        Job j2 = Job.builder().id("J02").area(Area.MOHAMMADPUR).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(8, 30)).windowEnd(LocalTime.of(12, 0)).status(JobStatus.PENDING).build();
        Job j3 = Job.builder().id("J03").area(Area.UTTARA).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(13, 0)).windowEnd(LocalTime.of(16, 0)).status(JobStatus.PENDING).build();
        Job j4 = Job.builder().id("J04").area(Area.MIRPUR).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(13, 0)).windowEnd(LocalTime.of(16, 0)).status(JobStatus.PENDING).build();

        List<Technician> techs = List.of(t1, t2);
        List<Job> jobs = List.of(j1, j2, j3, j4);

        BaselinePlanResponse baselineResponse = baselinePlanner.generateBaseline(techs, jobs, jobs);
        GeneratePlanResponse optimizedResponse = assignmentEngine.generatePlan(techs, jobs, jobs);

        assertTrue(optimizedResponse.getScore().getTotalTravelMinutes() <= baselineResponse.getScore().getTotalTravelMinutes());
    }
}
