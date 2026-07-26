package com.seth.routebook.domain.enums;

/**
 * Category of institutional knowledge captured in a KnowledgeEntry.
 * Stored as STRING in the database, never ordinal, so reordering
 * or adding values later doesn't corrupt existing data.
 */
public enum KnowledgeCategory {
    ACCESS,      // How to get into a building/dock/gate
    GATE_CODE,   // Specific codes for gates, doors, keypads
    PARKING,     // Where to park the van/truck
    HAZARD,      // Dogs, low clearance, tight turns, etc.
    CONTACT,     // Who to call/ask for at this location
    OTHER
}
