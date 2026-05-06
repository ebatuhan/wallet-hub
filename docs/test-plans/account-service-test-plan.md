# Account Service Test Plan

Module: `account-service`

Primary purpose: account listing, account detail, summaries, account upsert/deactivation, and account domain events.

## Existing Tests To Remove

- `account-service/src/test/java/com/batu/account_service/AccountServiceApplicationTests.java`

## Targets

- `AccountServiceImpl`
- `AccountController`
- `AccountInternalController`
- `AccountRepository`
- `AccountRepositoryImpl`
- `OutboxDomainEventPublisher`
- `ScheduledOutboxRelay`

## Unit Tests

- `AccountServiceImplTest`
- `OutboxDomainEventPublisherTest`
- `ScheduledOutboxRelayTest`

## Service Cases

- `getAccount_whenOwnedAndActive_shouldReturnAccount`
- `getAccount_whenMissing_shouldThrowNotFound`
- `getAccount_whenInactive_shouldThrowNotFound`
- `getAccount_whenOwnedByDifferentUser_shouldThrowNotFound`
- `getAccountSummary_whenNoAccounts_shouldReturnZeroTotals`
- `getAccountSummary_whenMultipleCurrencies_shouldGroupTotalsByCurrency`
- `getAccountsViewPaginated_whenCursorMissing_shouldUseInitialKeyset`
- `getAccountsViewPaginated_whenCursorProvided_shouldDecodeCursor`
- `getAccountsViewPaginated_whenMoreRowsExist_shouldReturnNextCursor`
- `getAccountsViewPaginated_whenSortFieldMissing_shouldUseDefaultSort`
- `upsertAccount_whenValid_shouldPersistAndPublishAccountRecorded`
- `upsertAccount_whenRepositoryFails_shouldNotPublishEvent`
- `deactivateAccountsByConnection_whenAccountsExist_shouldDeactivateAndPublishRemovedEvents`
- `deactivateAccountsByConnection_whenNoAccounts_shouldPublishNothing`

## Web Tests

- `AccountControllerTest`
- `AccountInternalControllerTest`

Cover:

- `GET /accounts/{accountId}` returns 200 for authenticated owner.
- `GET /accounts/{accountId}` returns 400 for invalid UUID.
- `GET /accounts/{accountId}` returns 401 without JWT.
- `GET /accounts/summary` returns currency totals.
- `POST /accounts/batch` validates body and returns names.
- `GET /accounts` validates `limit=0`, `limit=101`, invalid sort field, invalid direction, malformed cursor.
- Internal upsert and deactivate endpoints validate request body, path UUIDs, and status codes.
- Error responses use the shared `ProblemDetail` contract.

## Keycloak And Security Tests

- Valid JWT subject UUID is passed as the user ID to service methods.
- Missing JWT returns 401 for user and internal endpoints unless an endpoint is explicitly documented as public.
- Malformed subject claim returns 400 or 401 according to production behavior and must not call the service.
- JWT for user A cannot access user B's account because the service receives only the authenticated subject.
- Internal endpoints need an explicit policy decision: same user JWT, service-to-service scope, or gateway-only access. Tests should lock the chosen behavior.

## Repository And Integration Tests

- `AccountRepositoryTest` with `@DataJpaTest` and PostgreSQL Testcontainers.
- `AccountServiceIT` with `@SpringBootTest` for persistence plus outbox behavior.

Cover:

- Active account lookup by `accountId` and `userId`.
- Summary aggregation by currency.
- Batch lookup by account IDs.
- Filter combinations for institution, account name, type, subtype.
- Required columns and numeric precision.
- Outbox records are created inside the same transaction as account changes.
- Outbox relay publishes `AccountRecorded` and `AccountRemoved` to the expected routing keys.
- Outbox relay publish failure leaves the event retryable and does not mark it sent.
- Concurrent outbox relay workers do not publish the same event twice if locking or status checks are intended.

## Testing Techniques

- Equivalence Class Partitioning: active/disabled, owned/foreign, found/missing.
- Boundary Value Analysis: page limit `1`, `100`, `0`, `101`.
- Pairwise Testing: filters plus sort direction plus cursor.
- Race Condition Testing: concurrent outbox relay workers and simultaneous account upserts for the same external account.
- Error Guessing: malformed cursor, duplicate external account, database failure before event publish, RabbitMQ unavailable during relay.

## Reports

- Surefire: `account-service/target/surefire-reports`
- Failsafe: `account-service/target/failsafe-reports`
- Coverage: `account-service/target/site/jacoco`
- Every account-service test batch must add or update a report under `docs/test-reports/account-service/` using the standard template from `00-testing-standard.md`.
- The report must separate unit, web slice, repository, integration, messaging, security, and race-condition coverage.
