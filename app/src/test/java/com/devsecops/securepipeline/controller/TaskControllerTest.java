package com.devsecops.securepipeline.controller;

import com.devsecops.securepipeline.dto.TaskCreateDTO;
import com.devsecops.securepipeline.dto.TaskResponseDTO;
import com.devsecops.securepipeline.dto.TaskUpdateDTO;
import com.devsecops.securepipeline.exception.ResourceNotFoundException;
import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskControllerTest — Unit Tests for TaskController
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Uses {@code @WebMvcTest} which:
 * <ul>
 *   <li>Loads only the web layer (controllers, filters, advice) — NOT the full
 *       application context. This is FAST.</li>
 *   <li>Auto-configures MockMvc for sending HTTP requests without a real server.</li>
 *   <li>Does NOT load services, repositories, or database beans.</li>
 * </ul>
 *
 * <p>The {@code @MockBean} for {@link TaskService} replaces the real service
 * with a Mockito mock. We define what the mock returns for each test case.
 *
 * <p>This tests:
 * <ul>
 *   <li>HTTP status codes (200, 201, 204, 400, 404)</li>
 *   <li>JSON response structure</li>
 *   <li>Bean Validation triggers</li>
 *   <li>Exception handling maps to correct HTTP responses</li>
 * </ul>
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@WebMvcTest(TaskController.class)
@WithMockUser
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    // ═════════════════════════════════════════════════════════════════════════
    // Helper: build a sample response DTO
    // ═════════════════════════════════════════════════════════════════════════
    private TaskResponseDTO sampleResponse(Long id) {
        return new TaskResponseDTO(
            id, "Test Task", "Test Description",
            TaskStatus.PENDING, TaskPriority.MEDIUM,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CREATE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/tasks — Create Task")
    class CreateTask {

        @Test
        @DisplayName("should return 201 Created when valid input is provided")
        void shouldCreateTask() throws Exception {
            // Arrange
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "New Task", "Description", TaskStatus.PENDING, TaskPriority.HIGH
            );
            when(taskService.createTask(any(TaskCreateDTO.class)))
                .thenReturn(sampleResponse(1L));

            // Act & Assert
            mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("should return 400 Bad Request when title is blank")
        void shouldReturn400WhenTitleBlank() throws Exception {
            // Arrange — title is empty, violating @NotBlank
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "", "Description", TaskStatus.PENDING, TaskPriority.HIGH
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());

            // Verify the service was never called (validation happened before)
            verify(taskService, never()).createTask(any());
        }

        @Test
        @DisplayName("should return 400 Bad Request when title is too short")
        void shouldReturn400WhenTitleTooShort() throws Exception {
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "ab", "Description", TaskStatus.PENDING, TaskPriority.HIGH
            );

            mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 Bad Request when status is null")
        void shouldReturn400WhenStatusNull() throws Exception {
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "Valid Title", "Description", null, TaskPriority.HIGH
            );

            mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 Bad Request when priority is null")
        void shouldReturn400WhenPriorityNull() throws Exception {
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "Valid Title", "Description", TaskStatus.PENDING, null
            );

            mockMvc.perform(post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // READ Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/tasks — Read Tasks")
    class GetTasks {

        @Test
        @DisplayName("should return 200 OK with list of tasks")
        void shouldReturnAllTasks() throws Exception {
            when(taskService.getAllTasks())
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

            mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("should return 200 OK with task by ID")
        void shouldReturnTaskById() throws Exception {
            when(taskService.getTaskById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get("/api/v1/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        @DisplayName("should return 404 Not Found when task does not exist")
        void shouldReturn404ForMissingTask() throws Exception {
            when(taskService.getTaskById(999L))
                .thenThrow(new ResourceNotFoundException("Task not found with id: 999"));

            mockMvc.perform(get("/api/v1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 999"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when ID is not a number")
        void shouldReturn400ForNonNumericId() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should filter tasks by status query parameter")
        void shouldFilterByStatus() throws Exception {
            when(taskService.getTasksByStatus(TaskStatus.PENDING))
                .thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get("/api/v1/tasks?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("should search tasks by keyword query parameter")
        void shouldSearchByKeyword() throws Exception {
            when(taskService.searchTasksByTitle("deploy"))
                .thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get("/api/v1/tasks?search=deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

            verify(taskService).searchTasksByTitle("deploy");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UPDATE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/tasks/{id} — Update Task")
    class UpdateTask {

        @Test
        @DisplayName("should return 200 OK with updated task")
        void shouldUpdateTask() throws Exception {
            TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "Updated Title", null, TaskStatus.COMPLETED, null
            );
            when(taskService.updateTask(eq(1L), any(TaskUpdateDTO.class)))
                .thenReturn(sampleResponse(1L));

            mockMvc.perform(patch("/api/v1/tasks/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 Not Found when updating non-existent task")
        void shouldReturn404WhenUpdatingMissingTask() throws Exception {
            TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "Updated", null, TaskStatus.COMPLETED, null
            );
            when(taskService.updateTask(eq(999L), any(TaskUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Task not found with id: 999"));

            mockMvc.perform(patch("/api/v1/tasks/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id} — Delete Task")
    class DeleteTask {

        @Test
        @DisplayName("should return 204 No Content on successful deletion")
        void shouldDeleteTask() throws Exception {
            doNothing().when(taskService).deleteTask(1L);

            mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isNoContent());

            verify(taskService).deleteTask(1L);
        }

        @Test
        @DisplayName("should return 404 Not Found when deleting non-existent task")
        void shouldReturn404WhenDeletingMissingTask() throws Exception {
            doThrow(new ResourceNotFoundException("Task not found with id: 999"))
                .when(taskService).deleteTask(999L);

            mockMvc.perform(delete("/api/v1/tasks/999"))
                .andExpect(status().isNotFound());
        }
    }
}
