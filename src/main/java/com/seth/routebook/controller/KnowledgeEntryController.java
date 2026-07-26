package com.seth.routebook.controller;

import com.seth.routebook.dto.KnowledgeEntryDto;
import com.seth.routebook.service.KnowledgeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-entries")
@RequiredArgsConstructor
public class KnowledgeEntryController {

    private final KnowledgeEntryService knowledgeEntryService;

    @GetMapping
    public List<KnowledgeEntryDto> getFiltered(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stopId) {
        return knowledgeEntryService.findFiltered(routeId, stopId);
    }

    @GetMapping("/{id}")
    public KnowledgeEntryDto getById(@PathVariable Long id) {
        return knowledgeEntryService.findById(id);
    }

    @PostMapping
    public ResponseEntity<KnowledgeEntryDto> create(@Valid @RequestBody KnowledgeEntryDto request) {
        return ResponseEntity.ok(knowledgeEntryService.create(request));
    }
}
