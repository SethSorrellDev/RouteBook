package com.seth.routebook.controller;

import com.seth.routebook.dto.AttachmentDto;
import com.seth.routebook.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/api/knowledge-entries/{knowledgeEntryId}/attachments")
    public List<AttachmentDto> getAllForEntry(@PathVariable Long knowledgeEntryId) {
        return attachmentService.findAllForKnowledgeEntry(knowledgeEntryId);
    }

    @PostMapping(value = "/api/knowledge-entries/{knowledgeEntryId}/attachments",
                 consumes = "multipart/form-data")
    public ResponseEntity<AttachmentDto> upload(
            @PathVariable Long knowledgeEntryId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(attachmentService.upload(knowledgeEntryId, file));
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
