package com.seth.routebook.controller;

import com.seth.routebook.dto.LocationDto;
import com.seth.routebook.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDto> getAll() {
        return locationService.findAll();
    }

    @GetMapping("/{id}")
    public LocationDto getById(@PathVariable Long id) {
        return locationService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(@Valid @RequestBody LocationDto request) {
        return ResponseEntity.ok(locationService.create(request));
    }
}
