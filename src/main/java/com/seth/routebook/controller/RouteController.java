package com.seth.routebook.controller;

import com.seth.routebook.dto.RouteDto;
import com.seth.routebook.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public List<RouteDto> getAll() {
        return routeService.findAll();
    }

    @GetMapping("/{id}")
    public RouteDto getById(@PathVariable Long id) {
        return routeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<RouteDto> create(@Valid @RequestBody RouteDto request) {
        return ResponseEntity.ok(routeService.create(request));
    }
}
