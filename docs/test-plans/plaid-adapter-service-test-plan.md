# Plaid Adapter Service Test Plan

Module: `plaid-adapter-service`

Primary purpose: Plaid link-token creation, public-token exchange, connection lifecycle, account/transaction sync, webhook routing, and Plaid error handling.

## Existing Tests To Remove

- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/TestSupportConfiguration.java`
- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/unit/**`
- `plaid-adapter-service/src/test/java/com/batu/plaid_adapter_service/integration/**`

## Targets

- `PlaidIntegrationServiceImpl`
- `ConnectionServiceImpl`
- `PlaidClientWrapper`
- `PlaidRequestMapper`
- `ConnectionMapper`
- `DeterministicIdGenerator`
- `StringHasher`
- Webhook strategies and factories
- Plaid error handlers and factories
- `PlaidAdapterController`
- `WebhookController`
- `ConnectionRepository`

## Unit Tests

- `PlaidIntegrationServiceImplTest`
- `ConnectionServiceImplTest`
- `PlaidClientWrapperTest`
- `PlaidRequestMapperTest`
- `ConnectionMapperTest`
- `DeterministicIdGeneratorTest`
- `StringHasherTest`
- `WebhookFactoryTest`
- `PlaidErrorHandlerFactoryTest`
- One `*Test` per webhook strategy.
- One `*Test` per Plaid error strategy.

## Service Cases

- Create link token builds request with user ID, products, country, and webhook URL.
- Create link token returns Plaid token.
- Plaid exception maps to expected application error.
- Exchange token rejects duplicate selected accounts within the same institution.
- Exchange token skips duplicate check when selected account list is empty.
- Successful exchange creates connection, syncs accounts/transactions, and returns response.
- Plaid exchange failure creates no connection.
- Mock token flow delegates to exchange flow.
- Remove inactive connection is a no-op.
- Remove active connection removes Plaid item, deactivates accounts, deactivates transactions, and deactivates connection.
- Sync inactive connection is a no-op.
- Sync active connection handles single and multiple Plaid pages.
- Sync updates cursor only after successful downstream upserts.
- Sync uses deterministic account and transaction IDs.
- Confirm intended handling for removed Plaid transactions before writing final assertions.

## Web Tests

- `PlaidAdapterControllerTest`
- `WebhookControllerTest`

Cover:

- `POST /plaid/link-token` with default and explicit country.
- `POST /plaid/exchange` request validation and duplicate conflict.
- `POST /plaid/mock` sandbox flow.
- `GET /plaid/connections`.
- `GET /plaid/connections/{connectionId}`.
- `PATCH /plaid/connections/{connectionId}`.
- `POST /plaid/connections/{connectionId}/refresh`.
- `DELETE /plaid/connections/{connectionId}`.
- 401 without JWT.
- Invalid UUID returns 400.
- Webhook routes known events to the matching strategy.
- Unknown webhook type uses fallback behavior.
- Malformed webhook payload returns the documented error response.

## Keycloak And Security Tests

- Valid JWT subject UUID is used for every connection operation.
- Missing JWT returns 401 for user endpoints.
- Malformed subject claim does not call Plaid or downstream clients.
- User A cannot read, refresh, update, or delete user B's connection.
- Webhook endpoint authentication policy is explicit: public Plaid webhook with signature validation, or protected endpoint. Tests must lock the chosen behavior.

## Repository And Integration Tests

- `ConnectionRepositoryTest` with PostgreSQL Testcontainers.
- `PlaidIntegrationServiceIT` with mocked `PlaidClientWrapper` or WireMock.
- `ConnectionLockingIT` for pessimistic lock behavior.

Cover:

- Unique external item ID.
- Active filters.
- `lockByConnectionId` locks rows during sync.
- Connection state after successful and failed sync.
- Downstream account and transaction client calls use correct payloads.
- RabbitMQ outbox events for connection removal are created and relayed with retryable failure behavior if this module owns connection events.

## Testing Techniques

- Equivalence Class Partitioning: active/inactive connection, duplicate/non-duplicate account, single/multiple sync pages.
- Boundary Value Analysis: empty selected account list, one selected account, many accounts.
- Decision Table Testing: connection removal with Plaid success/failure and downstream success/failure.
- State Transition Testing: active to synced, active to inactive, inactive no-op.
- Race Condition Testing: two sync requests for the same connection, remove during sync, duplicate webhook delivery.
- Error Guessing: Plaid timeout, malformed webhook, downstream account failure, lock contention, RabbitMQ unavailable during connection event relay.

## Reports

- Surefire: `plaid-adapter-service/target/surefire-reports`
- Failsafe: `plaid-adapter-service/target/failsafe-reports`
- Coverage: `plaid-adapter-service/target/site/jacoco`
- Every Plaid adapter test batch must add or update a report under `docs/test-reports/plaid-adapter-service/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state whether Plaid client failures, webhooks, duplicate delivery, locking/race conditions, downstream clients, and RabbitMQ/outbox behavior were tested.
