package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.GeneratePlanResponse;
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
class SickTechnicianTest {

    @Autowired
    private TechnicianService technicianService;

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
    @DisplayName("Sick Technician Redistribution — Marking technician sick updates status and redistributes their jobs")
    void testMarkTechnicianSickRedistribution() {
        GeneratePlanResponse initialPlan = planService.getCurrentPlan();

        TechnicianRoute route = initialPlan.getPlan().getTechnicianRoutes().stream()
                .filter(r -> !r.getOrderedStops().isEmpty())
                .findFirst().orElseThrow();

        String sickTechId = route.getTechnicianId();

        GeneratePlanResponse updatedPlan = planService.markTechnicianSick(sickTechId);

        Technician sickTech = technicianService.getTechnicianById(sickTechId);
        assertEquals(TechnicianStatus.SICK, sickTech.getStatus());

        TechnicianRoute sickRoute = updatedPlan.getPlan().getTechnicianRoutes().stream()
                .filter(r -> r.getTechnicianId().equals(sickTechId))
                .findFirst().orElse(null);

        assertTrue(sickRoute == null || sickRoute.getOrderedStops().isEmpty());
        assertNotNull(updatedPlan.getScore());
    }
}
