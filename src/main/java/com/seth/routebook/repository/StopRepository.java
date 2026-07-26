package com.seth.routebook.repository;

import com.seth.routebook.domain.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {
}
