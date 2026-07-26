package com.seth.routebook.exception;

/**
 * Thrown when a lookup by id (driver, route, stop, location, knowledge entry)
 * finds nothing. Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
