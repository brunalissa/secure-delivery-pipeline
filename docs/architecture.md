# Architecture Overview

## High-Level Architecture

The Secure Delivery Pipeline project follows a layered architecture with clear separation between:

1. **Application Layer** — Spring Boot REST API (Java 21)
2. **Infrastructure Layer** — Docker, Kubernetes, Terraform (AWS)
3. **Observability Layer** — Prometheus, Grafana, Actuator
4. **Pipeline Layer** — GitHub Actions CI/CD workflows

## Application Architecture

```
┌──────────────────────────────────────────────────────────────────────────────────┐                                                                           │
│                          Spring Boot Application                               │
│                                                                                │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐  │
│  │ Controller│ → │  Service     │ → │Repository│ → │ JPA/Hiber │ → │PostgreSQL│  │
│  │  (Web)   │   │ (Business)   │   │ (Data)   │   │   nate    │   │          │  │
│  └────┬─────┘   └──────┬───────┘   └──────────┘   └──────────┘   └──────────┘  │
│       │                 │                                                        │
│       │                 │                                                        │
│  ┌────▼─────┐   ┌───────▼────────┐                                              │
│  │ @Valid   │   │ @Transactional │     ┌───────────────┐                        │
│  │ Bean Val │   │ DB Transaction  │     │ Actuator      │                        │
│  └──────────┘   └────────────────┘     │ /health        │                        │
│                                        │ /prometheus    │                        │
│                                        └───────────────┘                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

## Request Flow

1. **Client** sends HTTP request to `POST /api/v1/tasks`
2. **Controller** (`TaskController`) receives the request, validates input with `@Valid`
3. **Service** (`TaskService`) applies business logic, manages transactions
4. **Repository** (`TaskRepository`) interacts with PostgreSQL via JPA/Hibernate
5. **Response** is serialized to JSON and returned as `TaskResponseDTO`

## Exception Handling

All exceptions are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status | When |
|---|---|---|
| `ResourceNotFoundException` | 404 | Resource not in database |
| `MethodArgumentNotValidException` | 400 | Bean Validation fails |
| `MethodArgumentTypeMismatchException` | 400 | Invalid path variable type |
| `IllegalArgumentException` | 400 | Invalid argument |
| `Exception` (fallback) | 500 | Unhandled runtime error |

## Multistage Build Pipeline

```
Source Code → Maven Build → JAR Artifact → Docker Image → GHCR Push → K8s Deploy
     → Unit Tests → Integration Tests → Quality Gates → Security Scans
```

## Security Layers

1. **Code level**: Bean Validation, input sanitization, no raw exception leaks
2. **Dependency level**: OWASP Dependency Check (NVD CVE scanning)
3. **Source level**: Gitleaks (secret scanning of Git history)
4. **Container level**: Trivy (image vulnerability scanning)
5. **SAST level**: GitHub CodeQL (semantic code analysis)
6. **Infrastructure level**: Security Groups (DB only accepts traffic from app SG)
7. **Kubernetes level**: NetworkPolicy (micro-segmentation), non-root containers, read-only filesystem