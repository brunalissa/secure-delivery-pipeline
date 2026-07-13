# Security Scanning Deep Dive

## Overview

Security is embedded at **every stage** of the delivery pipeline — this is called "shifting left." Instead of scanning for vulnerabilities only in production, we scan at the earliest possible points (source code, dependencies, container image) so problems are caught before they reach customers.

## Pipeline Security Stages

### 1. Secret Scanning — Gitleaks

**What it scans:** The full Git commit history for accidentally committed secrets.

**How it works:** Gitleaks uses regex rules to detect patterns of known secret types:
- AWS access keys (`AKIA...`)
- GitHub tokens (`ghp_...`)
- Private keys (`-----BEGIN RSA PRIVATE KEY-----`)
- Generic high-entropy strings
- Database connection strings with passwords

**When it runs:**
- On every pull request (blocks merge if secrets found)
- On every push to main/develop
- Daily scheduled scan (catches secrets in old commits discovered retroactively)

**Configuration:** Uses default Gitleaks rules. A `.gitleaksignore` file can be added for false positives.

---

### 2. Static Application Security Testing (SAST) — CodeQL

**What it scans:** The Java source code for semantic vulnerability patterns.

**How it works:** CodeQL is GitHub's semantic code analysis engine. It builds a database of your code's data flow and control flow, then runs queries to detect:
- SQL injection (data flows from user input to a SQL query without sanitization)
- Cross-site scripting (XSS)
- Path traversal
- Insecure deserialization
- Hardcoded credentials
- Log injection

**Why CodeQL over simple pattern matching:** CodeQL understands code semantics. It traces data flow from "source" (user input) to "sink" (SQL query), reducing false positives that simple regex scanners would produce.

**When it runs:**
- On push and PR to main/develop
- Weekly scheduled scan (catches newly discovered vulnerability patterns)

---

### 3. Dependency Vulnerability Scanning — OWASP Dependency Check

**What it scans:** All Maven dependencies (including transitive) against the NVD (National Vulnerability Database).

**How it works:**
1. Maven resolves all dependencies (direct + transitive)
2. OWASP Dependency Check hashes each JAR and checks against NVD
3. Generates an HTML report with CVE details
4. **Fails the build** if any dependency has CVSS score ≥ 7.0 (High or Critical)

**Configuration** (`pom.xml`):
```xml
<failBuildOnCVSS>7</failBuildOnCVSS>
```

**When it runs:**
- During the `verify` phase of Maven build
- In the daily security scan workflow

---

### 4. Container Vulnerability Scanning — Trivy

**What it scans:** The built Docker image for:
- OS package vulnerabilities (Alpine packages, Java JARs)
- IaC misconfiguration (Dockerfile, Terraform, K8s YAML)
- Hardcoded secrets in configuration files

**How it works:** Trivy pulls its vulnerability database (maintained by Aqua Security) and scans every layer of the Docker image. It also scans Terraform and Kubernetes YAML for security misconfigurations.

**When it runs:**
- Filesystem scan: scans source code in CI (before Docker build)
- Image scan: scans the built image (after Docker build, before deploy)
- Daily scheduled scan

**Fail condition:** `exit-code: '1'` — fails if HIGH or CRITICAL vulnerabilities found.

---

### 5. Code Quality — Checkstyle & SpotBugs

#### Checkstyle
Enforces the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Catches:
- Naming convention violations
- Import ordering issues
- Missing Javadoc
- Line length violations

#### SpotBugs + FindSecBugs
Detects common Java bug patterns:
- Null pointer dereferences
- SQL injection patterns
- Insecure crypto usage
- HTTP response splitting
- Hardcoded passwords

---

## Kubernetes Security Hardening

| Measure | File | What It Does |
|---|---|---|
| Non-root user | `03-deployment.yaml` | `runAsNonRoot: true`, `runAsUser: 1000` |
| Read-only filesystem | `03-deployment.yaml` | `readOnlyRootFilesystem: true` |
| Drop all capabilities | `03-deployment.yaml` | `capabilities.drop: [ALL]` |
| No privilege escalation | `03-deployment.yaml` | `allowPrivilegeEscalation: false` |
| Seccomp profile | `03-deployment.yaml` | `seccompProfile.type: RuntimeDefault` |
| Resource limits | `03-deployment.yaml` | CPU/memory limits prevent DoS |
| NetworkPolicy | `08-network-policy.yaml` | Only allows traffic from ingress + Prometheus |
| AWS metadata blocked | `08-network-policy.yaml` | Egress blocks `169.254.169.254/32` |

## Terraform Security Best Practices

| Practice | Where |
|---|---|
| Private subnets for EC2 | `modules/vpc/` |
| DB SG accepts only from app SG | `modules/security_groups/` |
| EBS encryption at rest | `modules/ec2/main.tf` |
| IAM least-privilege trust policy | `modules/iam/main.tf` |
| AWS account restriction in trust | `modules/iam/main.tf` |
| Default tags for cost tracking | `main.tf` (provider block) |