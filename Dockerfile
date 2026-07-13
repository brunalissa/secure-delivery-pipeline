# ═══════════════════════════════════════════════════════════════════════════════
# Dockerfile — Multi-Stage Build for Secure Delivery Pipeline
# ═══════════════════════════════════════════════════════════════════════════════
#
# This Dockerfile uses a MULTI-STAGE build to create a minimal, production-ready
# container image. Multi-stage builds:
#   1. Use a full build image (with Maven + JDK) to compile the application
#   2. Copy ONLY the resulting artifact to a minimal runtime image
#   3. Discard all build tools, source code, and intermediate artifacts
#
# The result: the final image is ~200MB instead of ~800MB+.
#
# ─────────────────────────────────────────────────────────────────────────────
# SECURITY NOTE: We use a non-root user in the runtime image. Running containers
# as root is a common security anti-pattern. If an attacker escapes the container,
# they have root access. With a non-root user, the blast radius is limited.
# ─────────────────────────────────────────────────────────────────────────────

# ════════════════════════════════════════════════════════════════
# STAGE 1: Builder — Compile the Java application
# ════════════════════════════════════════════════════════════════
# Uses the Eclipse Temurin image (OpenJDK distro) with Maven pre-installed.
# We chose the eclipse-temurin image because it's maintained by the Adoptium
# community and is the de-facto standard for Java container builds.
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory inside the builder container
WORKDIR /build

# Copy only the Maven POM file first — this enables Docker layer caching.
# If pom.xml hasn't changed, Docker reuses the cached layer with dependencies
# already downloaded. This saves ~30-60 seconds on subsequent builds.
COPY app/pom.xml .

# Download all Maven dependencies (go-offline).
# This is a separate layer so that dependency downloads are cached independently.
# The `-B` flag runs in batch mode (no colored output, good for CI logs).
RUN mvn dependency:go-offline -B

# Now copy the source code. This layer changes frequently (every code change),
# but because dependencies are cached above, this layer is the only one rebuilt.
COPY app/src ./src

# Build the application JAR.
# -DskipTests: skip test execution in the Docker build (tests run in CI pipeline)
# The resulting JAR is an executable "fat JAR" containing all dependencies.
RUN mvn clean package -DskipTests -B

# ════════════════════════════════════════════════════════════════
# STAGE 2: Runtime — Minimal production image
# ════════════════════════════════════════════════════════════════
# Use a JRE-only image (no JDK). JRE is sufficient for running compiled Java.
# The "alpine" variant uses Alpine Linux — a minimal OS (~5MB).
# Total image size with JRE-alpine: ~180MB vs ~400MB for JDK Ubuntu.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl — needed for the HEALTHCHECK command below.
# Alpine uses apk (Alpine Package Keeper) instead of apt-get.
RUN apk add --no-cache curl

# Create a non-root user and group for security.
# The application will run as this user, not as root.
# -D = create home directory, -h = set home path
RUN addgroup -S appgroup && adduser -S appuser -G appgroup -h /app

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage.
# The --from=builder flag copies from the previous stage.
# We rename it to app.jar for consistency.
COPY --from=builder /build/target/*.jar /app/app.jar

# Change ownership of the application files to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user — ALL subsequent commands run as this user
USER appuser

# Expose port 8080 — documentation only.
# The container still needs -p 8080:8080 at runtime to actually map the port.
EXPOSE 8080

# ─────────────────────────────────────────────────────────────────────────────
# HEALTHCHECK — Docker-native health monitoring
# ─────────────────────────────────────────────────────────────────────────────
# Docker will periodically run this command to check if the container is healthy.
# If it fails 3 consecutive times, Docker marks the container as "unhealthy".
# In Docker Compose / Swarm, unhealthy containers can be automatically restarted.
#
# Parameters:
#   --interval=30s     : Check every 30 seconds
#   --timeout=10s      : Fail if the check takes more than 10 seconds
#   --start-period=40s : Grace period after container start (app needs time to boot)
#   --retries=3        : Mark unhealthy after 3 consecutive failures
#
# The health check hits Spring Boot Actuator's /actuator/health endpoint.
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ─────────────────────────────────────────────────────────────────────────────
# JVM Tuning for Containers
# ─────────────────────────────────────────────────────────────────────────────
# JAVA_OPTS are environment variables that configure the JVM.
# -XX:MaxRAMPercentage=75.0 : Limit heap to 75% of container memory limit
#                              (prevents OOM kills in K8s with resource limits)
# -Djava.security.egd=file:/dev/./urandom : Speed up Spring Boot startup
#                                           (avoids entropy pool blocking)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# ─────────────────────────────────────────────────────────────────────────────
# ENTRYPOINT — The command that starts the application
# ─────────────────────────────────────────────────────────────────────────────
# Using ENTRYPOINT (not CMD) because this is a purpose-built image that should
# always run the Java application. CMD is for generic images where the command
# might be overridden (e.g., ubuntu with a default bash shell).
#
# The shell form with $JAVA_OPTS is used because exec form doesn't expand
# environment variables. This is a known trade-off — PID 1 will be /bin/sh,
# but Spring Boot handles SIGTERM correctly for graceful shutdown.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]