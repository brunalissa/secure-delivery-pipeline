package com.devsecops.securepipeline.dto;

import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;

import java.time.LocalDateTime;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskResponseDTO — Data Transfer Object for API responses
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This DTO represents what the API returns to the client. It includes:
 * <ul>
 *   <li>All fields from {@link TaskCreateDTO}</li>
 *   <li>{@code id} — generated after persistence</li>
 *   <li>{@code createdAt} / {@code updatedAt} — audit timestamps</li>
 * </ul>
 *
 * <p><b>Why not return the entity directly?</b>
 * Returning the entity directly via JSON would:
 * <ol>
 *   <li>Expose the internal structure and coupling it to the API contract</li>
 *   <li>Risk exposing sensitive fields if added to the entity later</li>
 *   <li>Create a backward compatibility problem if the entity schema changes</li>
 * </ol>
 *
 * <p>Using a dedicated response DTO lets us evolve the API and database
 * independently.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
public record TaskResponseDTO(
    Long id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // No validation annotations here — this is an OUTPUT DTO, not an input.
    // The data already passed validation when it was created/updated.
}