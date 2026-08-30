package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.GeneratePlanResponse;
import com.example.routeoptimizer.dto.plan.ValidationResult;
import com.example.routeoptimizer.model.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssignmentEngine {

    private final ScheduleValidator scheduleValidator;
    private final LocalSearchOptimizer localSearchOptimizer;
    private final UnassignedReasonService unassignedReasonService;
    private final ScoringService scoringService;

    public AssignmentEngine(
            ScheduleValidator scheduleValidator,
            LocalSearchOptimizer localSearchOptimizer,
            UnassignedReasonService unassignedReasonService,
            ScoringService scoringService) {
        this.scheduleValidator = scheduleValidator;
        this.localSearchOptimizer = localSearchOptimizer;
        this.unassignedReasonService = unassignedReasonService;
        this.scoringService = scoringService;
    }

    public GeneratePlanResponse generatePlan(List<Technician> activeTechs, List<Job> pendingJobs, List<Job> allJobs) {
        // Sort pending jobs: narrowest window first, earliest window start, job ID
        List<Job> sortedJobs = new ArrayList<>(pendingJobs);
        sortedJobs.sort(Comparator
                .comparingLong((Job j) -> Duration.between(j.getWindowStart(), j.getWindowEnd()).toMinutes())
                .thenComparing(Job::getWindowStart)
                .thenComparing(Job::getId));

        Map<String, List<Job>> technicianJobRoutes = new HashMap<>();
        for (Technician tech : activeTechs) {
            technicianJobRoutes.put(tech.getId(), new ArrayList<>());
        }

        List<UnassignedEntry> unassignedEntries = new ArrayList<>();
        List<Technician> sortedActiveTechs = activeTechs.stream()
                .sorted(Comparator.comparing(Technician::getId))
                .toList();

        // Greedy insertion
        for (Job job : sortedJobs) {
            InsertionCandidate bestCandidate = null;

            for (Technician tech : sortedActiveTechs) {
                List<Job> currentRoute = technicianJobRoutes.get(tech.getId());
                int oldTravel = scheduleValidator.simulateRoute(tech, currentRoute).totalTravelMinutes();

                for (int pos = 0; pos <= currentRoute.size(); pos++) {
                    List<Job> proposedRoute = new ArrayList<>(currentRoute);
                    proposedRoute.add(pos, job);

                    ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRouteWithInsertedJobInfo(tech, proposedRoute, job.getId());
                    if (sim.validationResult().isValid()) {
                        int newTravel = sim.totalTravelMinutes();
                        int marginalCost = newTravel - oldTravel;

                        // Calculate at risk count in proposed route
                        int atRiskInRoute = 0;
                        Map<String, Job> jobMap = allJobs.stream().collect(Collectors.toMap(Job::getId, Function.identity(), (a, b) -> a));
                        for (Stop stop : sim.stops()) {
                            Job j = jobMap.get(stop.getJobId());
                            if (j != null && scoringService.isJobAtRisk(stop.getComputedArrival(), j.getWindowEnd())) {
                                atRiskInRoute++;
                            }
                        }

                        InsertionCandidate candidate = new InsertionCandidate(tech.getId(), pos, marginalCost, atRiskInRoute);
                        if (bestCandidate == null || isBetter(candidate, bestCandidate)) {
                            bestCandidate = candidate;
                        }
                    }
                }
            }

            if (bestCandidate != null) {
                technicianJobRoutes.get(bestCandidate.techId()).add(bestCandidate.position(), job);
            } else {
                List<TechnicianRoute> tempRoutes = buildTechnicianRoutes(technicianJobRoutes, activeTechs);
                UnassignedEntry entry = unassignedReasonService.deriveReason(job, sortedActiveTechs, tempRoutes, allJobs);
                unassignedEntries.add(entry);
            }
        }

        // Run local search optimization
        Map<String, List<Job>> optimizedJobRoutes = localSearchOptimizer.optimize(sortedActiveTechs, technicianJobRoutes, allJobs);

        // Build final Plan with computed stops
        Plan finalPlan = new Plan();
        for (Technician tech : sortedActiveTechs) {
            List<Job> routeJobs = optimizedJobRoutes.getOrDefault(tech.getId(), List.of());
            ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRoute(tech, routeJobs);

            TechnicianRoute techRoute = TechnicianRoute.builder()
                    .technicianId(tech.getId())
                    .orderedStops(sim.stops())
                    .build();
            finalPlan.getTechnicianRoutes().add(techRoute);
        }

        Score score = scoringService.calculateScore(finalPlan, allJobs, unassignedEntries.size());

        return GeneratePlanResponse.builder()
                .plan(finalPlan)
                .unassigned(unassignedEntries)
                .score(score)
                .build();
    }

    private boolean isBetter(InsertionCandidate candidate, InsertionCandidate currentBest) {
        if (candidate.marginalCost() != currentBest.marginalCost()) {
            return candidate.marginalCost() < currentBest.marginalCost();
        }
        if (candidate.atRiskCount() != currentBest.atRiskCount()) {
            return candidate.atRiskCount() < currentBest.atRiskCount();
        }
        if (!candidate.techId().equals(currentBest.techId())) {
            return candidate.techId().compareTo(currentBest.techId()) < 0;
        }
        return candidate.position() < currentBest.position();
    }

    private List<TechnicianRoute> buildTechnicianRoutes(Map<String, List<Job>> routesMap, List<Technician> activeTechs) {
        List<TechnicianRoute> list = new ArrayList<>();
        for (Technician tech : activeTechs) {
            List<Job> routeJobs = routesMap.getOrDefault(tech.getId(), List.of());
            ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRoute(tech, routeJobs);
            list.add(TechnicianRoute.builder()
                    .technicianId(tech.getId())
                    .orderedStops(sim.stops())
                    .build());
        }
        return list;
    }

    private record InsertionCandidate(String techId, int position, int marginalCost, int atRiskCount) {}
}
