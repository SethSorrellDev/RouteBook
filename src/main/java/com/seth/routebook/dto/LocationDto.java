package com.seth.routebook.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationDto(
        Long id,

        @NotBlank(message = "addressLine1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "city is required")
        String city,

        @NotBlank(message = "state is required")
        String state,

        @NotBlank(message = "zipCode is required")
        String zipCode,

        Double latitude,
        Double longitude
) {
}
