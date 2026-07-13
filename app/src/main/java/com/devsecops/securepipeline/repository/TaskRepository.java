package com.devsecops.securepipeline.repository;

import com.devsecops.securepipeline.model.Task;
import com.devsecops.securepipeline.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * TaskRepository — Spring Data JPA Repository
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Spring Data JPA auto-generates the implementation of this interface at
 * runtime. The {@code JpaRepository<Task, Long>} extension provides:
 * <ul>
 *   <li>CRUD operations: save(), findById(), findAll(), deleteById(), etc.</li>
 *   <li>Pagination & Sorting: findAll(Pageable), findAll(Sort)</li>
 *   <li>Query derivation: findByFieldName() — parsed from method name</li>
 * </ul>
 *
 * <p><b>Custom queries:</b> For complex queries, use {@code @Query} with JPQL
 * (default) or native SQL. The {@code @Param} annotation binds named parameters.
 *
 * <p><b>No implementation needed:</b> Spring creates a proxy at startup.
 * This is the core power of Spring Data JPA.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find all tasks by their status.
     *
     * <p>This method is auto-implemented by Spring Data from the method name:
     * "findBy" + "Status" = WHERE status = ?.
     *
     * @param status the TaskStatus to filter by
     * @return list of matching tasks (empty list if none found)
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * Find all tasks by their priority.
     * Auto-implemented: WHERE priority = ?
     *
     * @param priority the TaskPriority to filter by
     * @return list of matching tasks
     */
    List<Task> findByPriority(TaskStatus priority);

    /**
     * Custom JPQL query — find tasks by status, ordered by priority descending.
     *
     * <p>JPQL (Java Persistence Query Language) operates on entities, NOT tables.
     * "Task" refers to the entity class, not the table name.
     *
     * @param status the status to filter by
     * @return ordered list of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.priority DESC")
    List<Task> findByStatusOrderByPriorityDesc(@Param("status") TaskStatus status);

    /**
     * Search tasks by title (case-insensitive, partial match).
     *
     * <p>Demonstrates a JPQL query with LIKE and LOWER functions.
     * Input is sanitized via JPA parameter binding ({@code :keyword}),
     * preventing SQL injection even in hand-written queries.
     *
     * @param keyword the search keyword (will be wrapped with % wildcards)
     * @return list of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchByTitle(@Param("keyword") String keyword);

    /**
     * Count tasks by status — useful for dashboards and metrics.
     *
     * @param status the status to count
     * @return count of matching tasks
     */
    long countByStatus(TaskStatus status);
}