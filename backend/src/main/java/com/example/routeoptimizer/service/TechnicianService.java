package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.technician.UpdateTechnicianRequest;
import com.example.routeoptimizer.entity.TechnicianEntity;
import com.example.routeoptimizer.exception.ResourceNotFoundException;
import com.example.routeoptimizer.exception.ValidationException;
import com.example.routeoptimizer.model.Technician;
import com.example.routeoptimizer.model.TechnicianStatus;
import com.example.routeoptimizer.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public TechnicianService(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll().stream()
                .map(this::toModel)
                .sorted(Comparator.comparing(Technician::getId))
                .toList();
    }

    public List<Technician> getActiveTechnicians() {
        return technicianRepository.findAll().stream()
                .filter(t -> t.getStatus() == TechnicianStatus.ACTIVE)
                .map(this::toModel)
                .sorted(Comparator.comparing(Technician::getId))
                .toList();
    }

    public Technician getTechnicianById(String id) {
        TechnicianEntity entity = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician with ID '" + id + "' not found"));
        return toModel(entity);
    }

    public Technician saveTechnician(Technician technician) {
        if (technician.getId() == null || technician.getId().isBlank()) {
            throw new ValidationException("Technician ID must not be empty");
        }
        if (technician.getShiftStart() != null && technician.getShiftEnd() != null
                && !technician.getShiftStart().isBefore(technician.getShiftEnd())) {
            throw new ValidationException("shiftStart must be before shiftEnd");
        }
        if (technician.getStatus() == null) {
            technician.setStatus(TechnicianStatus.ACTIVE);
        }

        TechnicianEntity entity = toEntity(technician);
        TechnicianEntity saved = technicianRepository.save(entity);
        return toModel(saved);
    }

    public Technician updateTechnician(String id, UpdateTechnicianRequest request) {
        TechnicianEntity entity = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician with ID '" + id + "' not found"));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getSkills() != null) {
            entity.setSkills(new HashSet<>(request.getSkills()));
        }
        if (request.getShiftStart() != null) {
            entity.setShiftStart(request.getShiftStart());
        }
        if (request.getShiftEnd() != null) {
            entity.setShiftEnd(request.getShiftEnd());
        }
        if (entity.getShiftStart() != null && entity.getShiftEnd() != null
                && !entity.getShiftStart().isBefore(entity.getShiftEnd())) {
            throw new ValidationException("shiftStart must be before shiftEnd");
        }
        if (request.getHomeArea() != null) {
            entity.setHomeArea(request.getHomeArea());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        TechnicianEntity saved = technicianRepository.save(entity);
        return toModel(saved);
    }

    public void clearAll() {
        technicianRepository.deleteAll();
    }

    private Technician toModel(TechnicianEntity entity) {
        return Technician.builder()
                .id(entity.getId())
                .name(entity.getName())
                .skills(entity.getSkills() != null ? new HashSet<>(entity.getSkills()) : new HashSet<>())
                .shiftStart(entity.getShiftStart())
                .shiftEnd(entity.getShiftEnd())
                .homeArea(entity.getHomeArea())
                .status(entity.getStatus())
                .build();
    }

    private TechnicianEntity toEntity(Technician model) {
        return TechnicianEntity.builder()
                .id(model.getId())
                .name(model.getName())
                .skills(model.getSkills() != null ? new HashSet<>(model.getSkills()) : new HashSet<>())
                .shiftStart(model.getShiftStart())
                .shiftEnd(model.getShiftEnd())
                .homeArea(model.getHomeArea())
                .status(model.getStatus())
                .build();
    }
}
