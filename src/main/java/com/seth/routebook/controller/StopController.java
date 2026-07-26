package com.seth.routebook.controller;

import com.seth.routebook.dto.StopDto;
import com.seth.routebook.service.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes/{routeId}/stops")
@RequiredArgsConstructor
public class StopController {

    private final StopService stopService;

    @GetMapping
    public List<StopDto> getAllForRoute(@PathVariable Long routeId) {
        return stopService.findAllForRoute(routeId);
    }

    @PostMapping
    public ResponseEntity<StopDto> create(@PathVariable Long routeId, @Valid @RequestBody StopDto request) {
        return ResponseEntity.ok(stopService.create(routeId, request));
    }
}
