package com.example.routeoptimizer.controller;

import com.example.routeoptimizer.dto.job.CreateJobRequest;
import com.example.routeoptimizer.model.Job;
import com.example.routeoptimizer.model.JobStatus;
import com.example.routeoptimizer.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@Tag(name = "Jobs", description = "Job management endpoints")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "Get all jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @PostMapping
    @Operation(summary = "Add a new job")
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequest request) {
        Job job = Job.builder()
                .id(request.getId())
                .area(request.getArea())
                .requiredSkill(request.getRequiredSkill())
                .durationMinutes(request.getDurationMinutes())
                .windowStart(request.getWindowStart())
                .windowEnd(request.getWindowEnd())
                .status(request.getStatus() != null ? request.getStatus() : JobStatus.PENDING)
                .build();
        Job saved = jobService.saveJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
