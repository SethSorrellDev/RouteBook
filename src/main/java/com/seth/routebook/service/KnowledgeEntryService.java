package com.seth.routebook.service;

import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.dto.KnowledgeEntryDto;
import com.seth.routebook.exception.InvalidKnowledgeEntryTargetException;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.KnowledgeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeEntryService {

    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final RouteService routeService;
    private final StopService stopService;
    private final AttachmentService attachmentService;

    public List<KnowledgeEntryDto> findFiltered(Long routeId, Long stopId) {
        return knowledgeEntryRepository.findAll().stream()
                .filter(ke -> routeId == null || (ke.getRoute() != null && ke.getRoute().getId().equals(routeId)))
                .filter(ke -> stopId == null || (ke.getStop() != null && ke.getStop().getId().equals(stopId)))
                .map(this::toDto)
                .toList();
    }

    public KnowledgeEntryDto findById(Long id) {
        return toDto(getEntityOrThrow(id));
    }

    public KnowledgeEntryDto create(KnowledgeEntryDto request) {
        validateExactlyOneTarget(request);

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(request.title());
        entry.setBody(request.body());
        entry.setCategory(request.category());
        applyTarget(entry, request);

        KnowledgeEntry saved = knowledgeEntryRepository.save(entry);
        return toDto(saved);
    }

    // Re-validates the XOR rule on update too, since the target (route
    // vs. stop) can change - not just title/body/category.
    public KnowledgeEntryDto update(Long id, KnowledgeEntryDto request) {
        validateExactlyOneTarget(request);

        KnowledgeEntry entry = getEntityOrThrow(id);
        entry.setTitle(request.title());
        entry.setBody(request.body());
        entry.setCategory(request.category());
        applyTarget(entry, request);

        KnowledgeEntry saved = knowledgeEntryRepository.save(entry);
        return toDto(saved);
    }

    /**
     * Deleting a knowledge entry cascades to its attachments - both the
     * R2 object and the database row for each one.
     */
    public void delete(Long id) {
        KnowledgeEntry entry = getEntityOrThrow(id);
        attachmentService.deleteAllForKnowledgeEntry(id);
        knowledgeEntryRepository.delete(entry);
    }

    private void validateExactlyOneTarget(KnowledgeEntryDto request) {
        boolean hasRoute = request.routeId() != null;
        boolean hasStop = request.stopId() != null;
        if (hasRoute == hasStop) {
            throw new InvalidKnowledgeEntryTargetException(
                    "A knowledge entry must target exactly one of routeId or stopId, not both or neither."
            );
        }
    }

    private void applyTarget(KnowledgeEntry entry, KnowledgeEntryDto request) {
        if (request.routeId() != null) {
            Route route = routeService.getEntityOrThrow(request.routeId());
            entry.setRoute(route);
            entry.setStop(null);
        } else {
            Stop stop = stopService.getEntityOrThrow(request.stopId());
            entry.setStop(stop);
            entry.setRoute(null);
        }
    }

    // Package-private so AttachmentService could reuse this lookup if it's
    // ever refactored; currently AttachmentService uses the repository
    // directly to avoid a circular bean dependency.
    KnowledgeEntry getEntityOrThrow(Long id) {
        return knowledgeEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No knowledge entry found with id " + id));
    }

    private KnowledgeEntryDto toDto(KnowledgeEntry entry) {
        Long routeId = entry.getRoute() != null ? entry.getRoute().getId() : null;
        Long stopId = entry.getStop() != null ? entry.getStop().getId() : null;
        return new KnowledgeEntryDto(
                entry.getId(),
                entry.getTitle(),
                entry.getBody(),
                entry.getCategory(),
                routeId,
                stopId
        );
    }
}
