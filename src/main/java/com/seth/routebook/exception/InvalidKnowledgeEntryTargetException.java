package com.seth.routebook.exception;

/**
 * Thrown when a KnowledgeEntry request targets both a Route and a Stop,
 * or neither. This is the authoritative XOR check - the @PrePersist
 * check on the entity itself remains a first line of defense, but this
 * is what actually runs during normal request handling, with a message
 * clear enough to show directly to the person using the app.
 */
public class InvalidKnowledgeEntryTargetException extends RuntimeException {
    public InvalidKnowledgeEntryTargetException(String message) {
        super(message);
    }
}
