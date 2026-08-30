package com.example.routeoptimizer.service;

import com.example.routeoptimizer.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocalSearchOptimizer {

    private final ScheduleValidator scheduleValidator;
    private final int maxIterations;

    public LocalSearchOptimizer(
            ScheduleValidator scheduleValidator,
            @Value("${optimizer.local-search-max-iterations:100}") int maxIterations) {
        this.scheduleValidator = scheduleValidator;
        this.maxIterations = maxIterations;
    }

    public Map<String, List<Job>> optimize(List<Technician> activeTechs, Map<String, List<Job>> initialRoutes, List<Job> allJobs) {
        Map<String, Job> jobMap = allJobs.stream()
                .collect(Collectors.toMap(Job::getId, Function.identity(), (a, b) -> a));

        Map<String, List<Job>> currentRoutes = new HashMap<>();
        for (Technician tech : activeTechs) {
            List<Job> list = initialRoutes.getOrDefault(tech.getId(), List.of());
            currentRoutes.put(tech.getId(), new ArrayList<>(list));
        }

        Map<String, Technician> techMap = activeTechs.stream()
                .collect(Collectors.toMap(Technician::getId, Function.identity()));

        int iteration = 0;
        boolean improved = true;

        while (improved && iteration < maxIterations) {
            improved = false;
            iteration++;

            // 1. Try Relocations: move job from tech A (pos i) to tech B (pos j)
            if (tryRelocations(activeTechs, techMap, currentRoutes)) {
                improved = true;
                continue;
            }

            // 2. Try Pairwise Swaps: swap job at tech A (pos i) with job at tech B (pos j)
            if (trySwaps(activeTechs, techMap, currentRoutes)) {
                improved = true;
            }
        }

        return currentRoutes;
    }

    private boolean tryRelocations(List<Technician> activeTechs, Map<String, Technician> techMap, Map<String, List<Job>> routes) {
        List<String> techIds = activeTechs.stream().map(Technician::getId).sorted().toList();

        for (String sourceTechId : techIds) {
            Technician sourceTech = techMap.get(sourceTechId);
            List<Job> sourceJobs = routes.get(sourceTechId);
            if (sourceJobs.isEmpty()) continue;

            int currentSourceTravel = scheduleValidator.simulateRoute(sourceTech, sourceJobs).totalTravelMinutes();

            for (int i = 0; i < sourceJobs.size(); i++) {
                Job candidateJob = sourceJobs.get(i);

                List<Job> newSourceJobs = new ArrayList<>(sourceJobs);
                newSourceJobs.remove(i);
                ScheduleValidator.RouteSimulationResult newSourceSim = scheduleValidator.simulateRoute(sourceTech, newSourceJobs);
                if (!newSourceSim.validationResult().isValid()) continue;

                int newSourceTravel = newSourceSim.totalTravelMinutes();

                for (String targetTechId : techIds) {
                    Technician targetTech = techMap.get(targetTechId);
                    List<Job> targetJobs = routes.get(targetTechId);
                    int currentTargetTravel = scheduleValidator.simulateRoute(targetTech, targetJobs).totalTravelMinutes();

                    int currentTotal = currentSourceTravel + currentTargetTravel;

                    int maxTargetPos = sourceTechId.equals(targetTechId) ? newSourceJobs.size() : targetJobs.size();
                    for (int targetPos = 0; targetPos <= maxTargetPos; targetPos++) {
                        if (sourceTechId.equals(targetTechId) && targetPos == i) continue; // Same position

                        List<Job> baseTargetList = sourceTechId.equals(targetTechId) ? newSourceJobs : targetJobs;
                        List<Job> newTargetJobs = new ArrayList<>(baseTargetList);
                        newTargetJobs.add(targetPos, candidateJob);

                        ScheduleValidator.RouteSimulationResult targetSim = scheduleValidator.simulateRoute(targetTech, newTargetJobs);
                        if (!targetSim.validationResult().isValid()) continue;

                        int newTotal;
                        if (sourceTechId.equals(targetTechId)) {
                            newTotal = targetSim.totalTravelMinutes();
                        } else {
                            newTotal = newSourceTravel + targetSim.totalTravelMinutes();
                        }

                        if (newTotal < currentTotal) {
                            // Improvement found! Update routes and accept
                            if (sourceTechId.equals(targetTechId)) {
                                routes.put(sourceTechId, newTargetJobs);
                            } else {
                                routes.put(sourceTechId, newSourceJobs);
                                routes.put(targetTechId, newTargetJobs);
                            }
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean trySwaps(List<Technician> activeTechs, Map<String, Technician> techMap, Map<String, List<Job>> routes) {
        List<String> techIds = activeTechs.stream().map(Technician::getId).sorted().toList();

        for (int t1Idx = 0; t1Idx < techIds.size(); t1Idx++) {
            String techId1 = techIds.get(t1Idx);
            Technician tech1 = techMap.get(techId1);
            List<Job> jobs1 = routes.get(techId1);
            if (jobs1.isEmpty()) continue;

            for (int t2Idx = t1Idx; t2Idx < techIds.size(); t2Idx++) {
                String techId2 = techIds.get(t2Idx);
                Technician tech2 = techMap.get(techId2);
                List<Job> jobs2 = routes.get(techId2);
                if (jobs2.isEmpty()) continue;

                int currentTravel1 = scheduleValidator.simulateRoute(tech1, jobs1).totalTravelMinutes();
                int currentTravel2 = scheduleValidator.simulateRoute(tech2, jobs2).totalTravelMinutes();
                int currentTotal = techId1.equals(techId2) ? currentTravel1 : (currentTravel1 + currentTravel2);

                for (int i = 0; i < jobs1.size(); i++) {
                    int startJ = techId1.equals(techId2) ? (i + 1) : 0;
                    for (int j = startJ; j < jobs2.size(); j++) {
                        List<Job> newJobs1 = new ArrayList<>(jobs1);
                        List<Job> newJobs2 = techId1.equals(techId2) ? newJobs1 : new ArrayList<>(jobs2);

                        Job j1 = jobs1.get(i);
                        Job j2 = jobs2.get(j);

                        if (techId1.equals(techId2)) {
                            newJobs1.set(i, j2);
                            newJobs1.set(j, j1);

                            ScheduleValidator.RouteSimulationResult sim1 = scheduleValidator.simulateRoute(tech1, newJobs1);
                            if (sim1.validationResult().isValid() && sim1.totalTravelMinutes() < currentTotal) {
                                routes.put(techId1, newJobs1);
                                return true;
                            }
                        } else {
                            newJobs1.set(i, j2);
                            newJobs2.set(j, j1);

                            ScheduleValidator.RouteSimulationResult sim1 = scheduleValidator.simulateRoute(tech1, newJobs1);
                            ScheduleValidator.RouteSimulationResult sim2 = scheduleValidator.simulateRoute(tech2, newJobs2);

                            if (sim1.validationResult().isValid() && sim2.validationResult().isValid()) {
                                int newTotal = sim1.totalTravelMinutes() + sim2.totalTravelMinutes();
                                if (newTotal < currentTotal) {
                                    routes.put(techId1, newJobs1);
                                    routes.put(techId2, newJobs2);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
