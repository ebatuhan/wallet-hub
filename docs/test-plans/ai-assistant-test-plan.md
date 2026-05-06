# AI Assistant Service Test Plan

Module: `ai-assistant`

Primary purpose: chat orchestration, conversation state machine, chat history pagination, prompt guarding, and assistant tools over dashboard/budget/category APIs.

## Targets

- `AIAssistantServiceImpl`
- `AIAssistantController`
- `PromptGuardAdvisor`
- `BudgetTools`
- `DashboardTools`
- `LookupTools`
- `ToolResponse`
- `ConversationRepository`
- `MessageRepository`
- Spring AI and Feign configuration classes.

## Unit Tests

- `AIAssistantServiceImplTest`
- `PromptGuardAdvisorTest`
- `BudgetToolsTest`
- `DashboardToolsTest`
- `LookupToolsTest`
- `ToolResponseTest`

## Service Cases

- Chat with missing model returns 503.
- Chat creates conversation when absent.
- Existing non-stale `PROCESSING` conversation returns 409.
- Stale `PROCESSING` conversation transitions to `FAILED` and allows new processing.
- Successful chat wraps prompt, calls `ChatClient`, sanitizes UUIDs, and marks conversation `IDLE`.
- Transient AI/resource errors mark conversation `FAILED` and return 503.
- Unexpected runtime errors mark conversation `FAILED` and rethrow or map as documented.
- History with no conversation returns empty list and `IDLE`.
- History clamps limit to 1 through 100.
- History rejects invalid cursor with 400.
- History with valid cursor filters before timestamp.
- History excludes blank, system, and tool messages.
- History returns chronological messages after reverse ordering.
- History returns next cursor when more rows exist.

## Tool Cases

- Budget tool validates required fields and delegates to budgeting client.
- Dashboard tool returns safe fallback response when downstream body is null.
- Lookup tool maps categories and handles missing categories.
- Tool response consistently reports success, error code, and message.

## Web Tests

- `AIAssistantControllerTest`

Cover:

- `POST /assistant/chat` success.
- `POST /assistant/chat` blank message returns 400.
- `POST /assistant/chat` message length 4000 succeeds.
- `POST /assistant/chat` message length 4001 returns 400.
- `GET /assistant/chat/history` with valid and invalid limits.
- 401 without JWT.
- 409 processing conflict maps to `ProblemDetail`.
- 503 model unavailable maps to `ProblemDetail`.

## Keycloak And Security Tests

- Valid JWT subject UUID scopes conversation and tool calls.
- Missing JWT returns 401.
- Malformed subject claim does not call AI or tools.
- User A cannot read user B's chat history.
- Feign tools propagate caller JWT or configured service identity.

## Repository And Integration Tests

- `ConversationRepositoryTest` with PostgreSQL Testcontainers.
- `MessageRepositoryTest` with PostgreSQL Testcontainers.
- `AIAssistantServiceIT` with mocked `ChatClient` or test `ChatModel`.

Cover:

- Unique `userId` conversation constraint.
- `findWithLockByUserId` pessimistic lock behavior.
- Chat memory SQL schema and migration compatibility.
- Message history cursor ordering.
- Concurrent chat requests for the same user produce one processing conflict and do not corrupt conversation state.
- Tool downstream timeout marks the chat failed or returns a safe tool error according to production behavior.

## Testing Techniques

- Equivalence Class Partitioning: no conversation, idle conversation, processing conversation, stale processing conversation.
- Boundary Value Analysis: prompt length `1`, `4000`, `4001`; history limit `1`, `100`, `0`, `101`.
- State Transition Testing: absent to processing to idle, processing conflict, stale processing to failed to idle, processing to failed on AI error.
- Decision Table Testing: AI availability, conversation status, stale timeout, chat success/failure.
- Error Guessing: malformed cursor, null AI response, downstream tool timeout, UUID leakage in response.

## Reports

- Surefire: `ai-assistant/target/surefire-reports`
- Failsafe: `ai-assistant/target/failsafe-reports`
- Coverage: `ai-assistant/target/site/jacoco`
- Every AI assistant test batch must add or update a report under `docs/test-reports/ai-assistant/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state whether chat state transitions, history cursoring, prompt guard, tools, Spring AI failures, and concurrent chat races were tested.
