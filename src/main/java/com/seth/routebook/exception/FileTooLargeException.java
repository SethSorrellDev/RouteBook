package com.seth.routebook.exception;

/**
 * Thrown when an uploaded file exceeds its category's size limit
 * (25MB for photos/documents, 250MB for videos). Mapped to HTTP 400.
 */
public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(String message) {
        super(message);
    }
}
