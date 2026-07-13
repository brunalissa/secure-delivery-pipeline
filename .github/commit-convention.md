# Conventional Commits

This project follows the [Conventional Commits](https://www.conventionalcommits.org/) specification.

## Format

```
<type>(optional scope): description

[optional body]

[optional footer(s)]
```

## Types

| Type | Description | Example |
|---|---|---|
| `feat` | A new feature | `feat(api): add task search endpoint` |
| `fix` | A bug fix | `fix(dto): handle null title in update DTO` |
| `docs` | Documentation only changes | `docs: add deployment guide` |
| `style` | Code style changes (formatting, no logic change) | `style: format indentation in pom.xml` |
| `refactor` | Code refactoring (no new features, no bug fixes) | `refactor: extract DTO mapping to helper method` |
| `test` | Adding or modifying tests | `test: add integration test for CRUD flow` |
| `build` | Changes affecting the build system or dependencies | `build: upgrade Spring Boot to 3.2.5` |
| `ci` | CI configuration changes | `ci: add Trivy container scan job` |
| `chore` | Other changes (tooling, config, etc.) | `chore: update .gitignore` |
| `revert` | Reverting a previous commit | `revert: feat(api) add task search endpoint` |

## Scopes

Common scopes used in this project:
- `api` — REST API (controllers, DTOs, services)
- `db` — Database (Flyway migrations, entities)
- `docker` — Dockerfile, docker-compose
- `k8s` — Kubernetes manifests
| `tf` or `terraform` — Terraform
- `ci` — CI/CD workflows
- `monitoring` — Prometheus, Grafana

## Breaking Changes

Breaking changes are indicated by a `!` after the type/scope:

```
feat(api)!: remove deprecated v0 endpoints

BREAKING CHANGE: v0 API endpoints have been removed. Use v1 instead.
```

## Examples

```
feat(api): add PATCH /api/v1/tasks/{id} endpoint
fix(db): prevent Flyway from running in test profile
docs: add Kubernetes deployment guide
test: increase JaCoCo threshold to 80%
ci: add OWASP dependency check to CI pipeline
build: upgrade Java to 21
chore: add CODEOWNERS file
```

## Automated Changelog

Commits following this convention can be automatically converted into changelog entries using:
- [conventional-changelog](https://github.com/conventional-changelog/conventional-changelog)
- [semantic-release](https://github.com/semantic-release/semantic-release)
- [release-please](https://github.com/googleapis/release-please)