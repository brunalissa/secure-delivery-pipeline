# CI/CD Pipeline Explanation

## Overview

The project includes 4 GitHub Actions workflows:

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| **CI/CD Pipeline** | `ci.yml` | push, PR | Build, test, quality, security, publish, deploy |
| **Security Scanning** | `security.yml` | daily 2AM, PR, manual | Dedicated vulnerability scanning |
| **Terraform** | `terraform.yml` | push to `terraform/**`, PR | IaC validation, plan, apply |
| **CodeQL SAST** | `codeql.yml` | push, PR, weekly Monday | Semantic code analysis |

## CI/CD Pipeline (ci.yml) — 6 Jobs

```
┌─────────────────┐
│  Build & Test   │  ← Maven compile + unit tests + integration tests (Testcontainers)
└────────┬────────┘
         │ (needs: build-and-test)
         ▼
┌─────────────────┐
│  Code Quality   │  ← SonarQube + Checkstyle + SpotBugs
└────────┬────────┘
         │
   ┌─────┴─────┐
   ▼           ▼
┌────────┐ ┌──────────┐
│Security│ │  Docker  │
│  Scan  │ │ Publish  │  ← Build image → push to GHCR
└────────┘ └────┬─────┘
                │
                ▼
         ┌──────────────┐
         │Container Scan│  ← Trivy image vulnerability scan
         └──────┬───────┘
                │
                ▼
         ┌──────────────┐
         │   Deploy     │  ← kubectl apply to K8s (main branch only)
         └──────────────┘
```

## Security Scanning (security.yml)

Runs daily at 2 AM UTC to catch newly disclosed vulnerabilities:

1. **Gitleaks** — scans full Git history for committed secrets (API keys, passwords)
2. **Trivy Filesystem** — scans source code + IaC for vulnerabilities and misconfigurations
3. **OWASP Dependency Check** — scans Maven dependencies against NVD database

Results are uploaded as SARIF files to the GitHub Security tab.

## Terraform Workflow (terraform.yml)

| Job | Trigger | What |
|---|---|---|
| Validate | push + PR | `fmt`, `init`, `validate`, tflint, Trivy IaC scan |
| Plan | PR only | `terraform plan` → comment on PR |
| Apply | push to main | `terraform apply -auto-approve` |

Uses OIDC (OpenID Connect) for keyless AWS authentication — no long-lived secrets.

## CodeQL Workflow (codeql.yml)

GitHub's semantic analysis engine that understands Java code flow. Detects:
- SQL injection
- XSS
- Path traversal
- Deserialization vulnerabilities
- And more...