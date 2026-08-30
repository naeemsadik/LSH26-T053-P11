package com.example.routeoptimizer.model;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Technician {
    private String id;
    private String name;
    private Set<Skill> skills = new HashSet<>();
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private Area homeArea;
    private TechnicianStatus status;

    public Technician() {
    }

    public Technician(String id, String name, Set<Skill> skills, LocalTime shiftStart, LocalTime shiftEnd, Area homeArea, TechnicianStatus status) {
        this.id = id;
        this.name = name;
        this.skills = skills != null ? skills : new HashSet<>();
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.homeArea = homeArea;
        this.status = status;
    }

    public static TechnicianBuilder builder() {
        return new TechnicianBuilder();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Technician that = (Technician) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class TechnicianBuilder {
        private String id;
        private String name;
        private Set<Skill> skills = new HashSet<>();
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
        private Area homeArea;
        private TechnicianStatus status;

        public TechnicianBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TechnicianBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TechnicianBuilder skills(Set<Skill> skills) {
            this.skills = skills;
            return this;
        }

        public TechnicianBuilder shiftStart(LocalTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }

        public TechnicianBuilder shiftEnd(LocalTime shiftEnd) {
            this.shiftEnd = shiftEnd;
            return this;
        }

        public TechnicianBuilder homeArea(Area homeArea) {
            this.homeArea = homeArea;
            return this;
        }

        public TechnicianBuilder status(TechnicianStatus status) {
            this.status = status;
            return this;
        }

        public Technician build() {
            return new Technician(id, name, skills, shiftStart, shiftEnd, homeArea, status);
        }
    }
}
