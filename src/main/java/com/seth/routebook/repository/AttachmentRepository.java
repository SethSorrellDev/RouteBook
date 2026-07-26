package com.seth.routebook.repository;

import com.seth.routebook.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByKnowledgeEntryId(Long knowledgeEntryId);
}
