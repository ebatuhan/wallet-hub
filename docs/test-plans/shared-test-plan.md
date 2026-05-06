# Shared Module Test Plan

Module: `shared`

Primary purpose: reusable DTOs, cursor utilities, security converters, messaging contracts, and common error handling.

## Targets

- `shared/src/main/java/com/batu/shared/util/CursorUtils.java`
- `shared/src/main/java/com/batu/shared/error/CommonApplicationErrorAdvice.java`
- `shared/src/main/java/com/batu/shared/security/KeycloakRoleConverter.java`
- `shared/src/main/java/com/batu/shared/security/KeycloakScopeConverter.java`
- `shared/src/main/java/com/batu/shared/messaging/**`

## Unit Tests

- `CursorUtilsTest`
- `CommonApplicationErrorAdviceTest`
- `KeycloakRoleConverterTest`
- `KeycloakScopeConverterTest`

## Cases

- Cursor encoding returns `null` for initial or empty position.
- Cursor encoding creates stable Base64 JSON for non-initial positions.
- Cursor decoding accepts valid cursors with UUID, `Instant`, and date/time values.
- Cursor decoding rejects malformed Base64 and malformed JSON with `InvalidCursorException`.
- Error advice maps `ResponseStatusException` to `ProblemDetail` with status and detail.
- Error advice maps validation failures to a stable `errors` object.
- Error advice hides internal `DataAccessException` details.
- Error advice maps `IllegalArgumentException` to 400.
- Keycloak role converter extracts realm roles and client roles.
- Keycloak role converter handles missing, empty, and malformed role claims.
- Scope converter extracts space-separated scopes.
- Scope converter handles blank and missing scope claims.
- Messaging topology constants include queues and routing keys for every domain event that producers emit.
- Messaging topology tests assert `TRANSACTION_REMOVED_QUEUE` and `TRANSACTION_REMOVED_ROUTING_KEY` are deliberately paired with an intended consumer plan.

## Testing Techniques

- Equivalence Class Partitioning: valid cursor, malformed cursor, empty cursor.
- Boundary Value Analysis: empty values, single claim, multiple claims.
- Error Guessing: invalid Base64, wrong claim shape, missing validation fields.

## Integration Tests

No database integration is needed for this module. Add a lightweight serialization compatibility test for messaging DTOs only if event contracts change.

## Reporting

- Current command: run `mvn test` in `shared` because this module currently has no Maven wrapper.
- Publish Surefire and JaCoCo reports after tooling is added.
- Every shared test batch must add or update a report under `docs/test-reports/shared/` using the standard template from `00-testing-standard.md`.
- Current implemented shared tests are unit tests only; if no integration tests are added, the report must explicitly say integration tests were not needed for that batch.
- Shared-module tests are CI-ready through `.github/workflows/shared-tests.yml`.
- The CI workflow pins Java 21, runs `mvn --batch-mode --no-transfer-progress clean verify`, and uploads `shared/target/surefire-reports` as an artifact.
