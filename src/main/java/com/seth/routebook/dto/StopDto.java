package com.seth.routebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StopDto(
        Long id,

        @NotBlank(message = "customerName is required")
        String customerName,

        @NotNull(message = "sequenceOrder is required")
        Integer sequenceOrder,

        Long routeId,

        @NotNull(message = "locationId is required")
        Long locationId
) {
}
