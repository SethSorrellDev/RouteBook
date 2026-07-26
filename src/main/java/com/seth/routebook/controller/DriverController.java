package com.seth.routebook.controller;

import com.seth.routebook.dto.DriverDto;
import com.seth.routebook.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public List<DriverDto> getAll() {
        return driverService.findAll();
    }

    @GetMapping("/{id}")
    public DriverDto getById(@PathVariable Long id) {
        return driverService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DriverDto> create(@Valid @RequestBody DriverDto request) {
        return ResponseEntity.ok(driverService.create(request));
    }
}
