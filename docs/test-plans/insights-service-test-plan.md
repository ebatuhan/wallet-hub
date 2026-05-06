# Insights Service Test Plan

Module: `insights-service`

Primary purpose: spending/income analytics, graph bucket generation, account balance history, and ClickHouse-backed event projections.

## Targets

- `TransactionInsightsServiceImpl`
- `AccountInsightsServiceImpl`
- `TransactionInsightsController`
- `AccountInsightController`
- `TransactionInsightsRepository`
- `AccountInsightRepository`
- `TransactionEventListener`
- `AccountEventListener`
- `InsightsInbox`
- RabbitMQ topology for recorded and removed account/transaction events.

## Unit Tests

- `TransactionInsightsServiceImplTest`
- `AccountInsightsServiceImplTest`
- `TransactionEventListenerTest`
- `AccountEventListenerTest`
- `InsightsInboxTest`
- `InsightsRabbitTopologyTest`

## Service Cases

- Date range where `from <= to` succeeds.
- Date range where `from > to` throws 400.
- Spending by category returns empty groups for empty repository result.
- Spending by category groups and totals by currency.
- Spending graph uses daily buckets for 1 to 31 days.
- Spending graph uses weekly buckets for 32 to 180 days.
- Spending graph uses monthly buckets over 180 days.
- Missing graph buckets are filled with zero values.
- Weekly buckets align to Monday.
- Monthly buckets align to day 1.
- Account-specific spending passes `accountId` to repository.
- Income delegates to repository and groups by currency.
- Save transaction insight delegates to repository.
- Delete transaction insight delegates to repository.
- Removed transaction events delete or deactivate the transaction projection if the feature is intended. Current code does not bind `TRANSACTION_REMOVED_QUEUE`; this must be resolved as either a production gap or an explicitly documented non-requirement.
- Account balance history maps rows in chronological order.

## Web Tests

- `TransactionInsightsControllerTest`
- `AccountInsightControllerTest`

Cover:

- `GET /api/insights/spendings`.
- `GET /api/insights/spendings/graph`.
- `GET /api/insights/income`.
- `GET /api/insights/spendings/{accountId}`.
- `GET /api/insights/spendings/graph/{accountId}`.
- `GET /api/insights/accounts/{accountId}/balance-history`.
- 401 without JWT.
- Invalid UUID returns 400.
- Invalid date formats return 400.
- `from > to` maps to `ProblemDetail`.
- Response JSON shape for grouped currency totals and graph series.

## Keycloak And Security Tests

- Valid JWT subject UUID scopes every query.
- Missing JWT returns 401.
- Malformed subject claim does not query ClickHouse.
- User A cannot query user B's account insights.
- Event consumers trust event payload user IDs only from the message broker path; web requests must use the JWT subject.

## Repository And Integration Tests

- `TransactionInsightsRepositoryTest`.
- `AccountInsightRepositoryTest`.
- `InsightsProjectionIT`.
- `InsightsInboxConcurrencyIT`.
- `InsightsRabbitTopologyIT` with RabbitMQ Testcontainers after messaging tooling is added.

Preferred database strategy:

- Use ClickHouse Testcontainers for repository SQL because this module is ClickHouse-oriented.
- If ClickHouse container setup is delayed, test services and controllers first, then add repository integration as a dedicated phase.

Cover:

- Insert/update transaction projection rows.
- Delete by account and user.
- Spending aggregation excludes inactive, pending, or non-expense rows if SQL does so.
- Income aggregation by currency.
- Balance history ordering by date.
- Inbox idempotency for duplicate account and transaction events.
- Concurrent duplicate account or transaction events with the same `eventId` do not run the projection handler twice. If the current implementation allows double handling, keep the failing test as proof before changing the inbox logic.
- Handler failure before inbox insert allows a later redelivery to process once.
- `TransactionRemoved` topology is either fully covered with queue, binding, listener, service delete, repository delete, and projection assertions, or explicitly documented as intentionally unsupported.

## Testing Techniques

- Equivalence Class Partitioning: empty/non-empty results, account/global query, valid/invalid range.
- Boundary Value Analysis: 31/32 days and 180/181 days for bucket selection.
- Pairwise Testing: date range, account ID presence, currency grouping.
- Race Condition Testing: concurrent duplicate Rabbit deliveries and duplicate ClickHouse inbox insert attempts.
- Error Guessing: missing bucket, invalid date, duplicate event, ClickHouse connectivity failure, unbound routing key.

## Reports

- Surefire: `insights-service/target/surefire-reports`
- Failsafe: `insights-service/target/failsafe-reports`
- Coverage: `insights-service/target/site/jacoco`
- Every insights-service test batch must add or update a report under `docs/test-reports/insights-service/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state whether ClickHouse SQL, RabbitMQ topology, inbox idempotency, concurrent duplicate delivery, and `TransactionRemoved` projection cleanup were tested or deferred.
