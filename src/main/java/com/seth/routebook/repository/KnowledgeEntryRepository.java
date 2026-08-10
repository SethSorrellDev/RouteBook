package com.seth.routebook.repository;

import com.seth.routebook.domain.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
    List<KnowledgeEntry> findByRouteId(Long routeId);
    List<KnowledgeEntry> findByStopId(Long stopId);

    /**
     * A real database-level search - case-insensitive substring match
     * on title/body, combinable with the existing route/stop filters.
     * Replaces the earlier approach of fetching every entry and
     * filtering in memory (previously done client-side in the React
     * app, and even the backend's own findFiltered() did the equivalent
     * in a Java stream). All three filter params are optional; passing
     * null for any of them skips that condition entirely.
     */
    // k.body is @Lob (CLOB) - H2's LOWER() function rejects a CLOB
    // argument directly, so it must be CAST to a string first. This
    // only surfaced once search became a real DB query instead of a
    // Java-side .toLowerCase() call, which never cared about SQL types.
    @Query("SELECT k FROM KnowledgeEntry k WHERE " +
           "(:routeId IS NULL OR k.route.id = :routeId) AND " +
           "(:stopId IS NULL OR k.stop.id = :stopId) AND " +
           "(:query IS NULL OR LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(CAST(k.body AS string)) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeEntry> search(
            @Param("routeId") Long routeId,
            @Param("stopId") Long stopId,
            @Param("query") String query
    );
}
