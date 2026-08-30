package com.example.routeoptimizer.dto.technician;

import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.Skill;
import com.example.routeoptimizer.model.TechnicianStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;

public class CreateTechnicianRequest {
    @NotBlank(message = "id is required")
    private String id;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "skills cannot be null")
    private Set<Skill> skills;

    @NotNull(message = "shiftStart is required")
    @JsonProperty("shiftStart")
    @JsonAlias("shift_start")
    private LocalTime shiftStart;

    @NotNull(message = "shiftEnd is required")
    @JsonProperty("shiftEnd")
    @JsonAlias("shift_end")
    private LocalTime shiftEnd;

    @NotNull(message = "homeArea is required")
    @JsonProperty("homeArea")
    @JsonAlias("home_area")
    private Area homeArea;

    private TechnicianStatus status;

    public CreateTechnicianRequest() {
    }

    public CreateTechnicianRequest(String id, String name, Set<Skill> skills, LocalTime shiftStart, LocalTime shiftEnd, Area homeArea, TechnicianStatus status) {
        this.id = id;
        this.name = name;
        this.skills = skills;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.homeArea = homeArea;
        this.status = status;
    }

    public static CreateTechnicianRequestBuilder builder() {
        return new CreateTechnicianRequestBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = skills;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public Area getHomeArea() {
        return homeArea;
    }

    public void setHomeArea(Area homeArea) {
        this.homeArea = homeArea;
    }

    public TechnicianStatus getStatus() {
        return status;
    }

    public void setStatus(TechnicianStatus status) {
        this.status = status;
    }

    public static class CreateTechnicianRequestBuilder {
        private String id;
        private String name;
        private Set<Skill> skills;
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
        private Area homeArea;
        private TechnicianStatus status;

        public CreateTechnicianRequestBuilder id(String id) {
            this.id = id;
            return this;
        }

        public CreateTechnicianRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CreateTechnicianRequestBuilder skills(Set<Skill> skills) {
            this.skills = skills;
            return this;
        }

        public CreateTechnicianRequestBuilder shiftStart(LocalTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }

        public CreateTechnicianRequestBuilder shiftEnd(LocalTime shiftEnd) {
            this.shiftEnd = shiftEnd;
            return this;
        }

        public CreateTechnicianRequestBuilder homeArea(Area homeArea) {
            this.homeArea = homeArea;
            return this;
        }

        public CreateTechnicianRequestBuilder status(TechnicianStatus status) {
            this.status = status;
            return this;
        }

        public CreateTechnicianRequest build() {
            return new CreateTechnicianRequest(id, name, skills, shiftStart, shiftEnd, homeArea, status);
        }
    }
}
