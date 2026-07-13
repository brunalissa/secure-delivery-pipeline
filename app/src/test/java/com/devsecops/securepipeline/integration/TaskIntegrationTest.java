package com.devsecops.securepipeline.integration;

import com.devsecops.securepipeline.dto.TaskCreateDTO;
import com.devsecops.securepipeline.model.TaskPriority;
import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskIntegrationTest — Full Integration Tests with Testcontainers
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This is a FULL INTEGRATION test. Unlike unit tests, this:
 * <ul>
 *   <li>Starts a real PostgreSQL database in a Docker container (Testcontainers)</li>
 *   <li>Loads the complete Spring application context</li>
 *   <li>Executes real HTTP requests through MockMvc against the full stack</li>
 *   <li>Tests the entire pipeline: Controller → Service → Repository → Database</li>
 * </ul>
 *
 * <p><b>Why Testcontainers instead of H2?</b>
 * H2 is an in-memory database that doesn't perfectly match PostgreSQL behavior.
 * Differences in SQL dialect, JSON functions, and constraint handling can cause
 * tests to pass on H2 but fail on PostgreSQL in production. Testcontainers
 * uses a real PostgreSQL image, so what you test is what you run.
 *
 * <p><b>Requirements:</b> Docker must be running on the machine executing these
 * tests. In CI, Docker is available by default on GitHub Actions runners.
 *
 * <p><b>Performance note:</b> The container is started ONCE for the entire
 * test class (via {@code @Container} + {@code @Testcontainers}), not per test.
 * Each test cleans the database via {@code @BeforeEach} for isolation.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TaskIntegrationTest {

    // ═════════════════════════════════════════════════════════════════════════
    // Testcontainers — starts a real PostgreSQL Docker container
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * The PostgreSQL container. The {@code @Container} annotation ensures it
     * is started before all tests and stopped after all tests in this class.
     *
     * <p>The {@code withReuse} option (if enabled via ~testcontainers/reuse.properties)
     * keeps the container running between test runs for faster local development.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    // ── Spring MVC test client ───────────────────────────────────────────────
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Test Setup & Teardown
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Clean the database before each test to ensure test isolation.
     * Each test starts with an empty database.
     */
    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helper: Create a task and return the response body
    // ═════════════════════════════════════════════════════════════════════════

    private MvcResult createTask(String title, TaskStatus status, TaskPriority priority) throws Exception {
        TaskCreateDTO dto = new TaskCreateDTO(title, "Integration test description", status, priority);
        return mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andReturn();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FULL CRUD FLOW TESTS
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Full CRUD flow: create → read → update → delete")
    void testFullCrudFlow() throws Exception {
        // 1. CREATE — POST /api/v1/tasks
        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new TaskCreateDTO("Integration Task", "Test desc",
                        TaskStatus.PENDING, TaskPriority.MEDIUM))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Integration Task"))
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        // Extract ID from response
        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. READ — GET /api/v1/tasks/{id}
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Integration Task"));

        // 3. UPDATE — PATCH /api/v1/tasks/{id}
        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new TaskUpdateDTO("Updated Task", null, TaskStatus.COMPLETED, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Task"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 4. DELETE — DELETE /api/v1/tasks/{id}
        mockMvc.perform(delete("/api/v1/tasks/" + taskId))
            .andExpect(status().isNoContent());

        // 5. VERIFY — GET should return 404 after deletion
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should persist task to PostgreSQL and return it with generated ID")
    void shouldPersistToDatabase() throws Exception {
        createTask("Persist Task", TaskStatus.PENDING, TaskPriority.LOW);

        // Verify in the database directly
        assertThat(taskRepository.findAll()).hasSize(1);
        assertThat(taskRepository.findAll().get(0).getTitle()).isEqualTo("Persist Task");
    }

    @Test
    @DisplayName("should return all tasks from database")
    void shouldReturnAllTasksFromDatabase() throws Exception {
        createTask("Task 1", TaskStatus.PENDING, TaskPriority.LOW);
        createTask("Task 2", TaskStatus.IN_PROGRESS, TaskPriority.HIGH);

        mockMvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("should filter tasks by status")
    void shouldFilterByStatus() throws Exception {
        createTask("Task 1", TaskStatus.PENDING, TaskPriority.LOW);
        createTask("Task 2", TaskStatus.COMPLETED, TaskPriority.HIGH);

        mockMvc.perform(get("/api/v1/tasks?status=COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("should search tasks by title keyword")
    void shouldSearchByTitleKeyword() throws Exception {
        createTask("Deploy application", TaskStatus.PENDING, TaskPriority.MEDIUM);
        createTask("Write documentation", TaskStatus.PENDING, TaskPriority.LOW);

        mockMvc.perform(get("/api/v1/tasks?search=deploy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Deploy application"));
    }

    @Test
    @DisplayName("should reject task with title shorter than 3 characters")
    void shouldRejectShortTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new TaskCreateDTO("ab", "desc", TaskStatus.PENDING, TaskPriority.LOW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors").isNotEmpty());
    }

    @Test
    @DisplayName("should return 404 for non-existent task")
    void shouldReturn404ForNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("health endpoint should return 200 OK")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}