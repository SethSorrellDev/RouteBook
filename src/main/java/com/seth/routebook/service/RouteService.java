package com.seth.routebook.service;

import com.seth.routebook.domain.Driver;
import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.dto.RouteDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.KnowledgeEntryRepository;
import com.seth.routebook.repository.RouteRepository;
import com.seth.routebook.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final DriverService driverService;
    private final StopRepository stopRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;
    private final AttachmentService attachmentService;

    public List<RouteDto> findAll() {
        return routeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public RouteDto findById(Long id) {
        Route route = getEntityOrThrow(id);
        return toDto(route);
    }

    public RouteDto create(RouteDto request) {
        Route route = new Route();
        route.setName(request.name());
        route.setDescription(request.description());

        if (request.driverId() != null) {
            Driver driver = driverService.getEntityOrThrow(request.driverId());
            route.setDriver(driver);
        }

        Route saved = routeRepository.save(route);
        return toDto(saved);
    }

    public RouteDto update(Long id, RouteDto request) {
        Route route = getEntityOrThrow(id);
        route.setName(request.name());
        route.setDescription(request.description());

        if (request.driverId() != null) {
            Driver driver = driverService.getEntityOrThrow(request.driverId());
            route.setDriver(driver);
        } else {
            route.setDriver(null);
        }

        Route saved = routeRepository.save(route);
        return toDto(saved);
    }

    /**
     * Deleting a route cascades through everything that depends on it:
     * route-level knowledge entries, every stop under the route, and
     * every knowledge entry belonging to each of those stops - plus
     * each entry's attachments, both the R2 object and the DB row.
     * Stops themselves are deleted automatically by JPA's existing
     * cascade=ALL/orphanRemoval on Route.stops once the route is removed.
     */
    @Transactional
    public void delete(Long id) {
        Route route = getEntityOrThrow(id);

        deleteKnowledgeEntriesAndAttachments(knowledgeEntryRepository.findByRouteId(id));

        List<Stop> stops = stopRepository.findByRouteId(id);
        for (Stop stop : stops) {
            deleteKnowledgeEntriesAndAttachments(knowledgeEntryRepository.findByStopId(stop.getId()));
        }

        routeRepository.delete(route);
    }

    private void deleteKnowledgeEntriesAndAttachments(List<KnowledgeEntry> entries) {
        for (KnowledgeEntry entry : entries) {
            attachmentService.deleteAllForKnowledgeEntry(entry.getId());
            knowledgeEntryRepository.delete(entry);
        }
    }

    // Package-private so StopController/StopService can reuse this lookup.
    Route getEntityOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No route found with id " + id));
    }

    private RouteDto toDto(Route route) {
        Long driverId = route.getDriver() != null ? route.getDriver().getId() : null;
        return new RouteDto(
                route.getId(),
                route.getName(),
                route.getDescription(),
                driverId
        );
    }
}
