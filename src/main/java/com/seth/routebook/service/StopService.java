package com.seth.routebook.service;

import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.domain.Location;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.dto.CreateStopRequest;
import com.seth.routebook.dto.StopDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.KnowledgeEntryRepository;
import com.seth.routebook.repository.LocationRepository;
import com.seth.routebook.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StopService {

    private final StopRepository stopRepository;
    private final RouteService routeService;
    private final LocationService locationService;
    private final LocationRepository locationRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final AttachmentService attachmentService;

    public List<StopDto> findAllForRoute(Long routeId) {
        routeService.getEntityOrThrow(routeId);
        return stopRepository.findByRouteId(routeId).stream()
                .map(this::toDto)
                .toList();
    }

    public StopDto findById(Long id) {
        return toDto(getEntityOrThrow(id));
    }

    /**
     * Creates the Location and the Stop together in one transaction.
     * Previously the frontend made two separate API calls (create
     * Location, then create Stop) - if the second failed for any
     * reason, the Location was left orphaned with nothing pointing at
     * it. Wrapping both writes in @Transactional means a failure at
     * either step rolls back both, so an orphaned Location is no
     * longer possible.
     */
    @Transactional
    public StopDto create(Long routeId, CreateStopRequest request) {
        Route route = routeService.getEntityOrThrow(routeId);

        Location location = new Location();
        location.setAddressLine1(request.location().addressLine1());
        location.setAddressLine2(request.location().addressLine2());
        location.setCity(request.location().city());
        location.setState(request.location().state());
        location.setZipCode(request.location().zipCode());
        location.setLatitude(request.location().latitude());
        location.setLongitude(request.location().longitude());
        Location savedLocation = locationRepository.save(location);

        Stop stop = new Stop();
        stop.setCustomerName(request.customerName());
        stop.setSequenceOrder(request.sequenceOrder());
        stop.setRoute(route);
        stop.setLocation(savedLocation);

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
     * @Transactional ensures the DB-side cascade (entry deletions + stop
     * deletion) commits or rolls back as one unit; the R2 object deletes
     * happen outside the DB transaction boundary since they're calls to
     * an external service, not something a JPA transaction can roll back.
     */
    @Transactional
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
