package com.seth.routebook.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error shape for every API error response, so the future
 * React frontend has exactly one shape to parse regardless of what
 * went wrong. fieldErrors is null except for Bean Validation failures,
 * where it maps field name -> validation message.
 */
public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), null);
    }

    public ErrorResponse(int status, String message, Map<String, String> fieldErrors) {
        this(status, message, Instant.now(), fieldErrors);
    }
}
