package com.example.routeoptimizer.entity;

import com.example.routeoptimizer.model.Area;
import com.example.routeoptimizer.model.Skill;
import com.example.routeoptimizer.model.TechnicianStatus;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "technicians")
public class TechnicianEntity {

    @Id
    private String id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill")
    @Enumerated(EnumType.STRING)
    private Set<Skill> skills = new HashSet<>();

    private LocalTime shiftStart;

    private LocalTime shiftEnd;

    @Enumerated(EnumType.STRING)
    private Area homeArea;

    @Enumerated(EnumType.STRING)
    private TechnicianStatus status;

    public TechnicianEntity() {
    }

    public TechnicianEntity(String id, String name, Set<Skill> skills, LocalTime shiftStart, LocalTime shiftEnd, Area homeArea, TechnicianStatus status) {
        this.id = id;
        this.name = name;
        this.skills = skills != null ? skills : new HashSet<>();
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.homeArea = homeArea;
        this.status = status;
    }

    public static TechnicianEntityBuilder builder() {
        return new TechnicianEntityBuilder();
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
        TechnicianEntity that = (TechnicianEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class TechnicianEntityBuilder {
        private String id;
        private String name;
        private Set<Skill> skills = new HashSet<>();
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
        private Area homeArea;
        private TechnicianStatus status;

        public TechnicianEntityBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TechnicianEntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TechnicianEntityBuilder skills(Set<Skill> skills) {
            this.skills = skills;
            return this;
        }

        public TechnicianEntityBuilder shiftStart(LocalTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }

        public TechnicianEntityBuilder shiftEnd(LocalTime shiftEnd) {
            this.shiftEnd = shiftEnd;
            return this;
        }

        public TechnicianEntityBuilder homeArea(Area homeArea) {
            this.homeArea = homeArea;
            return this;
        }

        public TechnicianEntityBuilder status(TechnicianStatus status) {
            this.status = status;
            return this;
        }

        public TechnicianEntity build() {
            return new TechnicianEntity(id, name, skills, shiftStart, shiftEnd, homeArea, status);
        }
    }
}
