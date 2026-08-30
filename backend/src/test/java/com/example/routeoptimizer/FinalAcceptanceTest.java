package com.example.routeoptimizer;

import com.example.routeoptimizer.dto.plan.MoveRequest;
import com.example.routeoptimizer.service.PlanStateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinalAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanStateStore planStateStore;

    @Test
    @DisplayName("PRD Final Acceptance Test Flow - Full end-to-end API verification")
    void testFinalAcceptanceFlow() throws Exception {
        mockMvc.perform(get("/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(12))));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(30))));

        mockMvc.perform(get("/travel-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelTimes", notNullValue()));

        mockMvc.perform(post("/plan/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.technicianRoutes", notNullValue()))
                .andExpect(jsonPath("$.score.totalTravelMinutes", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.score.jobsScheduledCount", greaterThan(0)));

        var savedPlan = planStateStore.load().orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(savedPlan.plan().getTechnicianRoutes().isEmpty());

        mockMvc.perform(post("/plan/baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.technicianRoutes", notNullValue()))
                .andExpect(jsonPath("$.score.totalTravelMinutes", greaterThanOrEqualTo(0)));

        MoveRequest invalidReq = MoveRequest.builder()
                .jobId("J01")
                .targetTechnicianId("T05")
                .position(0)
                .build();

        mockMvc.perform(post("/plan/validate-move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.broken_rule", is("SKILL_MATCH")));

        mockMvc.perform(post("/plan/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code", is("INVALID_MOVE")))
                .andExpect(jsonPath("$.details.broken_rule", is("SKILL_MATCH")));

        mockMvc.perform(post("/technicians/T01/sick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", notNullValue()))
                .andExpect(jsonPath("$.score", notNullValue()));

        mockMvc.perform(get("/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='T01')].status", contains("SICK")));
    }
}
