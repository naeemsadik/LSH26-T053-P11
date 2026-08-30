package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.*;
import com.example.routeoptimizer.exception.InvalidMoveException;
import com.example.routeoptimizer.exception.ResourceNotFoundException;
import com.example.routeoptimizer.exception.ValidationException;
import com.example.routeoptimizer.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private final TechnicianService technicianService;
    private final JobService jobService;
    private final ScheduleValidator scheduleValidator;
    private final AssignmentEngine assignmentEngine;
    private final BaselinePlanner baselinePlanner;
    private final ScoringService scoringService;
    private final UnassignedReasonService unassignedReasonService;
    private final PlanStateStore planStateStore;

    private Plan currentPlan = new Plan();
    private List<UnassignedEntry> currentUnassigned = new ArrayList<>();
    private final Map<String, List<Job>> technicianJobsMap = new ConcurrentHashMap<>();

    public PlanService(
            TechnicianService technicianService,
            JobService jobService,
            ScheduleValidator scheduleValidator,
            AssignmentEngine assignmentEngine,
            BaselinePlanner baselinePlanner,
            ScoringService scoringService,
            UnassignedReasonService unassignedReasonService,
            PlanStateStore planStateStore) {
        this.technicianService = technicianService;
        this.jobService = jobService;
        this.scheduleValidator = scheduleValidator;
        this.assignmentEngine = assignmentEngine;
        this.baselinePlanner = baselinePlanner;
        this.scoringService = scoringService;
        this.unassignedReasonService = unassignedReasonService;
        this.planStateStore = planStateStore;
    }

    @PostConstruct
    void restoreSavedPlan() {
        planStateStore.load().ifPresent(saved -> {
            currentPlan = saved.plan();
            currentUnassigned = new ArrayList<>(saved.unassigned());
            technicianJobsMap.clear();

            for (Technician technician : technicianService.getAllTechnicians()) {
                technicianJobsMap.put(technician.getId(), new ArrayList<>());
            }
            for (TechnicianRoute route : currentPlan.getTechnicianRoutes()) {
                List<Job> jobs = technicianJobsMap.computeIfAbsent(route.getTechnicianId(), key -> new ArrayList<>());
                for (Stop stop : route.getOrderedStops()) {
                    try {
                        jobs.add(jobService.getJobById(stop.getJobId()));
                    } catch (ResourceNotFoundException ignored) {
                        // The plan can be regenerated after an input record is removed.
                    }
                }
            }
        });
    }

    public synchronized GeneratePlanResponse generatePlan() {
        List<Technician> activeTechs = technicianService.getActiveTechnicians();
        List<Job> allJobs = jobService.getAllJobs();

        List<Job> jobsToSchedule = allJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.PENDING || j.getStatus() == JobStatus.UNASSIGNED || j.getStatus() == JobStatus.ASSIGNED)
                .toList();

        GeneratePlanResponse response = assignmentEngine.generatePlan(activeTechs, jobsToSchedule, allJobs);

        technicianJobsMap.clear();
        for (Technician tech : technicianService.getAllTechnicians()) {
            technicianJobsMap.put(tech.getId(), new ArrayList<>());
        }

        Set<String> assignedJobIds = new HashSet<>();
        if (response.getPlan() != null && response.getPlan().getTechnicianRoutes() != null) {
            for (TechnicianRoute route : response.getPlan().getTechnicianRoutes()) {
                List<Job> routeJobList = technicianJobsMap.computeIfAbsent(route.getTechnicianId(), k -> new ArrayList<>());
                if (route.getOrderedStops() != null) {
                    for (Stop stop : route.getOrderedStops()) {
                        assignedJobIds.add(stop.getJobId());
                        jobService.updateJobStatus(stop.getJobId(), JobStatus.ASSIGNED);
                        routeJobList.add(jobService.getJobById(stop.getJobId()));
                    }
                }
            }
        }

        Set<String> unassignedJobIds = new HashSet<>();
        if (response.getUnassigned() != null) {
            for (UnassignedEntry entry : response.getUnassigned()) {
                unassignedJobIds.add(entry.getJobId());
                jobService.updateJobStatus(entry.getJobId(), JobStatus.UNASSIGNED);
            }
        }

        verifyDataIntegrity(allJobs, assignedJobIds, unassignedJobIds);

        this.currentPlan = response.getPlan();
        this.currentUnassigned = new ArrayList<>(response.getUnassigned());
        persistCurrentPlan();

        return response;
    }

    public BaselinePlanResponse generateBaseline() {
        List<Technician> activeTechs = technicianService.getActiveTechnicians();
        List<Job> allJobs = jobService.getAllJobs();
        List<Job> jobsToSchedule = allJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.PENDING || j.getStatus() == JobStatus.UNASSIGNED || j.getStatus() == JobStatus.ASSIGNED)
                .toList();

        return baselinePlanner.generateBaseline(activeTechs, jobsToSchedule, allJobs);
    }

    public synchronized GeneratePlanResponse getCurrentPlan() {
        List<Job> allJobs = jobService.getAllJobs();
        Score score = scoringService.calculateScore(currentPlan, allJobs, currentUnassigned.size());
        return GeneratePlanResponse.builder()
                .plan(currentPlan)
                .unassigned(currentUnassigned)
                .score(score)
                .build();
    }

    public ValidationResult validateMove(MoveRequest request) {
        Job job = jobService.getJobById(request.getJobId());
        Technician targetTech = technicianService.getTechnicianById(request.getTargetTechnicianId());

        if (targetTech.getStatus() == TechnicianStatus.SICK) {
            return ValidationResult.fail("TECHNICIAN_SICK", "Technician " + targetTech.getId() + " is SICK and cannot take jobs.");
        }

        List<Job> currentTargetJobs = new ArrayList<>(technicianJobsMap.getOrDefault(targetTech.getId(), List.of()));
        currentTargetJobs.removeIf(j -> j.getId().equals(job.getId()));

        if (request.getPosition() < 0 || request.getPosition() > currentTargetJobs.size()) {
            return ValidationResult.fail("INVALID_POSITION", "Position " + request.getPosition() + " is out of bounds for route length " + currentTargetJobs.size());
        }

        return scheduleValidator.validateInsertion(targetTech, currentTargetJobs, job, request.getPosition());
    }

    public synchronized GeneratePlanResponse commitMove(MoveRequest request) {
        ValidationResult vr = validateMove(request);
        if (!vr.isValid()) {
            throw new InvalidMoveException("Job " + request.getJobId() + " cannot be moved to technician " + request.getTargetTechnicianId() + ".", vr);
        }

        Job job = jobService.getJobById(request.getJobId());
        Technician targetTech = technicianService.getTechnicianById(request.getTargetTechnicianId());

        // Remove job from any existing technician route list
        for (List<Job> jobList : technicianJobsMap.values()) {
            jobList.removeIf(j -> j.getId().equals(job.getId()));
        }

        // Add job to target technician route list at position
        List<Job> targetJobList = technicianJobsMap.computeIfAbsent(targetTech.getId(), k -> new ArrayList<>());
        targetJobList.add(request.getPosition(), job);

        // Remove job from unassigned list if present
        currentUnassigned.removeIf(u -> u.getJobId().equals(job.getId()));
        jobService.updateJobStatus(job.getId(), JobStatus.ASSIGNED);

        recalculatePlanFromMap();
        persistCurrentPlan();

        List<Job> allJobs = jobService.getAllJobs();
        Score score = scoringService.calculateScore(currentPlan, allJobs, currentUnassigned.size());

        return GeneratePlanResponse.builder()
                .plan(currentPlan)
                .unassigned(currentUnassigned)
                .score(score)
                .build();
    }

    public synchronized GeneratePlanResponse markTechnicianSick(String technicianId) {
        Technician technician = technicianService.getTechnicianById(technicianId);
        technicianService.updateTechnician(technicianId, com.example.routeoptimizer.dto.technician.UpdateTechnicianRequest.builder()
                .status(TechnicianStatus.SICK)
                .build());

        List<Job> affectedJobs = new ArrayList<>(technicianJobsMap.getOrDefault(technicianId, List.of()));
        technicianJobsMap.put(technicianId, new ArrayList<>());

        List<Technician> activeTechs = technicianService.getActiveTechnicians();
        List<Job> allJobs = jobService.getAllJobs();

        // Redistribute affected jobs only
        for (Job job : affectedJobs) {
            InsertionCandidate bestCandidate = null;

            for (Technician tech : activeTechs) {
                List<Job> currentRoute = technicianJobsMap.getOrDefault(tech.getId(), List.of());
                int oldTravel = scheduleValidator.simulateRoute(tech, currentRoute).totalTravelMinutes();

                for (int pos = 0; pos <= currentRoute.size(); pos++) {
                    List<Job> proposedRoute = new ArrayList<>(currentRoute);
                    proposedRoute.add(pos, job);

                    ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRouteWithInsertedJobInfo(tech, proposedRoute, job.getId());
                    if (sim.validationResult().isValid()) {
                        int marginalCost = sim.totalTravelMinutes() - oldTravel;
                        InsertionCandidate candidate = new InsertionCandidate(tech.getId(), pos, marginalCost);
                        if (bestCandidate == null || candidate.marginalCost < bestCandidate.marginalCost) {
                            bestCandidate = candidate;
                        }
                    }
                }
            }

            if (bestCandidate != null) {
                List<Job> targetList = technicianJobsMap.computeIfAbsent(bestCandidate.techId(), k -> new ArrayList<>());
                targetList.add(bestCandidate.position(), job);
                jobService.updateJobStatus(job.getId(), JobStatus.ASSIGNED);
            } else {
                jobService.updateJobStatus(job.getId(), JobStatus.UNASSIGNED);
                List<TechnicianRoute> tempRoutes = buildTempRoutes();
                UnassignedEntry reason = unassignedReasonService.deriveReason(job, activeTechs, tempRoutes, allJobs);
                currentUnassigned.removeIf(u -> u.getJobId().equals(job.getId()));
                currentUnassigned.add(reason);
            }
        }

        recalculatePlanFromMap();
        persistCurrentPlan();
        Score score = scoringService.calculateScore(currentPlan, allJobs, currentUnassigned.size());

        return GeneratePlanResponse.builder()
                .plan(currentPlan)
                .unassigned(currentUnassigned)
                .score(score)
                .build();
    }

    public synchronized GeneratePlanResponse replanActive() {
        List<Job> pendingJobs = jobService.getPendingJobs();
        List<Technician> activeTechs = technicianService.getActiveTechnicians();
        List<Job> allJobs = jobService.getAllJobs();

        for (Job job : pendingJobs) {
            InsertionCandidate bestCandidate = null;

            for (Technician tech : activeTechs) {
                List<Job> currentRoute = technicianJobsMap.getOrDefault(tech.getId(), List.of());
                int oldTravel = scheduleValidator.simulateRoute(tech, currentRoute).totalTravelMinutes();

                for (int pos = 0; pos <= currentRoute.size(); pos++) {
                    List<Job> proposedRoute = new ArrayList<>(currentRoute);
                    proposedRoute.add(pos, job);

                    ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRouteWithInsertedJobInfo(tech, proposedRoute, job.getId());
                    if (sim.validationResult().isValid()) {
                        int marginalCost = sim.totalTravelMinutes() - oldTravel;
                        InsertionCandidate candidate = new InsertionCandidate(tech.getId(), pos, marginalCost);
                        if (bestCandidate == null || candidate.marginalCost < bestCandidate.marginalCost) {
                            bestCandidate = candidate;
                        }
                    }
                }
            }

            if (bestCandidate != null) {
                List<Job> targetList = technicianJobsMap.computeIfAbsent(bestCandidate.techId(), k -> new ArrayList<>());
                targetList.add(bestCandidate.position(), job);
                jobService.updateJobStatus(job.getId(), JobStatus.ASSIGNED);
            } else {
                jobService.updateJobStatus(job.getId(), JobStatus.UNASSIGNED);
                List<TechnicianRoute> tempRoutes = buildTempRoutes();
                UnassignedEntry reason = unassignedReasonService.deriveReason(job, activeTechs, tempRoutes, allJobs);
                currentUnassigned.removeIf(u -> u.getJobId().equals(job.getId()));
                currentUnassigned.add(reason);
            }
        }

        recalculatePlanFromMap();
        persistCurrentPlan();
        Score score = scoringService.calculateScore(currentPlan, allJobs, currentUnassigned.size());

        return GeneratePlanResponse.builder()
                .plan(currentPlan)
                .unassigned(currentUnassigned)
                .score(score)
                .build();
    }

    private void recalculatePlanFromMap() {
        List<TechnicianRoute> routes = new ArrayList<>();
        List<Technician> allTechs = technicianService.getAllTechnicians();

        for (Technician tech : allTechs) {
            List<Job> routeJobs = technicianJobsMap.getOrDefault(tech.getId(), List.of());
            ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRoute(tech, routeJobs);
            routes.add(TechnicianRoute.builder()
                    .technicianId(tech.getId())
                    .orderedStops(sim.stops())
                    .build());
        }

        this.currentPlan = Plan.builder()
                .technicianRoutes(routes)
                .build();
    }

    private List<TechnicianRoute> buildTempRoutes() {
        List<TechnicianRoute> routes = new ArrayList<>();
        for (Technician tech : technicianService.getAllTechnicians()) {
            List<Job> routeJobs = technicianJobsMap.getOrDefault(tech.getId(), List.of());
            ScheduleValidator.RouteSimulationResult sim = scheduleValidator.simulateRoute(tech, routeJobs);
            routes.add(TechnicianRoute.builder()
                    .technicianId(tech.getId())
                    .orderedStops(sim.stops())
                    .build());
        }
        return routes;
    }

    private void persistCurrentPlan() {
        planStateStore.save(currentPlan, currentUnassigned);
    }

    private void verifyDataIntegrity(List<Job> allJobs, Set<String> assignedIds, Set<String> unassignedIds) {
        for (Job j : allJobs) {
            boolean isAssigned = assignedIds.contains(j.getId());
            boolean isUnassigned = unassignedIds.contains(j.getId());
            boolean isOtherState = j.getStatus() == JobStatus.IN_PROGRESS || j.getStatus() == JobStatus.DONE;

            if (!isAssigned && !isUnassigned && !isOtherState && j.getStatus() == JobStatus.PENDING) {
                j.setStatus(JobStatus.UNASSIGNED);
            }
        }
    }

    private record InsertionCandidate(String techId, int position, int marginalCost) {}
}
