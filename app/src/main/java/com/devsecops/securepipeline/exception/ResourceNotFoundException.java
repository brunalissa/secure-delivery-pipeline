package com.devsecops.securepipeline.exception;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * ResourceNotFoundException — Custom Domain Exception
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Thrown when a requested resource is not found in the database. The
 * {@link com.devsecops.securepipeline.controller.GlobalExceptionHandler}
 * catches this and converts it to a 404 NOT FOUND HTTP response.
 *
 * <p><b>Why a custom exception?</b>
 * Using specific exception types instead of generic RuntimeException allows:
 * <ol>
 *   <li>Precise HTTP status mapping (404 vs 500)</li>
 *   <li>Clearer code intent — {@code throw new ResourceNotFoundException("/tasks/999")}
 *      is self-documenting</li>
 *   <li>Testability — easily assert in tests that the correct exception is thrown</li>
 * </ol>
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * <p>Uses {@code RuntimeException(String)} which does NOT chain a cause.
     * The message should be user-friendly because it appears in the error response body.
     *
     * @param message the detail message (visible in the API response)
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with a message and cause.
     *
     * <p>Used when wrapping a lower-level exception (e.g., a DB exception)
     * to preserve the original stack trace while providing a user-friendly message.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}