package com.seth.routebook.controller;

import com.seth.routebook.dto.StopDto;
import com.seth.routebook.service.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Separate from StopController (which is nested under
 * /api/routes/{routeId}/stops for listing/creating, since a stop can't
 * be created without knowing its route) - a stop's own id is sufficient
 * to look it up, update it, or delete it, so these live at a flat
 * /api/stops/{id} path instead of duplicating the routeId prefix.
 */
@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopByIdController {

    private final StopService stopService;

    @GetMapping("/{id}")
    public StopDto getById(@PathVariable Long id) {
        return stopService.findById(id);
    }

    @PutMapping("/{id}")
    public StopDto update(@PathVariable Long id, @Valid @RequestBody StopDto request) {
        return stopService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stopService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
