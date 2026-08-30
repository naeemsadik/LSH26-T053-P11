package com.example.routeoptimizer.service;

import com.example.routeoptimizer.entity.JobEntity;
import com.example.routeoptimizer.exception.ResourceNotFoundException;
import com.example.routeoptimizer.exception.ValidationException;
import com.example.routeoptimizer.model.Job;
import com.example.routeoptimizer.model.JobStatus;
import com.example.routeoptimizer.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::toModel)
                .sorted(Comparator.comparing(Job::getId))
                .toList();
    }

    public List<Job> getPendingJobs() {
        return jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.PENDING)
                .map(this::toModel)
                .sorted(Comparator.comparing(Job::getId))
                .toList();
    }

    public Job getJobById(String id) {
        JobEntity entity = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID '" + id + "' not found"));
        return toModel(entity);
    }

    public Job saveJob(Job job) {
        if (job.getId() == null || job.getId().isBlank()) {
            throw new ValidationException("Job ID must not be empty");
        }
        if (job.getDurationMinutes() <= 0) {
            throw new ValidationException("Job duration must be greater than 0");
        }
        if (job.getWindowStart() == null || job.getWindowEnd() == null || !job.getWindowStart().isBefore(job.getWindowEnd())) {
            throw new ValidationException("windowStart must be before windowEnd");
        }
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.PENDING);
        }

        JobEntity entity = toEntity(job);
        JobEntity saved = jobRepository.save(entity);
        return toModel(saved);
    }

    public Job replaceJob(String id, Job job) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job with ID '" + id + "' not found");
        }
        job.setId(id);
        return saveJob(job);
    }

    public void updateJobStatus(String id, JobStatus status) {
        JobEntity entity = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID '" + id + "' not found"));
        entity.setStatus(status);
        jobRepository.save(entity);
    }

    public void clearAll() {
        jobRepository.deleteAll();
    }

    private Job toModel(JobEntity entity) {
        return Job.builder()
                .id(entity.getId())
                .area(entity.getArea())
                .requiredSkill(entity.getRequiredSkill())
                .durationMinutes(entity.getDurationMinutes())
                .windowStart(entity.getWindowStart())
                .windowEnd(entity.getWindowEnd())
                .status(entity.getStatus())
                .build();
    }

    private JobEntity toEntity(Job model) {
        return JobEntity.builder()
                .id(model.getId())
                .area(model.getArea())
                .requiredSkill(model.getRequiredSkill())
                .durationMinutes(model.getDurationMinutes())
                .windowStart(model.getWindowStart())
                .windowEnd(model.getWindowEnd())
                .status(model.getStatus())
                .build();
    }
}
