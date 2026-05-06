# Budgeting Service Test Plan

Module: `budgeting`

Primary purpose: budget CRUD, period overlap prevention, budget spending updates from transaction events, and budget pagination.

## Targets

- `BudgetServiceImpl`
- `BudgetController`
- `BudgetRepository`
- `BudgetMapper`
- `TransactionRecordedEventListener`
- Inbox/outbox support classes if present.

## Unit Tests

- `BudgetServiceImplTest`
- `BudgetMapperTest`
- `TransactionRecordedEventListenerTest`

## Service Cases

- `createBudget_whenValid_shouldSaveWithUserIdAndPeriod`
- `createBudget_whenOverlappingActiveBudget_shouldThrowConflict`
- `createBudget_whenAdjacentPeriod_shouldSucceed`
- `updateBudget_whenMissing_shouldThrowNotFound`
- `updateBudget_whenOnlySelfOverlaps_shouldSucceed`
- `updateBudget_whenOtherBudgetOverlaps_shouldThrowConflict`
- `deactivateBudget_whenExists_shouldSetInactive`
- `deactivateBudget_whenMissing_shouldThrowNotFound`
- `getBudgets_whenCursorMissing_shouldUseInitialKeyset`
- `getBudgets_whenCursorPresent_shouldDecodeCursor`
- `getBudgets_whenHasNext_shouldReturnNextCursor`
- `applyTransaction_whenInactivePendingPositiveOrIncomplete_shouldNoOp`
- `applyTransaction_whenMatchingExpense_shouldIncreaseSpentAmountByAbsoluteAmount`
- `applyTransaction_whenNoCandidate_shouldNoOp`
- `applyTransaction_whenCandidatePeriodEnded_shouldNoOp`

## Web Tests

- `BudgetControllerTest`

Cover:

- `POST /budgets` returns created budget.
- `PUT /budgets/{budgetId}` updates owned budget.
- `GET /budgets` supports cursor and limit.
- `DELETE /budgets/{budgetId}` deactivates budget.
- 401 without JWT.
- Invalid UUID returns 400.
- Validation failures for missing `categoryId`, non-positive `limitAmount`, invalid currency, missing period, missing period start, and invalid limit.
- Conflict from overlap maps to 409 `ProblemDetail`.
- Missing budget maps to 404 `ProblemDetail`.

## Keycloak And Security Tests

- Valid JWT subject UUID is used as budget owner.
- Missing JWT returns 401.
- Malformed subject claim does not call the service.
- User A cannot update, delete, or list user B's budgets.
- Event-driven updates still preserve user isolation from the event payload.

## Repository And Integration Tests

- `BudgetRepositoryTest` with PostgreSQL Testcontainers.
- `BudgetServiceIT` with `@SpringBootTest`.
- `BudgetMessagingIT` with RabbitMQ Testcontainers after messaging tooling is added.
- `BudgetingInboxConcurrencyIT` with PostgreSQL Testcontainers and concurrent duplicate delivery simulation.

Cover:

- `findCandidates` returns active budgets matching user, category, currency, and date.
- `findByIdAndUserId` isolates users.
- Active filtering excludes deactivated budgets.
- Numeric precision is preserved.
- Period overlap behavior is correct against the real database.
- Inbox idempotency skips duplicate transaction events.
- Concurrent duplicate `TransactionRecorded` deliveries with the same `eventId` do not apply the same amount twice. If the current implementation applies twice, capture that as a failing regression test before fixing the inbox algorithm.
- Handler failure before inbox insert does not mark the event processed; redelivery can apply the event once.
- Rabbit listener binds to `BUDGETING_TRANSACTION_RECORDED_QUEUE` and receives `TRANSACTION_RECORDED_ROUTING_KEY` messages.

## Testing Techniques

- Equivalence Class Partitioning: active/inactive event, pending/non-pending, expense/income, complete/incomplete event.
- Boundary Value Analysis: adjacent budget periods, equal start/end dates, page limit `1`, `100`, `0`, `101`.
- Decision Table Testing: transaction event application based on status, amount sign, fields, candidate, and date containment.
- State Transition Testing: active budget to inactive budget.
- Race Condition Testing: concurrent duplicate Rabbit deliveries, concurrent updates to the same active budget, duplicate inbox insert conflict.
- Error Guessing: duplicate event, malformed cursor, conflicting period updates, Rabbit redelivery after handler exception.

## Reports

- Surefire: `budgeting/target/surefire-reports`
- Failsafe: `budgeting/target/failsafe-reports`
- Coverage: `budgeting/target/site/jacoco`
- Every budgeting test batch must add or update a report under `docs/test-reports/budgeting/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state whether overlap logic, event application, inbox idempotency, concurrent duplicate delivery, and PostgreSQL integration were tested.
