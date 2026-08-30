package com.example.routeoptimizer.repository;

import com.example.routeoptimizer.entity.TechnicianEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<TechnicianEntity, String> {
}
