# Config Service Test Plan

Module: `config-service`

Primary purpose: Spring Cloud Config server startup, configuration repository access, and operational health.

## Targets

- `ConfigServiceApplication`
- Config server bootstrap/properties.
- Actuator health endpoint if enabled.

## Tests

- `ConfigServiceApplicationIT`
- `ConfigServiceHealthIT`

## Cases

- Application context starts with a test native/file config repository.
- Config server does not require production paths or secrets in tests.
- `/actuator/health` returns UP when enabled.
- Missing config repository path fails predictably or is documented.
- Invalid config lookup returns the expected status code.

## Integration Strategy

Use `@SpringBootTest` with a test profile and temporary fixture config repository. Never read `.env` or real secret files.

## Testing Techniques

- Equivalence Class Partitioning: existing app config, missing app config, invalid profile.
- Error Guessing: invalid repository path, unavailable filesystem path, malformed properties file.

## Reports

- Surefire: `config-service/target/surefire-reports`
- Failsafe: `config-service/target/failsafe-reports`
- Coverage: `config-service/target/site/jacoco`
- Current command: run `mvn test` or `mvn verify` in `config-service` because this module currently has no Maven wrapper.
- Every config-service test batch must add or update a report under `docs/test-reports/config-service/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state which config repository, profile, actuator, and failure-mode cases were tested.
