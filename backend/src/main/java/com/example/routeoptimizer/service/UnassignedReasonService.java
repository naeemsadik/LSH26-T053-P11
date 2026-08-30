package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.ValidationResult;
import com.example.routeoptimizer.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UnassignedReasonService {

    private final ScheduleValidator scheduleValidator;
    private final TravelMatrixService travelMatrixService;

    public UnassignedReasonService(ScheduleValidator scheduleValidator, TravelMatrixService travelMatrixService) {
        this.scheduleValidator = scheduleValidator;
        this.travelMatrixService = travelMatrixService;
    }

    public UnassignedEntry deriveReason(Job job, List<Technician> activeTechnicians, List<TechnicianRoute> currentRoutes, List<Job> allJobs) {
        // Step 1: Check if any active technician has required skill
        List<Technician> qualifiedTechs = activeTechnicians.stream()
                .filter(t -> t.getSkills().contains(job.getRequiredSkill()))
                .toList();

        if (qualifiedTechs.isEmpty()) {
            return UnassignedEntry.builder()
                    .jobId(job.getId())
                    .reasonCode(UnassignedReasonCode.NO_SKILLED_TECH)
                    .reasonText("No active technician has the required " + job.getRequiredSkill() + " skill.")
                    .build();
        }

        // Step 2: Test insertion across qualified technicians and evaluate failure reasons
        boolean sawShiftBoundsFailure = false;
        boolean sawTimeWindowFailure = false;
        LocalTime earliestArrival = null;
        String nearestTechId = null;

        for (Technician tech : qualifiedTechs) {
            List<Job> currentJobSequence = getJobsForTechnician(tech.getId(), currentRoutes, allJobs);

            // Estimate earliest arrival at job from previous stop or home
            LocalTime arrivalEstimate = estimateEarliestArrival(tech, currentJobSequence, job);
            if (earliestArrival == null || arrivalEstimate.isBefore(earliestArrival)) {
                earliestArrival = arrivalEstimate;
                nearestTechId = tech.getId();
            }

            for (int pos = 0; pos <= currentJobSequence.size(); pos++) {
                ValidationResult vr = scheduleValidator.validateInsertion(tech, currentJobSequence, job, pos);
                if (!vr.isValid()) {
                    if ("SHIFT_BOUNDS".equals(vr.getBrokenRule())) {
                        sawShiftBoundsFailure = true;
                    } else if ("TIME_WINDOW".equals(vr.getBrokenRule())) {
                        sawTimeWindowFailure = true;
                    }
                }
            }
        }

        if (sawTimeWindowFailure && (!sawShiftBoundsFailure || earliestArrival != null && earliestArrival.isAfter(job.getWindowEnd()))) {
            String text = "Nearest qualified technician " + (nearestTechId != null ? nearestTechId : "candidate")
                    + " can arrive at " + (earliestArrival != null ? earliestArrival : "late time")
                    + ", but the customer window closes at " + job.getWindowEnd() + ".";
            return UnassignedEntry.builder()
                    .jobId(job.getId())
                    .reasonCode(UnassignedReasonCode.WINDOW_MISSED)
                    .reasonText(text)
                    .build();
        }

        if (sawShiftBoundsFailure) {
            return UnassignedEntry.builder()
                    .jobId(job.getId())
                    .reasonCode(UnassignedReasonCode.SHIFT_CAPACITY)
                    .reasonText("All qualified technicians would finish job " + job.getId() + " after their shift ends.")
                    .build();
        }

        if (earliestArrival != null && earliestArrival.isAfter(job.getWindowEnd())) {
            return UnassignedEntry.builder()
                    .jobId(job.getId())
                    .reasonCode(UnassignedReasonCode.WINDOW_MISSED)
                    .reasonText("Nearest qualified technician " + nearestTechId + " can arrive at " + earliestArrival + ", but the customer window closes at " + job.getWindowEnd() + ".")
                    .build();
        }

        return UnassignedEntry.builder()
                .jobId(job.getId())
                .reasonCode(UnassignedReasonCode.OTHER)
                .reasonText("Job " + job.getId() + " cannot be placed in any technician route without rule violations.")
                .build();
    }

    private LocalTime estimateEarliestArrival(Technician tech, List<Job> routeJobs, Job targetJob) {
        Area currentArea = tech.getHomeArea();
        LocalTime currentTime = tech.getShiftStart();
        for (Job j : routeJobs) {
            int t = travelMatrixService.getTravelTime(currentArea, j.getArea());
            currentTime = currentTime.plusMinutes(t);
            if (currentTime.isBefore(j.getWindowStart())) {
                currentTime = j.getWindowStart();
            }
            currentTime = currentTime.plusMinutes(j.getDurationMinutes());
            currentArea = j.getArea();
        }
        int travelToTarget = travelMatrixService.getTravelTime(currentArea, targetJob.getArea());
        return currentTime.plusMinutes(travelToTarget);
    }

    private List<Job> getJobsForTechnician(String techId, List<TechnicianRoute> routes, List<Job> allJobs) {
        if (routes == null) return List.of();
        TechnicianRoute route = routes.stream()
                .filter(r -> r.getTechnicianId().equals(techId))
                .findFirst()
                .orElse(null);
        if (route == null || route.getOrderedStops() == null) return List.of();

        List<Job> result = new ArrayList<>();
        for (Stop stop : route.getOrderedStops()) {
            allJobs.stream()
                    .filter(j -> j.getId().equals(stop.getJobId()))
                    .findFirst()
                    .ifPresent(result::add);
        }
        return result;
    }
}
