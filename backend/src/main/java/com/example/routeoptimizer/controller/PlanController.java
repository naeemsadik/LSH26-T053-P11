package com.example.routeoptimizer.controller;

import com.example.routeoptimizer.dto.plan.*;
import com.example.routeoptimizer.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plan")
@Tag(name = "Plan", description = "Schedule plan generation, baseline, manual overrides, and replanning")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate optimized route and shift assignment plan")
    public ResponseEntity<GeneratePlanResponse> generatePlan() {
        return ResponseEntity.ok(planService.generatePlan());
    }

    @PostMapping("/baseline")
    @Operation(summary = "Generate unoptimized first-fit baseline plan for score comparison")
    public ResponseEntity<BaselinePlanResponse> generateBaseline() {
        return ResponseEntity.ok(planService.generateBaseline());
    }

    @GetMapping({"", "/current"})
    @Operation(summary = "Get the current stored plan and score")
    public ResponseEntity<GeneratePlanResponse> getCurrentPlan() {
        return ResponseEntity.ok(planService.getCurrentPlan());
    }

    @PostMapping("/validate-move")
    @Operation(summary = "Statelessly validate a proposed manual job move")
    public ResponseEntity<ValidationResult> validateMove(@Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(planService.validateMove(request));
    }

    @PostMapping("/move")
    @Operation(summary = "Commit a manual job move after revalidation")
    public ResponseEntity<GeneratePlanResponse> commitMove(@Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(planService.commitMove(request));
    }

    @PostMapping("/replan-active")
    @Operation(summary = "Emergency insertion of pending jobs into current plan")
    public ResponseEntity<GeneratePlanResponse> replanActive() {
        return ResponseEntity.ok(planService.replanActive());
    }
}
