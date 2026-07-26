package com.seth.routebook.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A file (photo, PDF, Word doc, video, etc.) attached to a KnowledgeEntry.
 * The actual bytes live in Cloudflare R2 - this row only tracks metadata
 * and the r2Key needed to locate/delete the object. One KnowledgeEntry
 * can have many attachments (e.g. two photos of the same gate).
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fileName;

    @NotBlank
    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    // The object's path/key within the R2 bucket - not the same as fileName,
    // since fileName can collide across uploads (r2Key includes a UUID prefix).
    @NotBlank
    @Column(nullable = false, unique = true)
    private String r2Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_entry_id", nullable = false)
    private KnowledgeEntry knowledgeEntry;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;
}
