# Local Development Guide

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java JDK | 21+ | Compile & run the application |
| Maven | 3.9+ | Build tool & dependency management |
| Docker | 25+ | Container runtime (for Testcontainers & Docker Compose) |
| Docker Compose | 2.24+ | Multi-container orchestration |
| Git | 2.40+ | Version control |
| kubectl | 1.30+ | Kubernetes CLI (for K8s deployment) |
| Terraform | 1.7+ | Infrastructure provisioning |

## Setup Steps

### 1. Clone the Repository

```bash
git clone https://github.com/devsecops/secure-delivery-pipeline.git
cd secure-delivery-pipeline
```

### 2. Run the Application Locally (without Docker)

First, start PostgreSQL:
```bash
docker run -d --name dev-postgres \
  -e POSTGRES_DB=devsecops_db \
  -e POSTGRES_USER=devsecops_user \
  -e POSTGRES_PASSWORD=devsecops_password \
  -p 5432:5432 \
  postgres:15-alpine
```

Then run the Spring Boot app:
```bash
cd app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at: `http://localhost:8080/api/v1/tasks`

### 3. Run with Docker Compose (recommended)

```bash
# Start all services (app + db + prometheus + grafana)
docker compose up -d

# View logs
docker compose logs -f app

# Stop everything
docker compose down

# Stop and remove volumes (destructive)
docker compose down -v
```

### 4. Access the Services

| Service | URL | Credentials |
|---|---|---|
| API | http://localhost:8080/api/v1/tasks | — |
| Actuator Health | http://localhost:8080/actuator/health | — |
| Prometheus Metrics | http://localhost:8080/actuator/prometheus | — |
| Prometheus UI | http://localhost:9090 | — |
| Grafana UI | http://localhost:3000 | admin / admin |

### 5. Test the API

```bash
# Create a task
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"My Task","description":"Test description","status":"PENDING","priority":"HIGH"}'

# Get all tasks
curl http://localhost:8080/api/v1/tasks

# Get task by ID
curl http://localhost:8080/api/v1/tasks/1

# Update a task
curl -X PATCH http://localhost:8080/api/v1/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"COMPLETED"}'

# Delete a task
curl -X DELETE http://localhost:8080/api/v1/tasks/1
```

### 6. Running Tests

```bash
cd app

# Unit tests only (fast)
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify

# Generate JaCoCo coverage report
mvn test jacoco:report
# Open: app/target/site/jacoco/index.html

# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check

# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check
```

### 7. Build the Docker Image Manually

```bash
# Build
docker build -t secure-delivery-pipeline:local .

# Run
docker run -d --name sdp-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/devsecops_db \
  -e DB_USERNAME=devsecops_user \
  -e DB_PASSWORD=devsecops_password \
  secure-delivery-pipeline:local
```