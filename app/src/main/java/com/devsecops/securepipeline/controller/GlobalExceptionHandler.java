package com.devsecops.securepipeline.controller;

import com.devsecops.securepipeline.dto.ApiErrorResponse;
import com.devsecops.securepipeline.dto.ApiErrorResponse.ValidationError;
import com.devsecops.securepipeline.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * GlobalExceptionHandler — Centralized Exception Handling
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This class handles ALL exceptions thrown by ALL controllers in the application. Benefits of
 * centralized exception handling:
 *
 * <ul>
 *   <li><b>Consistency:</b> Every error response has the same structure ({@link ApiErrorResponse}).
 *   <li><b>Decoupling:</b> Controllers don't need try-catch blocks.
 *   <li><b>Security:</b> Stack traces are never leaked to clients. Only a generic message is
 *       returned; the full stack trace is logged server-side.
 *   <li><b>Extensibility:</b> Add new exception types by adding new handlers below.
 * </ul>
 *
 * <p><b>How it works:</b> When a controller method throws an exception, Spring intercepts it and
 * looks for an {@code @ExceptionHandler} method matching the exception type. This advice (via
 * {@code @RestControllerAdvice}) applies globally.
 *
 * <p><b>Security note:</b> The generic 500 error message intentionally does NOT include the
 * exception message. Exposing internal errors to clients can leak information useful to attackers.
 * We log the full stack trace internally and return a safe generic message.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ─────────────────────────────────────────────────────────────────────────
  // 404 — Resource Not Found (custom exception)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Handle {@link ResourceNotFoundException} — thrown by the service layer when a resource is not
   * found. Returns 404.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("Resource not found");
    return new ApiErrorResponse(
        LocalDateTime.now(),
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        ex.getMessage(),
        null,
        null);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 400 — Validation Error (Bean Validation)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Handle {@link MethodArgumentNotValidException} — thrown when @Valid validation fails on
   * a @RequestBody parameter. Returns 400 with a list of field-level validation errors.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
    log.warn("Validation failed");

    // Build a list of per-field validation errors
    List<ValidationError> validationErrors =
        ex.getBindingResult().getFieldErrors().stream().map(this::mapToValidationError).toList();

    return new ApiErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        "Input validation failed",
        null,
        validationErrors);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 400 — Type mismatch (e.g. "abc" provided for Long parameter)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Handle {@link MethodArgumentTypeMismatchException} — thrown when a path variable or request
   * param cannot be converted to the target type. Example: {@code /api/v1/tasks/abc} where "abc"
   * cannot be a Long.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    log.warn("Type mismatch");
    Class<?> requiredType = ex.getRequiredType();
    String message =
        String.format(
            "Parameter '%s' must be of type %s",
            ex.getName(), requiredType != null ? requiredType.getSimpleName() : "unknown");
    return new ApiErrorResponse(
        LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", message, null, null);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 400 — Illegal argument (generic business rule violation)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Handle {@link IllegalArgumentException} — a common runtime exception that may indicate invalid
   * argument values. Returns 400.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument");
    return new ApiErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        ex.getMessage(),
        null,
        null);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 500 — Fallback for ALL other unhandled exceptions
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Fallback handler for any exception not explicitly handled above.
   *
   * <p><b>SECURITY:</b> The response message is intentionally generic — NEVER expose the raw
   * exception message to the client, as it may contain internal details (database connection
   * strings, SQL syntax, file paths).
   *
   * <p>The full stack trace is logged at ERROR level for debugging.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred", ex);
    ApiErrorResponse response =
        new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            null,
            null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PRIVATE HELPERS
  // ─────────────────────────────────────────────────────────────────────────

  /** Convert a Spring {@link FieldError} into our API {@link ValidationError} record. */
  private ValidationError mapToValidationError(FieldError fieldError) {
    return new ValidationError(
        fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
  }
}
