package com.seth.routebook.service;

import com.seth.routebook.domain.Driver;
import com.seth.routebook.dto.DriverDto;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    public List<DriverDto> findAll() {
        return driverRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public DriverDto findById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No driver found with id " + id));
        return toDto(driver);
    }

    public DriverDto create(DriverDto request) {
        Driver driver = new Driver();
        driver.setEmployeeId(request.employeeId());
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setEmail(request.email());
        Driver saved = driverRepository.save(driver);
        return toDto(saved);
    }

    // Package-private so RouteService can reuse the same lookup + exception
    // behavior without duplicating the "not found" message format.
    Driver getEntityOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No driver found with id " + id));
    }

    private DriverDto toDto(Driver driver) {
        return new DriverDto(
                driver.getId(),
                driver.getEmployeeId(),
                driver.getFirstName(),
                driver.getLastName(),
                driver.getEmail()
        );
    }
}
