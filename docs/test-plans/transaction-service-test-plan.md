# Transaction Service Test Plan

Module: `transaction-service`

Primary purpose: transaction listing/detail/upsert/deactivation, transaction category lookup, taxonomy persistence, and transaction domain events.

## Existing Tests To Remove

- `transaction-service/src/test/java/com/batu/transaction_service/TransactionServiceApplicationTests.java`

## Targets

- `TransactionServiceImpl`
- `PrimaryCategoryServiceImpl`
- `DetailedCategoryServiceImpl`
- `TransactionSyncMapper`
- `TransactionController`
- `TransactionCategoryController`
- `TransactionRepository`
- `PrimaryCategoryRepository`
- `DetailedCategoryRepository`
- Outbox/inbox messaging classes if present in the module.

## Unit Tests

- `TransactionServiceImplTest`
- `PrimaryCategoryServiceImplTest`
- `DetailedCategoryServiceImplTest`
- `TransactionSyncMapperTest`

## Service Cases

- Paginated list without cursor uses initial keyset.
- Paginated list with valid cursor decodes cursor.
- Paginated list with extra row returns next cursor.
- Empty page returns empty data and no next cursor.
- Filters by user, account, primary category, and detailed category.
- Get transaction returns active owned transaction.
- Missing, inactive, or foreign transaction throws not found.
- Upsert active transaction persists and publishes `TransactionRecorded`.
- Upsert inactive transaction persists/removes and publishes `TransactionRemoved`.
- Upsert with unknown detailed category throws not found and publishes nothing.
- Deactivate by account marks active transactions inactive and publishes one removed event per transaction.
- Empty deactivation publishes nothing.

## Category Cases

- Lookup primary category by ID returns DTO.
- Missing primary category throws not found.
- Lookup detailed category by category code returns category with primary category.
- Missing detailed category code throws not found.
- Batch category lookup handles empty, full-hit, and partial-hit sets.
- Get all categories returns stable ordering if the service defines one.

## Web Tests

- `TransactionControllerTest`
- `TransactionCategoryControllerTest`

Cover:

- `GET /transactions` with authentication, filters, cursor, and pagination.
- `GET /transactions/{transactionId}` with valid and invalid UUIDs.
- 401 without JWT.
- Service 404 maps to shared `ProblemDetail`.
- Category endpoints return categories and batch lookup results.
- Invalid request parameters return 400.
- Add or confirm validation for `limit`; if production code has no min/max, document current behavior before changing it.

## Keycloak And Security Tests

- Valid JWT subject UUID is passed to transaction and category service calls where user scoping applies.
- Missing JWT returns 401.
- Malformed subject claim does not call the service.
- User A cannot fetch user B's transaction because service lookup includes authenticated user ID.

## Repository And Integration Tests

- `TransactionRepositoryTest` with PostgreSQL Testcontainers.
- `PrimaryCategoryRepositoryTest`.
- `DetailedCategoryRepositoryTest`.
- `TransactionServiceIT`.
- `TransactionOutboxRelayIT` with RabbitMQ Testcontainers after messaging tooling is added.

Cover:

- Fetch joins load detailed and primary category.
- Custom upsert behavior persists expected fields.
- Dynamic specs for user, account, category, active flag, and date range.
- Flyway taxonomy migration loads required categories.
- Unique category codes and required columns are enforced.
- `TransactionRemoved` outbox event uses `EventTypes.TRANSACTION_REMOVED` and `TRANSACTION_REMOVED_ROUTING_KEY`.
- RabbitMQ topology test proves `TRANSACTION_REMOVED_ROUTING_KEY` reaches every intended consumer. Current shared topology defines `TRANSACTION_REMOVED_QUEUE`; downstream service plans must either consume it or document why it is unused.
- Outbox relay publish failure leaves transaction events retryable.
- Concurrent relay workers do not publish the same transaction event twice if the implementation intends single delivery.

## Testing Techniques

- Equivalence Class Partitioning: active/inactive, owned/foreign, known/unknown category.
- Boundary Value Analysis: page limits and date edges.
- Pairwise Testing: category filter, account filter, cursor, direction.
- State Transition Testing: active transaction to inactive transaction.
- Race Condition Testing: concurrent deactivation and upsert for the same transaction, concurrent outbox relay workers.
- Error Guessing: invalid cursor, category FK mismatch, failed event publish dependency, outbox relay publish failure and retry.

## Reports

- Surefire: `transaction-service/target/surefire-reports`
- Failsafe: `transaction-service/target/failsafe-reports`
- Coverage: `transaction-service/target/site/jacoco`
- Every transaction-service test batch must add or update a report under `docs/test-reports/transaction-service/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state whether `TransactionRecorded`, `TransactionRemoved`, outbox relay, topology, and downstream-consumer behavior were tested.
