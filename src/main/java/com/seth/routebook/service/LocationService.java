package com.seth.routebook.service;

import com.seth.routebook.domain.Location;
import com.seth.routebook.dto.LocationDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public List<LocationDto> findAll() {
        return locationRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public LocationDto findById(Long id) {
        return toDto(getEntityOrThrow(id));
    }

    public LocationDto create(LocationDto request) {
        Location location = new Location();
        location.setAddressLine1(request.addressLine1());
        location.setAddressLine2(request.addressLine2());
        location.setCity(request.city());
        location.setState(request.state());
        location.setZipCode(request.zipCode());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        Location saved = locationRepository.save(location);
        return toDto(saved);
    }

    // Package-private so StopService can reuse this lookup.
    Location getEntityOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No location found with id " + id));
    }

    private LocationDto toDto(Location location) {
        return new LocationDto(
                location.getId(),
                location.getAddressLine1(),
                location.getAddressLine2(),
                location.getCity(),
                location.getState(),
                location.getZipCode(),
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
