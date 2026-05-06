# Wallet Hub Testing Standard

This standard applies to every backend module when we rebuild the test suite. It follows Dr JSkill Spring Boot testing guidance from `/home/batu/.config/opencode/skills/dr-jskill/references/TEST.md` and current Spring Boot 4 testing documentation checked with Context7.

## Scope

We will first remove the existing inconsistent tests, then rebuild tests service by service with one naming, structure, reporting, and quality pattern.

Modules covered:

- `shared`
- `account-service`
- `transaction-service`
- `budgeting`
- `plaid-adapter-service`
- `insights-service`
- `dashboard-service`
- `ai-assistant`
- `api-gateway`
- `config-service`

## Test Types

- Unit tests isolate one class with JUnit 5, Mockito, and AssertJ.
- Web slice tests use `@WebMvcTest`, `MockMvc`, Spring Security test support, and `@MockitoBean` instead of deprecated `@MockBean`.
- Repository tests use `@DataJpaTest` with PostgreSQL Testcontainers where the module uses JPA.
- Full integration tests use `@SpringBootTest`, Testcontainers, and `@ServiceConnection` where supported.
- Messaging tests cover inbox idempotency, outbox relay behavior, listener success, duplicate handling, and failure behavior.
- Messaging race tests cover concurrent duplicate deliveries, not only sequential duplicates. Current inbox implementations check for an event, run the handler, then insert the inbox row; tests must prove whether two simultaneous deliveries can run the handler twice.
- External-client tests use WireMock or a mocked wrapper, never real Plaid, Ollama, Keycloak, or downstream services.

## Naming

- Unit tests: `ClassNameTest`
- MVC slice tests: `ClassNameControllerTest` or `ClassNameWebMvcTest`; use one style per module.
- Repository tests: `ClassNameRepositoryTest`
- Integration tests: `ClassNameIT`
- Application smoke tests: `ServiceApplicationIT`
- Test methods: `methodName_whenCondition_shouldExpectedOutcome`

## Package Layout

Use this structure inside each module:

```text
src/test/java/com/batu/<service>/
  unit/
    service/
    mapper/
    factory/
    strategy/
    util/
  web/
  repository/
  integration/
  support/
```

## Test Style

- Use Given/When/Then sections in non-trivial tests.
- Use `@Nested` only when the module's generated reports remain readable. If Surefire `.txt` reports show confusing outer classes with `Tests run: 0`, prefer flat test classes with clear method names for CI and manual report readability.
- Use `@ParameterizedTest` for boundary values, equivalence classes, and repeated validation cases.
- Use deterministic UUIDs, dates, currencies, and amounts.
- Avoid asserting whole JSON strings; assert JSON paths and critical contract fields.
- Avoid private method tests; test behavior through public APIs.
- Verify emitted events and downstream calls when those are part of the behavior.

## Testing Techniques

- Equivalence Class Partitioning: owned vs foreign resource, active vs inactive, valid vs invalid cursor, present vs missing dependency response.
- Boundary Value Analysis: limits `1`, `100`, `0`, `101`; AI prompt length `1`, `4000`, `4001`; date range thresholds; adjacent budget periods.
- Decision Table Testing: budget transaction application, Plaid connection removal, AI conversation status handling.
- State Transition Testing: AI conversation lifecycle, Plaid connection lifecycle, budget active/inactive state, outbox/inbox processing.
- Pairwise Testing: filter combinations, sort direction, cursor presence, optional date ranges.
- Error Guessing: malformed UUIDs, invalid enums, malformed cursor, duplicate data, null Feign body, external timeout, database unique violation.

## Failure Modes We Must Test

- RabbitMQ duplicate delivery: same event delivered sequentially and concurrently.
- RabbitMQ retry behavior: handler throws before inbox insert, then a redelivery succeeds.
- RabbitMQ topology drift: every routing key in `MessagingTopology` that should be consumed has a queue, binding, listener, and projection cleanup test.
- Race conditions: pessimistic locks, duplicate event processing, stale AI conversation processing, concurrent Plaid sync, concurrent budget updates.
- Keycloak/JWT integration: unauthenticated request, valid JWT, missing subject, malformed UUID subject, missing scope or role, wrong tenant/user, and internal endpoint policy.
- External-service failure: Plaid, Feign downstream services, Ollama/Spring AI, Config Server, ClickHouse, PostgreSQL, and RabbitMQ unavailable or timing out.
- Persistence constraints: unique keys, required columns, numeric precision, soft-delete filtering, transaction rollback, and outbox records in the same transaction.

