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
    // body is a plain TEXT column (see KnowledgeEntry.java), so LOWER()
    // works directly here without any CAST - no more LOB/dialect quirks.
    @Query("SELECT k FROM KnowledgeEntry k WHERE " +
           "(:routeId IS NULL OR k.route.id = :routeId) AND " +
           "(:stopId IS NULL OR k.stop.id = :stopId) AND " +
           "(:query IS NULL OR LOWER(k.title) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(k.body) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    List<KnowledgeEntry> search(
            @Param("routeId") Long routeId,
            @Param("stopId") Long stopId,
            @Param("query") String query
    );
}
