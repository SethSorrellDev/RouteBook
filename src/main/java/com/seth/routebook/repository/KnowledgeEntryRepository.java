package com.seth.routebook.repository;

import com.seth.routebook.domain.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
    List<KnowledgeEntry> findByRouteId(Long routeId);
    List<KnowledgeEntry> findByStopId(Long stopId);
}
