package com.seth.routebook.exception;

/**
 * Thrown when an uploaded file's content type isn't on the allowed
 * whitelist (photos, PDFs, Word docs, text, spreadsheets, videos).
 * Mapped to HTTP 400.
 */
public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
