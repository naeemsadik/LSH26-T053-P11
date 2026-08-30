package com.example.routeoptimizer.controller;

import com.example.routeoptimizer.dto.plan.GeneratePlanResponse;
import com.example.routeoptimizer.dto.technician.CreateTechnicianRequest;
import com.example.routeoptimizer.dto.technician.UpdateTechnicianRequest;
import com.example.routeoptimizer.model.Technician;
import com.example.routeoptimizer.service.PlanService;
import com.example.routeoptimizer.service.TechnicianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/technicians")
@Tag(name = "Technicians", description = "Technician management endpoints")
public class TechnicianController {

    private final TechnicianService technicianService;
    private final PlanService planService;

    public TechnicianController(TechnicianService technicianService, PlanService planService) {
        this.technicianService = technicianService;
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Get all technicians")
    public ResponseEntity<List<Technician>> getAllTechnicians() {
        return ResponseEntity.ok(technicianService.getAllTechnicians());
    }

    @PostMapping
    @Operation(summary = "Add a new technician")
    public ResponseEntity<Technician> createTechnician(@Valid @RequestBody CreateTechnicianRequest request) {
        Technician technician = Technician.builder()
                .id(request.getId())
                .name(request.getName())
                .skills(request.getSkills())
                .shiftStart(request.getShiftStart())
                .shiftEnd(request.getShiftEnd())
                .homeArea(request.getHomeArea())
                .status(request.getStatus())
                .build();
        Technician saved = technicianService.saveTechnician(technician);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update technician fields")
    public ResponseEntity<Technician> updateTechnician(
            @PathVariable String id,
            @RequestBody UpdateTechnicianRequest request) {
        Technician updated = technicianService.updateTechnician(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/sick")
    @Operation(summary = "Mark technician sick and redistribute affected jobs")
    public ResponseEntity<GeneratePlanResponse> markTechnicianSick(@PathVariable String id) {
        GeneratePlanResponse response = planService.markTechnicianSick(id);
        return ResponseEntity.ok(response);
    }
}
