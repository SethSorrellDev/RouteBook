package com.seth.routebook.service;

import com.seth.routebook.domain.Driver;
import com.seth.routebook.domain.Route;
import com.seth.routebook.dto.RouteDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final DriverService driverService;

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
