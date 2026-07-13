package com.devsecops.securepipeline.controller;

import com.devsecops.securepipeline.dto.TaskCreateDTO;
import com.devsecops.securepipeline.dto.TaskResponseDTO;
import com.devsecops.securepipeline.dto.TaskUpdateDTO;
import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskController — REST API Controller (HTTP Layer)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This controller exposes a complete RESTful CRUD API for Task resources at
 * {@code /api/v1/tasks}. Key REST principles demonstrated:
 *
 * <ul>
 *   <li><b>Resource-based URLs:</b> Nouns not verbs. {@code /tasks} not {@code /getTasks}.</li>
 *   <li><b>HTTP methods map to operations:</b>
 *       GET = read, POST = create, PUT/PATCH = update, DELETE = delete.</li>
 *   <li><b>Proper status codes:</b>
 *       200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found.</li>
 *   <li><b>API versioning:</b> {@code /api/v1/} prefix supports future evolution.</li>
 *   <li><b>Validation:</b> {@code @Valid} triggers Bean Validation on request body.</li>
 *   <li><b>Thin controller:</b> Delegates all business logic to {@link TaskService}.</li>
 * </ul>
 *
 * <p><b>Why @RestController?</b>
 * It's a composite of {@code @Controller} + {@code @ResponseBody}. Every method
 * returns a domain object (DTO) that Spring serializes to JSON automatically.
 * No view resolution happens — this is a pure REST API.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ═════════════════════════════════════════════════════════════════════════
    // POST   /api/v1/tasks           — Create a new task
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Create a new task.
     *
     * <p>{@code @Valid} triggers Bean Validation on the request body. If
     * validation fails (e.g. blank title), a {@code MethodArgumentNotValidException}
     * is thrown, which is caught by {@link GlobalExceptionHandler}.
     *
     * <p>Returns 201 Created with the Location header pointing to the newly created
     * resource — a REST best practice. The response body contains the full resource
     * including the server-generated id and timestamps.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponseDTO createTask(@Valid @RequestBody TaskCreateDTO createDTO) {
        log.info("REST request to create task: {}", createDTO.title());
        return taskService.createTask(createDTO);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET    /api/v1/tasks           — Get all tasks (optionally filtered)
    // GET    /api/v1/tasks/{id}      — Get a single task
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Get all tasks, optionally filtered by status or search keyword.
     *
     * <p>Demonstrates the "query string" pattern for optional parameters:
     * <ul>
     *   <li>{@code ?status=PENDING}       — filter by status</li>
     *   <li>{@code ?search=deploy}         — search by title keyword</li>
     *   <li>no params                      — return all tasks</li>
     * </ul>
     */
    @GetMapping
    public List<TaskResponseDTO> getAllTasks(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "search", required = false) String searchKeyword
    ) {
        log.info("REST request to get tasks - status: {}, search: {}", status, searchKeyword);

        if (status != null) {
            return taskService.getTasksByStatus(status);
        }
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            return taskService.searchTasksByTitle(searchKeyword);
        }
        return taskService.getAllTasks();
    }

    /**
     * Get a single task by ID.
     *
     * <p>If the task doesn't exist, {@link TaskService} throws
     * {@link ResourceNotFoundException}, which the
     * {@link GlobalExceptionHandler} converts to a 404 response.
     */
    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        log.info("REST request to get task by id: {}", id);
        return taskService.getTaskById(id);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUT    /api/v1/tasks/{id}      — Full update
    // PATCH  /api/v1/tasks/{id}      — Partial update
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Partially update a task (PATCH semantics).
     *
     * <p>Only non-null fields in the request body are applied. Unspecified
     * fields remain unchanged in the database.
     */
    @PatchMapping("/{id}")
    public TaskResponseDTO updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateDTO updateDTO
    ) {
        log.info("REST request to update task with id: {}", id);
        return taskService.updateTask(id, updateDTO);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/tasks/{id}      — Delete a task
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Delete a task by ID.
     *
     * <p>Returns 204 No Content (standard for DELETE operations) on success.
     * If the task doesn't exist, a 404 is returned.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        log.info("REST request to delete task with id: {}", id);
        taskService.deleteTask(id);
    }
}