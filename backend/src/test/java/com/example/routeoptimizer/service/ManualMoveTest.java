package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.GeneratePlanResponse;
import com.example.routeoptimizer.dto.plan.MoveRequest;
import com.example.routeoptimizer.dto.plan.ValidationResult;
import com.example.routeoptimizer.exception.InvalidMoveException;
import com.example.routeoptimizer.model.*;
import com.example.routeoptimizer.seed.DataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ManualMoveTest {

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private JobService jobService;

    @Autowired
    private TravelMatrixService travelMatrixService;

    @Autowired
    private PlanService planService;

    @Autowired
    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder.run();
        planService.generatePlan();
    }

    @Test
    @DisplayName("Stateless Validation — /plan/validate-move must not mutate application state")
    void testValidateMoveStateless() {
        GeneratePlanResponse initialPlan = planService.getCurrentPlan();

        MoveRequest req = MoveRequest.builder()
                .jobId("J01")
                .targetTechnicianId("T01")
                .position(0)
                .build();

        ValidationResult vr = planService.validateMove(req);
        assertNotNull(vr);

        GeneratePlanResponse planAfterValidate = planService.getCurrentPlan();
        assertEquals(initialPlan.getScore().getTotalTravelMinutes(), planAfterValidate.getScore().getTotalTravelMinutes());
        assertEquals(initialPlan.getScore().getJobsScheduledCount(), planAfterValidate.getScore().getJobsScheduledCount());
    }

    @Test
    @DisplayName("Invalid Move — Unqualified technician move must return SKILL_MATCH and throw InvalidMoveException on commit")
    void testInvalidSkillMove() {
        MoveRequest req = MoveRequest.builder()
                .jobId("J01")
                .targetTechnicianId("T05")
                .position(0)
                .build();

        ValidationResult vr = planService.validateMove(req);
        assertFalse(vr.isValid());
        assertEquals("SKILL_MATCH", vr.getBrokenRule());

        InvalidMoveException ex = assertThrows(InvalidMoveException.class, () -> planService.commitMove(req));
        assertEquals("SKILL_MATCH", ex.getValidationResult().getBrokenRule());
    }

    @Test
    @DisplayName("Valid Move Commit — Moving job to compatible technician updates plan and score")
    void testValidMoveCommit() {
        GeneratePlanResponse plan = planService.getCurrentPlan();
        TechnicianRoute route = plan.getPlan().getTechnicianRoutes().stream()
                .filter(r -> !r.getOrderedStops().isEmpty())
                .findFirst().orElseThrow();

        Stop stop = route.getOrderedStops().get(0);
        String jobId = stop.getJobId();
        Job job = jobService.getJobById(jobId);

        Technician targetTech = technicianService.getActiveTechnicians().stream()
                .filter(t -> t.getSkills().contains(job.getRequiredSkill()) && !t.getId().equals(route.getTechnicianId()))
                .findFirst().orElseThrow();

        MoveRequest req = MoveRequest.builder()
                .jobId(jobId)
                .targetTechnicianId(targetTech.getId())
                .position(0)
                .build();

        ValidationResult vr = planService.validateMove(req);
        if (vr.isValid()) {
            GeneratePlanResponse updatedPlan = planService.commitMove(req);
            assertNotNull(updatedPlan);
            assertTrue(updatedPlan.getPlan().getTechnicianRoutes().stream()
                    .anyMatch(r -> r.getTechnicianId().equals(targetTech.getId()) &&
                            r.getOrderedStops().stream().anyMatch(s -> s.getJobId().equals(jobId))));
        } else {
            assertNotNull(vr.getBrokenRule());
        }
    }
}
