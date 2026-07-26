package com.seth.routebook.dto;

import java.time.Instant;

/**
 * downloadUrl is a presigned R2 URL generated fresh on every response -
 * it's never stored, since presigned URLs expire (15 min, per config).
 */
public record AttachmentDto(
        Long id,
        String fileName,
        String contentType,
        Long fileSizeBytes,
        Long knowledgeEntryId,
        Instant uploadedAt,
        String downloadUrl
) {
}
