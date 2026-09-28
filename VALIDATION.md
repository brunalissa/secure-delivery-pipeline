# Fix application and delivery validation

The application failed compilation without Springdoc, controller tests used the wrong security configuration, and integration tests were not bound to the Maven lifecycle. This change restores those paths and fixes container/database and Kubernetes startup configuration.

- Add Springdoc, correct MockBean imports, test the actual demo security configuration and start the full application context in the smoke test.
- Bind PostgreSQL Testcontainers integration tests to Failsafe, using the same container for datasource properties.
- Enforce the original 80% line coverage gate at verify, so Docker packaging can skip tests without a premature coverage failure.
- Apply Google Java formatting and naming; fix nullable type handling, defensively copy validation errors and stop logging user input.
- Keep SpotBugs enabled, with narrow documented exclusions for informational endpoint discovery, injected singleton references and the stateless demo's CSRF policy. Authentication remains a production prerequisite.
- Upgrade OWASP Dependency-Check for current NVD API compatibility. CI runs its mandatory security gate separately, with optional NVD_API_KEY configuration and a bounded timeout.
- Use a Maven builder image (the previous plain JDK image had no mvn executable), then verify startup with Compose.
- Honor Compose database environment variables, forward termination signals to Java, provide a writable Kubernetes temporary directory and create the database Secret explicitly during deployment.
- Match published image SHA tags to deployment and scan references; validate Docker builds on the fix branch without deploying cloud resources.

Validation: 32 local unit/web/context tests passed; coverage and Checkstyle passed. SpotBugs passed with the documented exclusions. GitHub PostgreSQL integration tests passed. Docker Compose startup and security results are being checked on this draft PR. Local Docker is unavailable, so integration checks run in GitHub Actions. No AWS deployment or Terraform apply was performed.

OWASP upgrade rationale: https://github.com/dependency-check/DependencyCheck#mandatory-upgrade-to-1210
