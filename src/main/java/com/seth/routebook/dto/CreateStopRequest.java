package com.seth.routebook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request shape for creating a Stop, used only for the create endpoint -
 * distinct from StopDto (the resource representation returned by GETs),
 * since a new Stop always needs a brand-new Location created alongside
 * it, not a reference to an existing one. Nesting the location here lets
 * StopService create both in a single @Transactional method, so a
 * failure partway through never leaves an orphaned Location behind.
 */
public record CreateStopRequest(
        @NotBlank(message = "customerName is required")
        String customerName,

        @NotNull(message = "sequenceOrder is required")
        Integer sequenceOrder,

        @NotNull(message = "location is required")
        @Valid
        LocationDto location
) {
}
