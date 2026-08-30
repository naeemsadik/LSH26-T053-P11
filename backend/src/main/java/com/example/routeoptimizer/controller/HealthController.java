package com.example.routeoptimizer.controller;

import com.example.routeoptimizer.service.TechnicianService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final TechnicianService technicianService;

    public HealthController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "technicians", technicianService.getAllTechnicians().size());
    }
}
