package com.seth.routebook.dto;

import com.seth.routebook.domain.enums.KnowledgeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// routeId/stopId deliberately have no @NotNull here - the XOR relationship
// (exactly one, not both, not neither) isn't expressible with standard Bean
// Validation annotations, so it's enforced in KnowledgeEntryService instead.
public record KnowledgeEntryDto(
        Long id,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "body is required")
        String body,

        @NotNull(message = "category is required")
        KnowledgeCategory category,

        Long routeId,
        Long stopId
) {
}
