package com.example.routeoptimizer.service;

import com.example.routeoptimizer.dto.plan.ValidationResult;
import com.example.routeoptimizer.model.*;
import com.example.routeoptimizer.repository.TravelMatrixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleValidatorTest {

    private TravelMatrixService travelMatrixService;
    private ScheduleValidator scheduleValidator;

    @BeforeEach
    void setUp() {
        TravelMatrixRepository travelMatrixRepository = Mockito.mock(TravelMatrixRepository.class);
        travelMatrixService = new TravelMatrixService(travelMatrixRepository, 10);
        travelMatrixService.initializeDefaultDhakaMatrix();
        scheduleValidator = new ScheduleValidator(travelMatrixService);
    }

    @Test
    @DisplayName("Skill Rule - Qualified technician should be valid, unqualified should be invalid")
    void testSkillRule() {
        Technician acTech = Technician.builder()
                .id("T01")
                .name("AC Tech")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(16, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job acJob = Job.builder()
                .id("J01")
                .area(Area.UTTARA)
                .requiredSkill(Skill.AC)
                .durationMinutes(60)
                .windowStart(LocalTime.of(8, 30))
                .windowEnd(LocalTime.of(11, 0))
                .status(JobStatus.PENDING)
                .build();

        Job plumbingJob = Job.builder()
                .id("J02")
                .area(Area.UTTARA)
                .requiredSkill(Skill.PLUMBING)
                .durationMinutes(60)
                .windowStart(LocalTime.of(8, 30))
                .windowEnd(LocalTime.of(11, 0))
                .status(JobStatus.PENDING)
                .build();

        ValidationResult res1 = scheduleValidator.validateInsertion(acTech, List.of(), acJob, 0);
        assertTrue(res1.isValid());

        ValidationResult res2 = scheduleValidator.validateInsertion(acTech, List.of(), plumbingJob, 0);
        assertFalse(res2.isValid());
        assertEquals("SKILL_MATCH", res2.getBrokenRule());
    }

    @Test
    @DisplayName("Shift Bounds - Job finishing after shift end should fail SHIFT_BOUNDS")
    void testShiftBoundsRule() {
        Technician tech = Technician.builder()
                .id("T01")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(10, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job longJob = Job.builder()
                .id("J01")
                .area(Area.UTTARA)
                .requiredSkill(Skill.AC)
                .durationMinutes(150)
                .windowStart(LocalTime.of(8, 0))
                .windowEnd(LocalTime.of(12, 0))
                .status(JobStatus.PENDING)
                .build();

        ValidationResult res = scheduleValidator.validateInsertion(tech, List.of(), longJob, 0);
        assertFalse(res.isValid());
        assertEquals("SHIFT_BOUNDS", res.getBrokenRule());
    }

    @Test
    @DisplayName("Time Window - Arrival after window end should fail TIME_WINDOW")
    void testTimeWindowRule() {
        Technician tech = Technician.builder()
                .id("T01")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job earlyWindowJob = Job.builder()
                .id("J01")
                .area(Area.MOTIJHEEL)
                .requiredSkill(Skill.AC)
                .durationMinutes(60)
                .windowStart(LocalTime.of(8, 0))
                .windowEnd(LocalTime.of(8, 30))
                .status(JobStatus.PENDING)
                .build();

        ValidationResult res = scheduleValidator.validateInsertion(tech, List.of(), earlyWindowJob, 0);
        assertFalse(res.isValid());
        assertEquals("TIME_WINDOW", res.getBrokenRule());
    }

    @Test
    @DisplayName("Cascade Rule - Inserting a job that causes a later job to miss its window should be rejected")
    void testCascadeValidation() {
        Technician tech = Technician.builder()
                .id("T01")
                .skills(Set.of(Skill.AC))
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .homeArea(Area.UTTARA)
                .status(TechnicianStatus.ACTIVE)
                .build();

        Job job1 = Job.builder().id("J01").area(Area.BANANI).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(8, 0)).windowEnd(LocalTime.of(10, 0)).status(JobStatus.PENDING).build();
        Job job2 = Job.builder().id("J02").area(Area.GULSHAN).requiredSkill(Skill.AC).durationMinutes(60).windowStart(LocalTime.of(9, 0)).windowEnd(LocalTime.of(10, 0)).status(JobStatus.PENDING).build();
        Job job99 = Job.builder().id("J99").area(Area.DHANMONDI).requiredSkill(Skill.AC).durationMinutes(120).windowStart(LocalTime.of(9, 0)).windowEnd(LocalTime.of(12, 0)).status(JobStatus.PENDING).build();

        ValidationResult cascadeResult = scheduleValidator.validateInsertion(tech, List.of(job1, job2), job99, 1);
        assertFalse(cascadeResult.isValid());
        assertEquals("TIME_WINDOW", cascadeResult.getBrokenRule());
    }
}