## Reporting And Tooling

Required later in each module or a future shared parent:

- `maven-surefire-plugin` for `*Test` reports in `target/surefire-reports`.
- `maven-failsafe-plugin` explicitly declared so `*IT` tests run during `verify`.
- `jacoco-maven-plugin` for coverage HTML and XML reports.
- Testcontainers dependencies for PostgreSQL, RabbitMQ, and ClickHouse where needed.

Optional later:

- Allure for richer business-readable test reports.
- Spring REST Docs if API documentation becomes a deliverable.
- PIT mutation testing for core business services after baseline coverage is stable.

## CI Readiness

Every module test batch should be CI-ready before it is considered complete.

Minimum CI requirements:

- GitHub Actions workflow or existing CI job runs the same command recorded in the test report.
- Java version is pinned explicitly. For this project, use Java 21 for modules declaring `<java.version>21</java.version>`.
- Maven runs in non-interactive mode with `--batch-mode --no-transfer-progress`.
- Surefire reports are uploaded as CI artifacts.
- Failsafe reports are uploaded once `*IT` integration tests are added.
- JaCoCo coverage artifacts are uploaded only after JaCoCo is configured.
- CI must fail on test failures and compile failures.
- CI must not require `.env` or real secrets.
- External services must be mocked, disabled, or containerized.

Current CI status by module:

- `shared`: CI-ready via `.github/workflows/shared-tests.yml`.
- Other modules: not CI-ready yet under this testing standard; add module workflows or a shared matrix workflow as their tests are implemented.

## Commands

Current command reality:

- Modules with `mvnw`: `account-service`, `transaction-service`, `budgeting`, `plaid-adapter-service`, `insights-service`, `dashboard-service`, `ai-assistant`, `api-gateway`.
- Modules without `mvnw`: `shared`, `config-service`; use system `mvn` there until wrappers or an aggregator are added.
- JaCoCo, Failsafe, and Testcontainers are planned tooling. `jacoco:report` and `*IT` execution are only valid after the relevant plugin/dependencies are added.

Current per-module commands before tooling is added:

```bash
./mvnw test
./mvnw verify
```

For `shared` and `config-service` before wrappers are added:

```bash
mvn test
mvn verify
```

Future per-module commands after tooling is added:

```bash
./mvnw test
./mvnw verify
./mvnw jacoco:report
```

After Failsafe is configured, `./mvnw verify` must show integration tests being executed. A build success that silently skips `*IT` files is not acceptable.

## Existing Tests To Remove First

- `account-service/src/test/java/com/batu/account_service/AccountServiceApplicationTests.java`
- `transaction-service/src/test/java/com/batu/transaction_service/TransactionServiceApplicationTests.java`
- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/TestSupportConfiguration.java`
- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/unit/**`
- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/integration/**`

## Report Template

Each service must maintain a test report after every test implementation batch. Reports live under `docs/test-reports/<module>/` and must be updated in the same branch as the tests.

Report file naming:

```text
YYYY-MM-DD-<module>-<scope>.md
```

Examples:

- `2026-05-06-shared-unit-tests.md`
- `2026-05-07-budgeting-service-tests.md`
- `2026-05-08-account-webmvc-tests.md`

Each report must include:

```text
Module:
Branch:
Date:
Test scope:
Test type:
Classes under test:
Test files:
Cases covered:
Testing techniques used:
External systems mocked or containerized:
Libraries/tools used:
Command run:
Result:
Surefire/Failsafe report path:
Coverage path:
Known gaps:
Next recommended tests:
```

Do not claim coverage reports exist unless JaCoCo or another coverage tool is configured and the report was generated.
