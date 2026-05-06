# Dashboard Service Test Plan

Module: `dashboard-service`

Primary purpose: aggregate account, transaction, budget, and insight data into dashboard views through Feign clients.

## Targets

- `DashboardServiceImpl`
- `DashboardController`
- Feign clients for account, transaction, insights, and budgeting services.

## Unit Tests

- `DashboardServiceImplTest`

## Service Cases

- User summary defaults missing date range to current month through today.
- User summary passes explicit date range to downstream clients.
- Recent transaction limit defaults to 5 when missing.
- Null downstream bodies are converted to empty sections where production code defines fallback behavior.
- Spending categories are enriched with category metadata.
- Missing category metadata leaves optional display fields null without failing.
- Yearly trend uses January 1 through December 31 of the resolved year.
- Budget aggregation returns empty cursor response for null or empty budget response.
- Budget aggregation enriches category metadata.
- Account summary defaults transaction limit to 10.
- Account summary fetches account, balance history, spending, graph, and transactions.
- Account summary passes cursor through to transaction client.
- Transaction summary handles null transaction body.
- Transaction summary loads account and combines both responses.

## Web Tests

- `DashboardControllerTest`

Cover:

- `GET /dashboard/summary`.
- `GET /dashboard/budgets`.
- `GET /dashboard/accounts/{accountId}/summary`.
- `GET /dashboard/transactions/{transactionId}/summary`.
- 401 without JWT.
- Invalid UUID returns 400.
- Query parameter propagation.
- Response JSON shape for summary sections.
- If `from > to` currently has no validation, document current behavior before changing production code.

## Keycloak And Security Tests

- Valid JWT subject UUID is propagated to dashboard service calls.
- Caller JWT or service identity is propagated to Feign clients according to the configured Feign security pattern.
- Missing JWT returns 401.
- Malformed subject claim does not call downstream services.
- User A cannot request account or transaction summaries for user B because downstream calls include the authenticated subject context.

## Integration Tests

- `DashboardClientContractIT` using WireMock for downstream services.
- `DashboardServiceApplicationIT` for startup with test profile.

Cover:

- Feign paths, query parameters, and authorization header propagation.
- Downstream 404/500 behavior maps to the documented response.
- Null or empty downstream responses are handled consistently.
- Downstream timeout and partial failure behavior is tested for every client.

## Testing Techniques

- Equivalence Class Partitioning: null/non-null downstream body, metadata found/missing, default/explicit range.
- Boundary Value Analysis: transaction limit minimum and high values if validation is added.
- Pairwise Testing: from/to null combinations and recent limit presence.
- Error Guessing: downstream timeout, malformed downstream body, missing category metadata.

## Reports

- Surefire: `dashboard-service/target/surefire-reports`
- Failsafe: `dashboard-service/target/failsafe-reports`
- Coverage: `dashboard-service/target/site/jacoco`
- Every dashboard-service test batch must add or update a report under `docs/test-reports/dashboard-service/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state which Feign clients, downstream failure modes, security propagation, and aggregation cases were tested.
