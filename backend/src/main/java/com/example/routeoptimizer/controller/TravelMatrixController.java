package com.example.routeoptimizer.controller;

import com.example.routeoptimizer.dto.travel.UpdateTravelMatrixRequest;
import com.example.routeoptimizer.model.TravelMatrix;
import com.example.routeoptimizer.service.TravelMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/travel-matrix")
@Tag(name = "Travel Matrix", description = "Travel matrix management endpoints")
public class TravelMatrixController {

    private final TravelMatrixService travelMatrixService;

    public TravelMatrixController(TravelMatrixService travelMatrixService) {
        this.travelMatrixService = travelMatrixService;
    }

    @GetMapping
    @Operation(summary = "Get the current travel time matrix")
    public ResponseEntity<TravelMatrix> getTravelMatrix() {
        return ResponseEntity.ok(travelMatrixService.getTravelMatrix());
    }

    @PutMapping
    @Operation(summary = "Update travel time between two areas")
    public ResponseEntity<TravelMatrix> updateTravelTime(@Valid @RequestBody UpdateTravelMatrixRequest request) {
        travelMatrixService.updateTravelTime(request.getAreaA(), request.getAreaB(), request.getTravelTimeMinutes());
        return ResponseEntity.ok(travelMatrixService.getTravelMatrix());
    }
}
