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

    public List<KnowledgeEntryDto> findFiltered(Long routeId, Long stopId) {
        return knowledgeEntryRepository.findAll().stream()
                .filter(ke -> routeId == null || (ke.getRoute() != null && ke.getRoute().getId().equals(routeId)))
                .filter(ke -> stopId == null || (ke.getStop() != null && ke.getStop().getId().equals(stopId)))
                .map(this::toDto)
                .toList();
    }

    public KnowledgeEntryDto findById(Long id) {
        KnowledgeEntry entry = knowledgeEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No knowledge entry found with id " + id));
        return toDto(entry);
    }

    public KnowledgeEntryDto create(KnowledgeEntryDto request) {
        // Authoritative XOR check - this is the real enforcement point.
        // The @PrePersist on the entity is now just a defense-in-depth backstop.
        boolean hasRoute = request.routeId() != null;
        boolean hasStop = request.stopId() != null;
        if (hasRoute == hasStop) {
            throw new InvalidKnowledgeEntryTargetException(
                    "A knowledge entry must target exactly one of routeId or stopId, not both or neither."
            );
        }

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(request.title());
        entry.setBody(request.body());
        entry.setCategory(request.category());

        if (hasRoute) {
            Route route = routeService.getEntityOrThrow(request.routeId());
            entry.setRoute(route);
        } else {
            Stop stop = stopService.getEntityOrThrow(request.stopId());
            entry.setStop(stop);
        }

        KnowledgeEntry saved = knowledgeEntryRepository.save(entry);
        return toDto(saved);
    }

    // Package-private so AttachmentService can link uploads to a
    // KnowledgeEntry without duplicating the lookup/exception logic.
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
