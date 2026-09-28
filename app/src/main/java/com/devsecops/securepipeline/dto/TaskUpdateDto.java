package com.devsecops.securepipeline.dto;

import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import jakarta.validation.constraints.Size;

/**
 * ───────────────────────────────────────────────────────────────────────────── TaskUpdateDto —
 * Data Transfer Object for partial updates
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p><b>Why all fields are optional?</b> This DTO supports PATCH-style partial updates. Clients can
 * send just the fields they want to change. The service layer uses null-checking to determine which
 * fields to update (vs the "replace all" approach of PUT).
 *
 * <p>Using a record (immutable by default) is fine here because the service layer reads the values;
 * it does not need to modify them. If a field is null, the service simply skips the update for that
 * field.
 *
 * <p>For full replacement (PUT semantics), use {@link TaskCreateDto} instead.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
public record TaskUpdateDto(
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters") String title,
    @Size(max = 500, message = "Description must not exceed 500 characters") String description,
    TaskStatus status,
    TaskPriority priority) {

  // All fields intentionally nullable for PATCH semantics (partial update).
  // The @NotBlank / @NotNull constraints from TaskCreateDto are omitted here
  // because providing NO field at all should be valid for partial updates.
}
