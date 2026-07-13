package com.devsecops.securepipeline.service;

import com.devsecops.securepipeline.dto.TaskCreateDTO;
import com.devsecops.securepipeline.dto.TaskResponseDTO;
import com.devsecops.securepipeline.dto.TaskUpdateDTO;
import com.devsecops.securepipeline.exception.ResourceNotFoundException;
import com.devsecops.securepipeline.model.Task;
import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskService — Business Logic Layer
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>The service layer sits between the controller and the repository. It is
 * responsible for:
 * <ul>
 *   <li>Business logic & validation rules</li>
 *   <li>DTO ↔ Entity conversion (mapping)</li>
 *   <li>Transaction management ({@code @Transactional})</li>
 *   <li>Throwing domain-specific exceptions</li>
 * </ul>
 *
 * <p><b>Why a service layer?</b> Controllers should stay thin. The controller
 * handles HTTP concerns (parsing requests, serializing responses) but delegates
 * all business logic to services. This separation is critical because:
 * <ol>
 *   <li>Business logic is reusable (can be called from other services, batch
 *       jobs, message consumers, etc.)</li>
 *   <li>Services can be unit-tested without mocking the web layer</li>
 *   <li>Transactions are managed here, not at the controller layer</li>
 * </ol>
 *
 * <p><b>Annotations:</b>
 * <ul>
 *   <li>{@code @Service} — Stereotype annotation. Tells Spring to create a
 *       singleton bean and inject it where needed.</li>
 *   <li>{@code @Transactional} — Wraps each method in a database transaction.
 *       Read-only methods can use {@code @Transactional(readOnly = true)} which
 *       allows Hibernate to skip dirty checking, improving performance.</li>
 *   <li>{@code @RequiredArgsConstructor} (Lombok) — Generates a constructor
 *       taking {@code final} fields. Spring uses this constructor for DI.</li>
 *   <li>{@code @Slf4j} (Lombok) — Generates a logger field {@code log}.</li>
 * </ul>
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    // Dependency injection by constructor (final field + @RequiredArgsConstructor)
    private final TaskRepository taskRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Create a new task.
     *
     * <p>Converts the DTO to an entity, flags as new (id=null), and persists.
     * The entity returned by {@code save()} will have id, createdAt, updatedAt populated.
     *
     * @param createDTO validated input from the controller
     * @return a response DTO with generated id and timestamps
     */
    public TaskResponseDTO createTask(TaskCreateDTO createDTO) {
        log.info("Creating new task with title: {}", createDTO.title());

        Task task = Task.builder()
                .title(createDTO.title())
                .description(createDTO.description())
                .status(createDTO.status())
                .priority(createDTO.priority())
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created with id: {}", savedTask.getId());
        return mapToResponseDTO(savedTask);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // READ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Get all tasks.
     *
     * <p>Uses {@code readOnly = true} because no modification is made — this
     * allows Hibernate to optimize the transaction (no dirty checking).
     *
     * @return list of all tasks as response DTOs
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Get a task by its ID. Throws {@link ResourceNotFoundException} if not found.
     *
     * @param id the task ID
     * @return a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long id) {
        log.debug("Fetching task with id: {}", id);
        Task task = findTaskOrThrow(id);
        return mapToResponseDTO(task);
    }

    /**
     * Get tasks filtered by status.
     *
     * @param status task status to filter by
     * @return list of matching tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByStatus(TaskStatus status) {
        log.debug("Fetching tasks with status: {}", status);
        return taskRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Search tasks by title keyword.
     *
     * @param keyword search keyword (case-insensitive partial match)
     * @return list of matching tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> searchTasksByTitle(String keyword) {
        log.debug("Searching tasks with keyword: {}", keyword);
        return taskRepository.searchByTitle(keyword)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Get count of tasks by status — used for metrics/dashboards.
     *
     * @param status the status to count
     * @return number of tasks with that status
     */
    @Transactional(readOnly = true)
    public long countTasksByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Partially update a task (PATCH semantics).
     *
     * <p>Only non-null fields in the DTO are applied. This is the standard
     * approach for partial updates.
     *
     * @param id        the ID of the task to update
     * @param updateDTO fields to update (all optional)
     * @return updated task as response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public TaskResponseDTO updateTask(Long id, TaskUpdateDTO updateDTO) {
        log.info("Updating task with id: {}", id);
        Task task = findTaskOrThrow(id);

        // Apply non-null fields from the DTO to the entity
        // Only non-null fields constitute a "partial update" (PATCH)
        if (Objects.nonNull(updateDTO.title()) && !updateDTO.title().isBlank()) {
            task.setTitle(updateDTO.title());
        }
        if (Objects.nonNull(updateDTO.description())) {
            task.setDescription(updateDTO.description());
        }
        if (Objects.nonNull(updateDTO.status())) {
            task.setStatus(updateDTO.status());
        }
        if (Objects.nonNull(updateDTO.priority())) {
            task.setPriority(updateDTO.priority());
        }

        // No explicit save() call needed — @Transactional + JPA dirty checking
        // will flush the changes to the database at transaction commit.
        log.info("Task with id: {} updated", id);
        return mapToResponseDTO(task);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Delete a task by ID.
     *
     * @param id the ID of the task to delete
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public void deleteTask(Long id) {
        log.info("Deleting task with id: {}", id);
        Task task = findTaskOrThrow(id);
        taskRepository.delete(task);
        log.info("Task with id: {} deleted", id);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Find a task by ID or throw a domain-specific exception.
     *
     * <p>This centralizes the "not found" logic and resulting error message,
     * so all read/update/delete methods behave consistently.
     *
     * @param id the task ID
     * @return the task entity
     * @throws ResourceNotFoundException if not found
     */
    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    /**
     * Map a Task entity to a TaskResponseDTO.
     *
     * <p>This is done manually (rather than with MapStruct) for simplicity.
     * In a large project, consider using MapStruct for automatic mapping.
     *
     * @param task the JPA entity
     * @return the response DTO
     */
    private TaskResponseDTO mapToResponseDTO(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}