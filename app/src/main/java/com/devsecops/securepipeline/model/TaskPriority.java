package com.devsecops.securepipeline.model;

/**
 * TaskPriority — Enumeration of Task Priority Levels
 *
 * <p>Stored as a string in the database. STRING ordering is alphabetical (CRITICAL → C, HIGH → H,
 * LOW → L, MEDIUM → M) — if you need semantic ordering, use a custom AttributeConverter instead.
 */
public enum TaskPriority {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}
