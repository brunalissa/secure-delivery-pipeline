package com.devsecops.securepipeline.service;

import com.devsecops.securepipeline.dto.TaskCreateDTO;
import com.devsecops.securepipeline.dto.TaskResponseDTO;
import com.devsecops.securepipeline.dto.TaskUpdateDTO;
import com.devsecops.securepipeline.exception.ResourceNotFoundException;
import com.devsecops.securepipeline.model.Task;
import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskServiceTest — Unit Tests for TaskService
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Uses {@code @ExtendWith(MockitoExtension.class)} to enable Mockito.
 * This is a PURE UNIT TEST — NO Spring context, NO database, NO web server.
 *
 * <p>The {@link TaskRepository} is mocked via {@code @Mock}. We define what
 * the mock returns for each test case. This allows testing the service logic
 * in isolation.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Create task — verify save() is called with correct data</li>
 *   <li>Get task by ID — verify findById() and mapping</li>
 *   <li>Get all tasks — verify findAll() and mapping</li>
 *   <li>Update task — verify partial update logic (null fields skipped)</li>
 *   <li>Delete task — verify delete() is called</li>
 *   <li>Not found scenarios — verify ResourceNotFoundException is thrown</li>
 * </ul>
 *
 * <p>AssertJ is used for fluent assertions (assertThat(result)...). It provides
 * richer assertions than JUnit's built-in assertions.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    // ── Test fixtures ────────────────────────────────────────────────────────

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
            .id(1L)
            .title("Test Task")
            .description("Test Description")
            .status(TaskStatus.PENDING)
            .priority(TaskPriority.MEDIUM)
            .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CREATE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createTask()")
    class CreateTaskTests {

        @Test
        @DisplayName("should create and return a task when valid input")
        void shouldCreateTask() {
            // Arrange
            TaskCreateDTO createDTO = new TaskCreateDTO(
                "New Task", "Desc", TaskStatus.PENDING, TaskPriority.HIGH
            );
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                task.setId(1L);
                return task;
            });

            // Act
            TaskResponseDTO result = taskService.createTask(createDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("New Task");
            assertThat(result.status()).isEqualTo(TaskStatus.PENDING);
            assertThat(result.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.id()).isEqualTo(1L);

            verify(taskRepository).save(any(Task.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // READ Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskByIdTests {

        @Test
        @DisplayName("should return task when ID exists")
        void shouldReturnTaskWhenIdExists() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            TaskResponseDTO result = taskService.getTaskById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Test Task");

            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowWhenIdDoesNotExist() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");

            verify(taskRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasksTests {

        @Test
        @DisplayName("should return all tasks")
        void shouldReturnAllTasks() {
            when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

            List<TaskResponseDTO> result = taskService.getAllTasks();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Test Task");

            verify(taskRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no tasks exist")
        void shouldReturnEmptyList() {
            when(taskRepository.findAll()).thenReturn(List.of());

            List<TaskResponseDTO> result = taskService.getAllTasks();

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UPDATE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateTask()")
    class UpdateTaskTests {

        @Test
        @DisplayName("should update only non-null fields (partial update)")
        void shouldUpdateOnlyNonNullFields() {
            // Arrange: only title and status are set; description and priority are null
            TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "Updated Title", null, TaskStatus.COMPLETED, null
            );
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            // Act
            TaskResponseDTO result = taskService.updateTask(1L, updateDTO);

            // Assert: title and status are updated, description and priority unchanged
            assertThat(result.title()).isEqualTo("Updated Title");
            assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
            // Original values should remain
            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when updating non-existent task")
        void shouldThrowWhenUpdatingMissingTask() {
            TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "Updated", null, TaskStatus.COMPLETED, null
            );
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(999L, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should not update title when title is blank")
        void shouldNotUpdateTitleWhenBlank() {
            TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "", null, null, null
            );
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            TaskResponseDTO result = taskService.updateTask(1L, updateDTO);

            // Title should remain unchanged
            assertThat(result.title()).isEqualTo("Test Task");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTaskTests {

        @Test
        @DisplayName("should delete task when ID exists")
        void shouldDeleteTask() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            taskService.deleteTask(1L);

            verify(taskRepository).delete(sampleTask);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when deleting non-existent task")
        void shouldThrowWhenDeletingMissingTask() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SEARCH Tests
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchTasksByTitle()")
    class SearchTasksTests {

        @Test
        @DisplayName("should return matching tasks by keyword")
        void shouldReturnMatchingTasks() {
            when(taskRepository.searchByTitle("test")).thenReturn(List.of(sampleTask));

            List<TaskResponseDTO> result = taskService.searchTasksByTitle("test");

            assertThat(result).hasSize(1);
            verify(taskRepository).searchByTitle("test");
        }
    }
}