package com.example.routeoptimizer.seed;

import com.example.routeoptimizer.model.*;
import com.example.routeoptimizer.service.JobService;
import com.example.routeoptimizer.service.TechnicianService;
import com.example.routeoptimizer.service.TravelMatrixService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TechnicianService technicianService;
    private final JobService jobService;
    private final TravelMatrixService travelMatrixService;

    public DataSeeder(TechnicianService technicianService, JobService jobService, TravelMatrixService travelMatrixService) {
        this.technicianService = technicianService;
        this.jobService = jobService;
        this.travelMatrixService = travelMatrixService;
    }

    @Override
    public void run(String... args) {
        if (travelMatrixService.getTravelMatrix().getTravelTimes().isEmpty()) {
            seedTravelMatrix();
        }
        if (technicianService.getAllTechnicians().isEmpty()) {
            seedTechnicians();
        }
        if (jobService.getAllJobs().isEmpty()) {
            seedJobs();
        }
    }

    private void seedTravelMatrix() {
        travelMatrixService.initializeDefaultDhakaMatrix();
    }

    private void seedTechnicians() {
        // 12 Technicians with realistic schedules and skills across Dhaka zones
        createTech("T01", "Rahim Ahmed", Set.of(Skill.AC), "08:00", "16:00", Area.UTTARA);
        createTech("T02", "Karim Uddin", Set.of(Skill.AC), "08:00", "16:00", Area.MIRPUR);
        createTech("T03", "Tanvir Hossain", Set.of(Skill.AC), "08:30", "16:30", Area.GULSHAN);
        createTech("T04", "Farhan Kabir", Set.of(Skill.AC), "09:00", "17:00", Area.BANANI);

        createTech("T05", "Shafiqul Islam", Set.of(Skill.PLUMBING), "08:00", "16:00", Area.DHANMONDI);
        createTech("T06", "Mahmudul Hasan", Set.of(Skill.PLUMBING), "08:30", "16:30", Area.MOHAMMADPUR);
        createTech("T07", "Arifur Rahman", Set.of(Skill.PLUMBING), "09:00", "17:00", Area.MOTIJHEEL);
        createTech("T08", "Kamrul Islam", Set.of(Skill.PLUMBING), "09:30", "17:30", Area.OLD_DHAKA);

        createTech("T09", "Naimur Rashid", Set.of(Skill.AC, Skill.PLUMBING), "08:00", "16:00", Area.BASHUNDHARA);
        createTech("T10", "Zubair Ahmed", Set.of(Skill.AC, Skill.PLUMBING), "09:00", "17:00", Area.JATRABARI);

        createTech("T11", "Mehedi Hasan", Set.of(Skill.AC), "10:00", "18:00", Area.UTTARA);
        createTech("T12", "Sajid Khan", Set.of(Skill.PLUMBING), "10:00", "18:00", Area.MIRPUR);
    }

    private void createTech(String id, String name, Set<Skill> skills, String start, String end, Area home) {
        technicianService.saveTechnician(Technician.builder()
                .id(id)
                .name(name)
                .skills(skills)
                .shiftStart(LocalTime.parse(start))
                .shiftEnd(LocalTime.parse(end))
                .homeArea(home)
                .status(TechnicianStatus.ACTIVE)
                .build());
    }

    private void seedJobs() {
        // 30 Jobs with varying windows, skills, durations, and geographical distribution
        createJob("J01", Area.UTTARA, Skill.AC, 60, "08:30", "11:00");
        createJob("J02", Area.BANANI, Skill.AC, 45, "09:00", "11:30");
        createJob("J03", Area.GULSHAN, Skill.AC, 60, "10:00", "12:30");
        createJob("J04", Area.BASHUNDHARA, Skill.AC, 90, "11:00", "14:00");
        createJob("J05", Area.MIRPUR, Skill.AC, 60, "09:00", "12:00");
        createJob("J06", Area.UTTARA, Skill.AC, 45, "12:30", "15:00");
        createJob("J07", Area.BANANI, Skill.AC, 60, "13:30", "16:00");
        createJob("J08", Area.GULSHAN, Skill.AC, 60, "14:00", "16:30");

        createJob("J09", Area.DHANMONDI, Skill.PLUMBING, 60, "08:30", "11:00");
        createJob("J10", Area.MOHAMMADPUR, Skill.PLUMBING, 45, "09:00", "11:30");
        createJob("J11", Area.MOTIJHEEL, Skill.PLUMBING, 60, "10:00", "12:30");
        createJob("J12", Area.OLD_DHAKA, Skill.PLUMBING, 90, "11:00", "14:00");
        createJob("J13", Area.JATRABARI, Skill.PLUMBING, 60, "09:30", "12:30");
        createJob("J14", Area.DHANMONDI, Skill.PLUMBING, 45, "12:30", "15:00");
        createJob("J15", Area.MOHAMMADPUR, Skill.PLUMBING, 60, "13:00", "15:30");
        createJob("J16", Area.MOTIJHEEL, Skill.PLUMBING, 60, "14:00", "16:30");

        createJob("J17", Area.BASHUNDHARA, Skill.AC, 60, "08:30", "10:30");
        createJob("J18", Area.UTTARA, Skill.AC, 60, "09:30", "11:30");
        createJob("J19", Area.MIRPUR, Skill.PLUMBING, 60, "10:00", "12:00");
        createJob("J20", Area.DHANMONDI, Skill.PLUMBING, 60, "11:00", "13:00");
        createJob("J21", Area.OLD_DHAKA, Skill.PLUMBING, 60, "14:30", "16:30");
        createJob("J22", Area.JATRABARI, Skill.PLUMBING, 60, "15:00", "17:00");
        createJob("J23", Area.GULSHAN, Skill.AC, 60, "14:30", "16:30");
        createJob("J24", Area.BANANI, Skill.AC, 60, "15:00", "17:00");

        // Scenario demonstrator jobs: tight windows, shift edge, and unassigned constraints
        createJob("J25", Area.JATRABARI, Skill.AC, 120, "14:00", "16:00");
        createJob("J26", Area.UTTARA, Skill.PLUMBING, 90, "14:30", "16:30");
        createJob("J27", Area.OLD_DHAKA, Skill.AC, 90, "15:00", "16:30");
        createJob("J28", Area.MIRPUR, Skill.AC, 120, "14:30", "16:00");

        // Job with tight window creating a potential unassigned / tight window condition
        createJob("J29", Area.JATRABARI, Skill.AC, 120, "08:00", "08:45");
        // Job with late window matching late shift tech
        createJob("J30", Area.UTTARA, Skill.AC, 60, "16:30", "17:45");
    }

    private void createJob(String id, Area area, Skill skill, int duration, String windowStart, String windowEnd) {
        jobService.saveJob(Job.builder()
                .id(id)
                .area(area)
                .requiredSkill(skill)
                .durationMinutes(duration)
                .windowStart(LocalTime.parse(windowStart))
                .windowEnd(LocalTime.parse(windowEnd))
                .status(JobStatus.PENDING)
                .build());
    }
}
