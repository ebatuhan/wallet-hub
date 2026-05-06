# Shared Module Unit Test Report

Module: `shared`

Branch: `test/shared-module`

Date: 2026-05-06

## Test Scope

This batch covers shared-module unit tests only. No integration tests were added because the tested shared behaviors are pure utility, converter, error-mapping, and constant-contract logic with no database, broker, HTTP server, or external-service dependency.

## Test Type

- Unit tests with JUnit 5 and AssertJ.
- Test classes are intentionally flat rather than `@Nested` so Surefire `.txt` reports show real per-class test counts instead of confusing outer containers with `Tests run: 0`.
- No Spring application context was started.
- No Testcontainers were used.
- No RabbitMQ, PostgreSQL, ClickHouse, Keycloak server, Plaid, or AI service was started.

## Classes Under Test

- `com.batu.shared.cursor.CursorUtils`
- `com.batu.shared.error.CommonApplicationErrorAdvice`
- `com.batu.shared.security.KeycloakRoleConverter`
- `com.batu.shared.security.KeycloakScopeConverter`
- `com.batu.shared.messaging.MessagingTopology`
- `com.batu.shared.messaging.EventTypes`

## Test Files

- `shared/src/test/java/com/batu/shared/unit/cursor/CursorUtilsTest.java`
- `shared/src/test/java/com/batu/shared/unit/error/CommonApplicationErrorAdviceTest.java`
- `shared/src/test/java/com/batu/shared/unit/security/KeycloakRoleConverterTest.java`
- `shared/src/test/java/com/batu/shared/unit/security/KeycloakScopeConverterTest.java`
- `shared/src/test/java/com/batu/shared/unit/messaging/MessagingTopologyTest.java`

## Cases Covered

### CursorUtils

- `decode` returns an initial keyset position for `null` cursor.
- `decode` returns an initial keyset position for blank cursor.
- `decode` converts valid Base64 JSON keys into typed UUID, `Instant`, and `LocalDateTime` values.
- `decode` rejects malformed Base64 cursor with `InvalidCursorException`.
- `decode` rejects malformed JSON cursor with `InvalidCursorException`.
- `encode` returns `null` for initial keyset position.
- `encode` returns a round-trippable cursor for non-initial keyset position.

### CommonApplicationErrorAdvice

- `ResponseStatusException` maps to `ProblemDetail` with status, detail, and message property.
- `MethodArgumentNotValidException` maps field errors into a stable `errors` map.
- `IllegalArgumentException` maps to 400 `ProblemDetail`.
- `DataAccessException` maps to generic 500 `ProblemDetail` and hides database details.

### KeycloakRoleConverter

- Extracts realm roles as `REALM_`, raw role, and `ROLE_` authorities.
- Extracts client/resource roles as raw role and `ROLE_` authorities.
- Extracts groups with and without leading `/`.
- Returns empty authorities when role claims are absent.
- Ignores malformed role and group claims instead of throwing.

### KeycloakScopeConverter

- Extracts space-separated `scope` string claim.
- Ignores extra spaces in string scopes.
- Extracts collection-based `scp` claim.
- Ignores blank values in collection-based scopes.
- Returns empty authorities when scope claims are absent.

### MessagingTopology And EventTypes

- Account event queues, routing keys, aliases, and event type names remain stable.
- Transaction event queues, routing keys, aliases, and event type names remain stable.

## Testing Techniques Used

- Equivalence Class Partitioning: valid cursor, missing cursor, malformed cursor, valid claims, absent claims, malformed claims.
- Boundary Value Analysis: blank cursor, empty/missing claim sets, extra-space scopes.
- Error Guessing: malformed Base64, malformed JSON, malformed Keycloak claim shapes, database exception detail leakage.
- Contract Testing: messaging topology and event type constant values are locked by tests.

## External Systems Mocked Or Containerized

None.

## Libraries And Tools Used

- Spring Boot `spring-boot-starter-test`
- JUnit 5
- AssertJ
- Spring test support classes for validation/error-advice testing
- No JaCoCo yet
- No Failsafe yet
- No Testcontainers yet
- No Allure or other rich reporting library yet
- GitHub Actions workflow for CI execution

## Command Run

```bash
JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64" mvn clean verify
```

CI-equivalent command:

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

## Result

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Report Paths

- Surefire reports: `shared/target/surefire-reports`
- CI artifact: `shared-surefire-reports`, uploaded by `.github/workflows/shared-tests.yml`
- Failsafe reports: not applicable; no `*IT` tests in this batch.
- Coverage report: not generated; JaCoCo is not configured yet.

## CI Readiness

Status: CI-ready for this module.

- Workflow: `.github/workflows/shared-tests.yml`
- Trigger: pull requests and pushes touching `shared/**`, shared reports/plans, or the workflow file.
- Java: pinned to Java 21 through `actions/setup-java@v4`.
- Command: `mvn --batch-mode --no-transfer-progress clean verify`.
- Artifacts: `shared/target/surefire-reports` uploaded on every run.

## Known Gaps

- No integration tests were added for `shared` because current shared targets do not require external infrastructure.
- No JaCoCo coverage report exists yet.
- No Allure or rich HTML test report exists yet.
- Messaging topology tests lock constants, but actual RabbitMQ bindings/listeners must be tested in consuming services.
- Keycloak converter tests use synthetic JWTs, not a live Keycloak server. Live issuer/JWK behavior belongs in service or gateway security integration tests.

## Next Recommended Tests

- Move to `budgeting` and establish the first full service pattern: service unit tests, web slice tests, PostgreSQL repository tests, and inbox/concurrency tests.
- Add shared test reporting/coverage tooling once the team decides between per-module plugin configuration and a root Maven aggregator/parent.
