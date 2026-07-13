package com.devsecops.securepipeline.model;

/**
 * TaskStatus — Enumeration of Task Lifecycle States
 * 
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}.
 * Using STRING (not ORDINAL) is safer because reordering the constants here does
 * not corrupt existing database rows.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}