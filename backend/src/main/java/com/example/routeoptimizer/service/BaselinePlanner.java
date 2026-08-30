package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.BaselinePlanResponse;
import com.example.routeoptimizer.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BaselinePlanner {

    private final ScheduleValidator scheduleValidator;
    private final UnassignedReasonService unassignedReasonService;
    private final ScoringService scoringService;

    public BaselinePlanner(
            ScheduleValidator scheduleValidator,
            UnassignedReasonService unassignedReasonService,
            ScoringService scoringService) {
        this.scheduleValidator = scheduleValidator;
        this.unassignedReasonService = unassignedReasonService;
        this.scoringService = scoringService;
    }

    public BaselinePlanResponse generateBaseline(List<Technician> activeTechs, List<Job> pendingJobs, List<Job> allJobs) {
        List<Technician> sortedActiveTechs = activeTechs.stream()
                .sorted(Comparator.comparing(Technician::getId))
                .toList();

        List<Job> sortedJobs = pendingJobs.stream()
                .sorted(Comparator.comparing(Job::getId))
                .toList();

        Map<String, List<Job>> technicianJobRoutes = new HashMap<>();
        for (Technician tech : sortedActiveTechs) {
            technicianJobRoutes.put(tech.getId(), new ArrayList<>());
        }

        List<UnassignedEntry> unassignedEntries = new ArrayList<>();

        for (Job job : sortedJobs) {
            boolean assigned = false;

            for (Technician tech : sortedActiveTechs) {
                List<Job> currentRoute = technicianJobRoutes.get(tech.getId());

                // First-fit position
                for (int pos = 0; pos <= currentRoute.size(); pos++) {
                    List<Job> proposedRoute = new ArrayList<>(currentRoute);
                    proposedRoute.add(pos, job);

                    ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRouteWithInsertedJobInfo(tech, proposedRoute, job.getId());
                    if (sim.validationResult().isValid()) {
                        currentRoute.add(pos, job);
                        assigned = true;
                        break;
                    }
                }
                if (assigned) break;
            }

            if (!assigned) {
                List<TechnicianRoute> tempRoutes = buildTechnicianRoutes(technicianJobRoutes, sortedActiveTechs);
                UnassignedEntry entry = unassignedReasonService.deriveReason(job, sortedActiveTechs, tempRoutes, allJobs);
                unassignedEntries.add(entry);
            }
        }

        Plan baselinePlan = new Plan();
        for (Technician tech : sortedActiveTechs) {
            List<Job> routeJobs = technicianJobRoutes.getOrDefault(tech.getId(), List.of());
            ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRoute(tech, routeJobs);

            baselinePlan.getTechnicianRoutes().add(TechnicianRoute.builder()
                    .technicianId(tech.getId())
                    .orderedStops(sim.stops())
                    .build());
        }

        Score score = scoringService.calculateScore(baselinePlan, allJobs, unassignedEntries.size());

        return BaselinePlanResponse.builder()
                .plan(baselinePlan)
                .unassigned(unassignedEntries)
                .score(score)
                .build();
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
}
