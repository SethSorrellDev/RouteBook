package com.seth.routebook.domain;

import com.seth.routebook.domain.enums.KnowledgeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * The core payload: a piece of institutional knowledge about a Route
 * or a Stop - never both. Both route and stop are nullable at the
 * column level; the XOR rule is enforced here as a first line of
 * defense via @PrePersist/@PreUpdate, with the authoritative check
 * living in the service layer (Phase 3), per project convention.
 */
@Entity
@Table(name = "knowledge_entries")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Lob
    @Column(nullable = false)
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnowledgeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id")
    private Stop stop;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void validateExactlyOneTarget() {
        boolean hasRoute = route != null;
        boolean hasStop = stop != null;
        if (hasRoute == hasStop) {
            throw new IllegalStateException(
                "KnowledgeEntry must target exactly one of Route or Stop, never both or neither."
            );
        }
    }
}
