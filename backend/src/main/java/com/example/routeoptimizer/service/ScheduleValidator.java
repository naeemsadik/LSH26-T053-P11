package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.ValidationResult;
import com.example.routeoptimizer.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleValidator {

    private final TravelMatrixService travelMatrixService;

    public ScheduleValidator(TravelMatrixService travelMatrixService) {
        this.travelMatrixService = travelMatrixService;
    }

    public record RouteSimulationResult(
            ValidationResult validationResult,
            List<Stop> stops,
            int totalTravelMinutes
    ) {}

    public RouteSimulationResult simulateRoute(Technician technician, List<Job> routeJobs) {
        return simulateRouteWithInsertedJobInfo(technician, routeJobs, null);
    }

    public RouteSimulationResult simulateRouteWithInsertedJobInfo(Technician technician, List<Job> routeJobs, String insertedJobId) {
        if (technician.getStatus() == TechnicianStatus.SICK) {
            return new RouteSimulationResult(
                    ValidationResult.fail("TECHNICIAN_SICK", "Technician " + technician.getId() + " is marked SICK and cannot be assigned jobs."),
                    List.of(), 0
            );
        }

        List<Stop> stops = new ArrayList<>();
        int totalTravel = 0;
        LocalTime currentDeparture = technician.getShiftStart();
        Area currentArea = technician.getHomeArea();

        for (int i = 0; i < routeJobs.size(); i++) {
            Job job = routeJobs.get(i);

            // Rule 1: SKILL_MATCH
            if (!technician.getSkills().contains(job.getRequiredSkill())) {
                String reason = "Technician " + technician.getId() + " does not have the required " + job.getRequiredSkill() + " skill.";
                return new RouteSimulationResult(ValidationResult.fail("SKILL_MATCH", reason), List.of(), 0);
            }

            int travelTime = travelMatrixService.getTravelTime(currentArea, job.getArea());
            totalTravel += travelTime;

            LocalTime rawArrival = currentDeparture.plusMinutes(travelTime);
            // Arrival cannot be earlier than windowStart (technician waits if arriving early)
            LocalTime computedArrival = rawArrival.isBefore(job.getWindowStart()) ? job.getWindowStart() : rawArrival;

            // Rule 3: TIME_WINDOW
            if (rawArrival.isAfter(job.getWindowEnd()) || computedArrival.isAfter(job.getWindowEnd())) {
                boolean isCascaded = insertedJobId != null && !job.getId().equals(insertedJobId);
                String reason;
                if (isCascaded) {
                    reason = "Inserting job " + insertedJobId + " into route for technician " + technician.getId()
                            + " causes subsequent job " + job.getId() + " to arrive at " + rawArrival
                            + ", after its window closes at " + job.getWindowEnd() + ".";
                } else {
                    reason = "Nearest qualified technician " + technician.getId() + " arrives at " + rawArrival
                            + ", but the customer window closes at " + job.getWindowEnd() + ".";
                }
                return new RouteSimulationResult(ValidationResult.fail("TIME_WINDOW", reason), List.of(), 0);
            }

            // Rule 2: SHIFT_BOUNDS
            if (computedArrival.isBefore(technician.getShiftStart())) {
                String reason = "Technician " + technician.getId() + " arrives at " + computedArrival
                        + ", which is before shift start " + technician.getShiftStart() + ".";
                return new RouteSimulationResult(ValidationResult.fail("SHIFT_BOUNDS", reason), List.of(), 0);
            }

            LocalTime computedDeparture = computedArrival.plusMinutes(job.getDurationMinutes());
            if (computedDeparture.isAfter(technician.getShiftEnd())) {
                boolean isCascaded = insertedJobId != null && !job.getId().equals(insertedJobId);
                String reason;
                if (isCascaded) {
                    reason = "Inserting job " + insertedJobId + " into route for technician " + technician.getId()
                            + " causes subsequent job " + job.getId() + " to finish at " + computedDeparture
                            + ", after shift end " + technician.getShiftEnd() + ".";
                } else {
                    reason = "Technician " + technician.getId() + " would finish job " + job.getId()
                            + " at " + computedDeparture + ", after their shift ends at " + technician.getShiftEnd() + ".";
                }
                return new RouteSimulationResult(ValidationResult.fail("SHIFT_BOUNDS", reason), List.of(), 0);
            }

            stops.add(Stop.builder()
                    .jobId(job.getId())
                    .computedArrival(computedArrival)
                    .computedDeparture(computedDeparture)
                    .travelFromPrevious(travelTime)
                    .build());

            currentDeparture = computedDeparture;
            currentArea = job.getArea();
        }

        return new RouteSimulationResult(ValidationResult.ok(), stops, totalTravel);
    }

    public ValidationResult validateInsertion(Technician technician, List<Job> currentRouteJobs, Job jobToInsert, int insertionPosition) {
        if (insertionPosition < 0 || insertionPosition > currentRouteJobs.size()) {
            return ValidationResult.fail("INVALID_POSITION", "Insertion position " + insertionPosition + " out of bounds");
        }

        List<Job> proposedJobs = new ArrayList<>(currentRouteJobs);
        proposedJobs.add(insertionPosition, jobToInsert);

        RouteSimulationResult result = simulateRouteWithInsertedJobInfo(technician, proposedJobs, jobToInsert.getId());
        return result.validationResult();
    }
}
