package com.devsecops.securepipeline.dto;

import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskCreateDTO — Data Transfer Object for creating tasks
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p><b>Why DTOs and not entities directly?</b>
 * Using separate DTOs as the API contract:
 * <ul>
 *   <li><b>Security:</b> Prevents mass-assignment attacks. Users can’t set
 *       {@code id}, {@code createdAt}, {@code updatedAt} via the API.</li>
 *   <li><b>Decoupling:</b> The API contract is independent of the database schema.
 *       We can change the entity structure without breaking existing clients.</li>
 *   <li><b>Validation separation:</b> Different validation rules for create vs
 *       update scenarios (e.g., partial updates).</li>
 *   <li><b>Serialization control:</b> We can add computed fields or exclude
 *       sensitive ones in the response DTOs.</li>
 * </ul>
 *
 * <p>Uses Bean Validation annotations. Spring automatically validates these
 * when the controller parameter is annotated with {@code @Valid}.</p>
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
public record TaskCreateDTO(

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @NotNull(message = "Status is required")
    TaskStatus status,

    @NotNull(message = "Priority is required")
    TaskPriority priority

) {
    // Java Records provide immutable DTOs with automatic getters (accessor methods).
    // For a record, fields are accessed as method calls: dto.title(), dto.status(), etc.
    // This is the modern Java 21+ way of creating DTOs without Lombok or boilerplate.
}