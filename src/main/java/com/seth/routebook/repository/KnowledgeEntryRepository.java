package com.seth.routebook.repository;

import com.seth.routebook.domain.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
}
