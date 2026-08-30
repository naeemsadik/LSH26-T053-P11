package com.example.routeoptimizer.repository;

import com.example.routeoptimizer.entity.TravelMatrixEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelMatrixRepository extends JpaRepository<TravelMatrixEntity, String> {
}
