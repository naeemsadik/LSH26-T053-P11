package com.example.routeoptimizer.service;

import com.example.routeoptimizer.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private final int riskBufferMinutes;

    public ScoringService(
            @Value("${optimizer.risk-buffer-minutes:10}") int riskBufferMinutes) {
        this.riskBufferMinutes = riskBufferMinutes;
    }

    public int getRiskBufferMinutes() {
        return riskBufferMinutes;
    }

    public Score calculateScore(Plan plan, List<Job> allJobs, int totalUnassignedCount) {
        int totalTravelMinutes = 0;
        int jobsScheduledCount = 0;
        int jobsAtRiskCount = 0;

        Map<String, Job> jobMap = allJobs.stream()
                .collect(Collectors.toMap(Job::getId, Function.identity(), (a, b) -> a));

        if (plan != null && plan.getTechnicianRoutes() != null) {
            for (TechnicianRoute route : plan.getTechnicianRoutes()) {
                if (route.getOrderedStops() == null) continue;

                for (Stop stop : route.getOrderedStops()) {
                    totalTravelMinutes += stop.getTravelFromPrevious();
                    jobsScheduledCount++;

                    Job job = jobMap.get(stop.getJobId());
                    if (job != null && isJobAtRisk(stop.getComputedArrival(), job.getWindowEnd())) {
                        jobsAtRiskCount++;
                    }
                }
            }
        }

        return Score.builder()
                .totalTravelMinutes(totalTravelMinutes)
                .jobsScheduledCount(jobsScheduledCount)
                .jobsUnassignedCount(totalUnassignedCount)
                .jobsAtRiskCount(jobsAtRiskCount)
                .build();
    }

    public boolean isJobAtRisk(LocalTime arrival, LocalTime windowEnd) {
        if (arrival == null || windowEnd == null) return false;
        long minutesUntilWindowEnd = Duration.between(arrival, windowEnd).toMinutes();
        return minutesUntilWindowEnd >= 0 && minutesUntilWindowEnd <= riskBufferMinutes;
    }
}
