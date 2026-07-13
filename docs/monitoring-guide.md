# Monitoring & Metrics Guide

## Overview

The project includes a full observability stack:

- **Spring Boot Actuator** — exposes health, metrics, and info endpoints
- **Micrometer** — application metrics facade (assesses Spring Boot → Prometheus format)
- **Prometheus** — time-series database that scrapes metrics
- **Grafana** — visualization dashboard
- **Alerting** — Prometheus alert rules → Alertmanager (email/Slack/PagerDuty)

## Architecture

```
Spring Boot App
  └─ Actuator
     ├─ /actuator/health         → K8s liveness/readiness probes
     ├─ /actuator/prometheus     → Prometheus scrape target
     └─ /actuator/metrics        → Human-readable metrics listing
        │
        ▼
   Prometheus (scrapes every 15s)
     ├─ Alert rules (evaluated every 15s)
     │  └─ Alertmanager → email/Slack/PagerDuty
     │
     ▼
   Grafana (queries Prometheus)
     └─ Provisioned dashboard (7 panels)
```

## Available Metrics

### HTTP Metrics (auto-exposed)

| Metric | Type | Description |
|---|---|---|
| `http_server_requests_seconds_count` | Counter | HTTP request count by method, URI, status |
| `http_server_requests_seconds_sum` | Counter | Sum of HTTP request durations |
| `http_server_requests_seconds_max` | Gauge | Max HTTP request duration |
| `http_server_requests_seconds_bucket` | Histogram | Duration histogram (for P50, P95, P99) |

### JVM Metrics (auto-exposed)

| Metric | Type | Description |
|---|---|---|
| `jvm_memory_used_bytes{area="heap"}` | Gauge | Heap memory used (bytes) |
| `jvm_memory_max_bytes{area="heap"}` | Gauge | Max heap (bytes) |
| `jvm_threads_live_threads` | Gauge | Number of live JVM threads |
| `jvm_gc_pause_seconds` | Histogram | Garbage collection pause times |
| `process_cpu_usage` | Gauge | JVM process CPU usage (0.0 - 1.0) |
| `process_uptime_seconds` | Gauge | Application uptime (seconds) |

### System Metrics (auto-exposed)

| Metric | Type | Description |
|---|---|---|
| `system_cpu_usage` | Gauge | System-level CPU usage |
| `system_load_average_1m` | Gauge | System load average (1 minute) |
| `disk_free_bytes` | Gauge | Free disk space |
| `disk_total_bytes` | Gauge | Total disk space |

### Custom Application Metrics

| Metric | Type | Description |
|---|---|---|
| `tasks_total` | Gauge | Total number of tasks in the database |
| `tasks_by_status_total{status="PENDING"}` | Gauge | Number of tasks by each status |

## Querying Metrics with PromQL

### CPU Usage (last 5 min average)
```promql
avg_over_time(process_cpu_usage[5m]) * 100
```

### HTTP Error Rate (5xx)
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

### P95 Response Latency
```promql
histogram_quantile(0.95, sum by (le) (
  rate(http_server_requests_seconds_bucket[5m])
))
```

### Heap Memory Usage %
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Tasks by Status Distribution
```promql
tasks_by_status_total
```

## Accessing the Observability Stack

### Start the stack (Docker Compose)
```bash
docker compose up -d
```

### URLs

| Service | URL | Credentials |
|---|---|---|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Actuator Health | http://localhost:8080/actuator/health | — |
| Raw Prometheus Metrics | http://localhost:8080/actuator/prometheus | — |

### Grafana Dashboard

The pre-provisioned dashboard ("Secure Delivery Pipeline — Application Metrics") includes 8 panels:

1. HTTP Request Rate (req/s) — stat panel
2. HTTP Status Codes — stat panel
3. HTTP Response Latency (P50 + P95) — timeseries
4. JVM Heap Memory Usage — timeseries
5. JVM CPU Usage — gauge
6. Tasks by Status — pie chart
7. Total Tasks — stat
8. JVM Uptime — stat

The dashboard auto-refreshes every 10 seconds.