package com.devsecops.securepipeline.metrics;

import com.devsecops.securepipeline.model.TaskStatus;
import com.devsecops.securepipeline.repository.TaskRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ───────────────────────────────────────────────────────────────────────────── CustomMetrics —
 * Application-Specific Prometheus Metrics
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Micrometer provides application metrics that Prometheus can scrape from {@code
 * /actuator/prometheus}. These metrics are visualized in Grafana.
 *
 * <p>Spring Boot Actuator + Micrometer auto-exposes many metrics:
 *
 * <ul>
 *   <li>{@code http_server_requests_seconds} — HTTP request latency histogram
 *   <li>{@code jvm_memory_used_bytes} — JVM heap usage
 *   <li>{@code system_cpu_usage} — CPU utilization
 *   <li>{@code process_uptime_seconds} — Application uptime
 * </ul>
 *
 * <p>Below we define CUSTOM metrics specific to our domain:
 *
 * <ul>
 *   <li>{@code tasks_total} — Gauge: total task count
 *   <li>{@code tasks_by_status_total} — Gauge: count per status
 * </ul>
 *
 * <p><b>How to view these metrics:</b>
 *
 * <ul>
 *   <li>Visit {@code http://localhost:8080/actuator/prometheus}
 *   <li>Add the endpoint to Prometheus scrape configuration (see /monitoring)
 *   <li>Dashboard in Grafana (see /monitoring/grafana/dashboards)
 * </ul>
 *
 * <p><b>Why gauge and not counter?</b> A gauge goes up and down as tasks are created/deleted. A
 * counter only ever increases (useful for counting events like HTTP requests).
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomMetrics {

  private final MeterRegistry meterRegistry;
  private final TaskRepository taskRepository;

  /** Register custom gauges after Spring finishes injecting dependencies. */
  @PostConstruct
  public void init() {
    log.info("Registering custom Prometheus metrics");

    // Gauge: total count of tasks in the database
    Gauge.builder("tasks.total", () -> taskRepository.count())
        .description("Total number of tasks")
        .register(meterRegistry);

    // Gauge: count of tasks by each status
    for (TaskStatus status : TaskStatus.values()) {
      Gauge.builder("tasks.by.status", () -> taskRepository.countByStatus(status))
          .tag("status", status.name())
          .description("Number of tasks by status")
          .register(meterRegistry);
    }
  }
}
