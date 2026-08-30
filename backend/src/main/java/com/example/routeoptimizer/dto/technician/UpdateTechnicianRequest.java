package com.example.routeoptimizer.dto.technician;

import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.Skill;
import com.example.routeoptimizer.model.TechnicianStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.util.Set;

public class UpdateTechnicianRequest {
    private String name;
    private Set<Skill> skills;

    @JsonProperty("shiftStart")
    @JsonAlias("shift_start")
    private LocalTime shiftStart;

    @JsonProperty("shiftEnd")
    @JsonAlias("shift_end")
    private LocalTime shiftEnd;

    @JsonProperty("homeArea")
    @JsonAlias("home_area")
    private Area homeArea;

    private TechnicianStatus status;

    public UpdateTechnicianRequest() {
    }

    public UpdateTechnicianRequest(String name, Set<Skill> skills, LocalTime shiftStart, LocalTime shiftEnd, Area homeArea, TechnicianStatus status) {
        this.name = name;
        this.skills = skills;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.homeArea = homeArea;
        this.status = status;
    }

    public static UpdateTechnicianRequestBuilder builder() {
        return new UpdateTechnicianRequestBuilder();
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

    public static class UpdateTechnicianRequestBuilder {
        private String name;
        private Set<Skill> skills;
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
        private Area homeArea;
        private TechnicianStatus status;

        public UpdateTechnicianRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UpdateTechnicianRequestBuilder skills(Set<Skill> skills) {
            this.skills = skills;
            return this;
        }

        public UpdateTechnicianRequestBuilder shiftStart(LocalTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }

        public UpdateTechnicianRequestBuilder shiftEnd(LocalTime shiftEnd) {
            this.shiftEnd = shiftEnd;
            return this;
        }

        public UpdateTechnicianRequestBuilder homeArea(Area homeArea) {
            this.homeArea = homeArea;
            return this;
        }

        public UpdateTechnicianRequestBuilder status(TechnicianStatus status) {
            this.status = status;
            return this;
        }

        public UpdateTechnicianRequest build() {
            return new UpdateTechnicianRequest(name, skills, shiftStart, shiftEnd, homeArea, status);
        }
    }
}
