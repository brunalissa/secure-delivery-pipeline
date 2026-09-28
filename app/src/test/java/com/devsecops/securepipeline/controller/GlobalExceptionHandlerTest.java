package com.devsecops.securepipeline.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsecops.securepipeline.dto.ApiErrorResponse;
import com.devsecops.securepipeline.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * GlobalExceptionHandlerTest — Unit Tests for GlobalExceptionHandler
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Tests the exception handler in isolation to verify correct HTTP status codes and error
 * response bodies for each exception type.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("should return 404 for ResourceNotFoundException")
  void shouldReturn404ForResourceNotFound() {
    ApiErrorResponse response =
        handler.handleResourceNotFound(new ResourceNotFoundException("Task not found with id: 1"));

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.error()).isEqualTo("Not Found");
    assertThat(response.message()).contains("Task not found");
  }

  @Test
  @DisplayName("should return 400 for MethodArgumentNotValidException")
  void shouldReturn400ForValidationException() throws Exception {
    // Create a mock binding result with a field error
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "task");
    bindingResult.addError(new FieldError("task", "title", "Title is required"));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(
            new org.springframework.core.MethodParameter(
                TaskController.class.getMethod(
                    "createTask", com.devsecops.securepipeline.dto.TaskCreateDto.class),
                0),
            bindingResult);

    ApiErrorResponse response = handler.handleValidationException(ex);

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.validationErrors()).hasSize(1);
    assertThat(response.validationErrors().get(0).field()).isEqualTo("title");
  }

  @Test
  @DisplayName("should return 500 for generic Exception")
  void shouldReturn500ForGenericException() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleGenericException(new RuntimeException("Unexpected error"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(500);
    // Should NOT expose the raw exception message
    assertThat(response.getBody().message()).doesNotContain("Unexpected error");
  }

  @Test
  @DisplayName("should return 400 for MethodArgumentTypeMismatchException")
  void shouldReturn400ForTypeMismatch() {
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException(
            "abc", Long.class, "id", null, new RuntimeException());

    ApiErrorResponse response = handler.handleTypeMismatch(ex);

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.message()).contains("id");
  }

  @Test
  void shouldHandleUnknownRequiredType() {
    var ex = new MethodArgumentTypeMismatchException("abc", null, "id", null, null);
    assertThat(handler.handleTypeMismatch(ex).message()).contains("unknown");
  }
}
