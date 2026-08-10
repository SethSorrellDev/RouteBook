package com.seth.routebook.service;

import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.domain.Location;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.dto.StopDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.KnowledgeEntryRepository;
import com.seth.routebook.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StopService {

    private final StopRepository stopRepository;
    private final RouteService routeService;
    private final LocationService locationService;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final AttachmentService attachmentService;

    public List<StopDto> findAllForRoute(Long routeId) {
        // Throws ResourceNotFoundException if the route itself doesn't exist,
        // so callers get a clear 404 rather than a silently empty list.
        routeService.getEntityOrThrow(routeId);
        return stopRepository.findByRouteId(routeId).stream()
                .map(this::toDto)
                .toList();
    }

    public StopDto findById(Long id) {
        return toDto(getEntityOrThrow(id));
    }

    public StopDto create(Long routeId, StopDto request) {
        Route route = routeService.getEntityOrThrow(routeId);
        Location location = locationService.getEntityOrThrow(request.locationId());

        Stop stop = new Stop();
        stop.setCustomerName(request.customerName());
        stop.setSequenceOrder(request.sequenceOrder());
        stop.setRoute(route);
        stop.setLocation(location);

        Stop saved = stopRepository.save(stop);
        return toDto(saved);
    }

    // Note: a stop's route is intentionally immutable here - this app has
    // no "move stop to a different route" workflow, so routeId in the
    // request is ignored on update.
    public StopDto update(Long id, StopDto request) {
        Stop stop = getEntityOrThrow(id);
        stop.setCustomerName(request.customerName());
        stop.setSequenceOrder(request.sequenceOrder());

        if (request.locationId() != null && !request.locationId().equals(stop.getLocation().getId())) {
            Location location = locationService.getEntityOrThrow(request.locationId());
            stop.setLocation(location);
        }

        Stop saved = stopRepository.save(stop);
        return toDto(saved);
    }

    /**
     * Deleting a stop cascades to every knowledge entry that targets it,
     * and each of those entries' attachments (R2 object + DB row).
     */
    public void delete(Long id) {
        Stop stop = getEntityOrThrow(id);

        List<KnowledgeEntry> entries = knowledgeEntryRepository.findByStopId(id);
        for (KnowledgeEntry entry : entries) {
            attachmentService.deleteAllForKnowledgeEntry(entry.getId());
            knowledgeEntryRepository.delete(entry);
        }

        stopRepository.delete(stop);
    }

    // Package-private so KnowledgeEntryService can reuse this lookup.
    Stop getEntityOrThrow(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No stop found with id " + id));
    }

    private StopDto toDto(Stop stop) {
        return new StopDto(
                stop.getId(),
                stop.getCustomerName(),
                stop.getSequenceOrder(),
                stop.getRoute().getId(),
                stop.getLocation().getId()
        );
    }
}
