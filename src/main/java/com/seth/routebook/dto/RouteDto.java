package com.seth.routebook.dto;

import jakarta.validation.constraints.NotBlank;

public record RouteDto(
        Long id,

        @NotBlank(message = "name is required")
        String name,

        String description,

        // Nullable on purpose - a route can exist without an assigned driver yet
        Long driverId
) {
}
