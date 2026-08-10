package com.seth.routebook.repository;

import com.seth.routebook.domain.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, Long> {
    List<Stop> findByRouteId(Long routeId);
}
