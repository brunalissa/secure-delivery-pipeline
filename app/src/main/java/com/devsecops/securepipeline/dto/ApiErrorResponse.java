package com.devsecops.securepipeline.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ───────────────────────────────────────────────────────────────────────────── ApiErrorResponse —
 * Standardized error response body
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This DTO is returned for ALL error responses (4xx / 5xx). It provides:
 *
 * <ul>
 *   <li>{@code timestamp} — When the error occurred (useful for debugging)
 *   <li>{@code status} — HTTP status code (redundant with header but convenient for clients)
 *   <li>{@code error} — Short error reason (e.g. "Bad Request", "Not Found")
 *   <li>{@code message} — Human-readable error detail
 *   <li>{@code path} — The request path that triggered the error
 *   <li>{@code validationErrors} — List of field-level validation errors (for 400 responses)
 * </ul>
 *
 * <p>This is produced by the {@code GlobalExceptionHandler} and ensures all errors have a
 * consistent structure — crucial for API consumers to handle errors gracefully.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
public record ApiErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ValidationError> validationErrors) {
  /** Retains an immutable snapshot of validation errors. */
  public ApiErrorResponse {
    validationErrors = validationErrors == null ? null : List.copyOf(validationErrors);
  }

  /**
   * Nested record representing a single field validation error. Used when Bean Validation fails
   * (e.g. @NotBlank on a null field).
   */
  public record ValidationError(String field, String message, Object rejectedValue) {}
}
