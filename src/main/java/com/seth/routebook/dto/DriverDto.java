package com.seth.routebook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DriverDto(
        Long id,

        @NotBlank(message = "employeeId is required")
        String employeeId,

        @NotBlank(message = "firstName is required")
        String firstName,

        @NotBlank(message = "lastName is required")
        String lastName,

        @Email(message = "email must be a valid email address")
        String email
) {
}
