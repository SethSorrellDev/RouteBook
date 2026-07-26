package com.seth.routebook.service;

import com.seth.routebook.domain.Location;
import com.seth.routebook.domain.Route;
import com.seth.routebook.domain.Stop;
import com.seth.routebook.dto.StopDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.LocationRepository;
import com.seth.routebook.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StopService {

    private final StopRepository stopRepository;
    private final RouteService routeService;
    private final LocationRepository locationRepository;

    public List<StopDto> findAllForRoute(Long routeId) {
        // Throws ResourceNotFoundException if the route itself doesn't exist,
        // so callers get a clear 404 rather than a silently empty list.
        routeService.getEntityOrThrow(routeId);
        return stopRepository.findAll().stream()
                .filter(s -> s.getRoute().getId().equals(routeId))
                .map(this::toDto)
                .toList();
    }

    public StopDto create(Long routeId, StopDto request) {
        Route route = routeService.getEntityOrThrow(routeId);
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No location found with id " + request.locationId()));

        Stop stop = new Stop();
        stop.setCustomerName(request.customerName());
        stop.setSequenceOrder(request.sequenceOrder());
        stop.setRoute(route);
        stop.setLocation(location);

        Stop saved = stopRepository.save(stop);
        return toDto(saved);
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
