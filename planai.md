# AI Assistant Service Plan

## Goal

Build a very simple `ai-assistant-service` that:

- uses Spring AI tool calling
- uses a local model later
- does not own business logic
- does not own finance data
- helps users:
  - understand their financial state
  - inspect dashboard summaries
  - inspect budgets
  - create/update/deactivate budgets

The assistant should act as a **thin orchestration layer** over the services we already have.

---

## Hard Rule

## Do not download or choose a model yet

This is important.

At this phase:

- do **not** download Ollama models
- do **not** add provider-specific model setup yet
- do **not** hardcode any model choice

Reason:

- the model will be chosen later manually
- Spring AI abstracts the chat model behind interfaces
- we should prepare the service so the model provider can be plugged in later with minimal code changes

So for now the design must be:

- Spring AI first
- model/provider later

---

## Core Principles

- no database in v1
- no vector database
- no RAG in v1
- no autonomous agent loops
- no long-lived memory persistence
- no business rules duplicated from other services
- no direct raw orchestration by frontend if AI can call a domain/BFF endpoint
- keep the tool set small and deterministic

If something is complex, do not include it in v1.

---

## Architecture

`ai-assistant-service` should be:

- stateless HTTP service
- secured by JWT like other services
- tool-calling chat service
- thin orchestration over existing services

It should call:

- `dashboard-service` for summaries
- `budgeting-service` for budget actions
- `transaction-service` for category lookup
- `account-service` only if account disambiguation is needed later

It should **not**:

- query databases directly
- maintain its own financial projections
- duplicate category metadata persistence
- implement budgeting math itself

---

## Why this is the best fit

This gives us:

- small service surface
- easy model replacement later
- no coupling to low-level internal details
- clean AI function-calling integration
- minimal risk of inconsistent business logic

The assistant becomes a safe interface, not another domain service.

---

## Existing services it should use

### `dashboard-service`

Use for:

- user summary
- account summary

This is the main read entry point for the AI.

### `budgeting-service`

Use for:

- list budgets
- create budget
- update budget
- deactivate budget

### `transaction-service`

Use for:

- primary category lookup

We already have:

- `GET /transactions/categories/primary/{id}`
- `POST /transactions/categories/primary/by-ids`

For AI, the simplest addition should be:

- `GET /transactions/categories/primary`

Reason:

- primary category count is very small
- no pagination is needed
- no search endpoint is needed
- AI can inspect all primary categories and choose the closest one

### `account-service`

Optional in v1.

Only needed if we want prompts like:

- “show my checking account summary”

to resolve account names to IDs without relying on the dashboard summary.

---

## Minimal Tool Set

These are the tools we should expose to the model in v1.

### Read tools

1. `getDashboardSummary(from, to)`
2. `getAccountDashboardSummary(accountId, from, to)`
3. `getBudgets()`
4. `getAllPrimaryCategories()`

### Write tools

5. `createBudget(categoryId, limitAmount, isoCurrencyCode, period, periodStart)`
6. `updateBudget(budgetId, categoryId, limitAmount, isoCurrencyCode, period, periodStart)`
7. `deactivateBudget(budgetId)`

### Optional but useful soon after

8. `searchAccounts(query)`
9. `getRecentTransactions(limit)`

---

## Why these tools are enough

With these tools, the assistant can already handle:

- “show me my dashboard”
- “analyze my spendings this month”
- “show my account summary”
- “what budgets do I have?”
- “create me a 500 dollar food budget”
- “increase my food budget to 700”
- “delete my shopping budget”

This is enough for a useful v1.

---

## Tool workflow examples

## Example 1: Direct budget creation

Prompt:

> create me 500 dollar food budget

Expected flow:

1. detect intent = create budget
2. detect amount = `500`
3. detect currency = `USD`
4. detect category intent = `food`
5. call `getAllPrimaryCategories()`
6. resolve the best matching category ID from the small category list
7. fill defaults:
   - `period = MONTHLY`
   - `periodStart = first day of current month`
8. call `createBudget(...)`
9. return confirmation

## Example 2: Ambiguous prompt

Prompt:

> create a budget for food

Missing:

- amount
- maybe currency

Expected flow:

- do not write anything
- ask follow-up question

Example response:

> What budget amount should I use, and in which currency?

## Example 3: Analysis only

Prompt:

> analyze my spendings this month

Expected flow:

1. call `getDashboardSummary(...)`
2. summarize top categories / recent spending / budget pressure
3. no write tool called

## Example 4: Goal-oriented prompt

Prompt:

> analyze my spendings and create me a budget for saving for a car

Expected flow:

1. call `getDashboardSummary(...)`
2. call `getBudgets()`
3. analyze data
4. if user intent is underspecified, ask follow-up
5. suggest category budgets to help save
6. only write after confirmation

Important:

The current budgeting service supports **category budgets**, not savings-goal entities.

So the assistant should not pretend it can create a true “car savings goal” yet.

It should instead say something like:

> I can create category budgets that help you save for a car.

---

## Defaulting Rules

To keep the assistant usable and simple:

### Dates

If user does not specify dates:

- `from = first day of current month`
- `to = today`

### Budget period

If user says “budget” but not period:

- default to `MONTHLY`

### Budget start date

If user does not specify budget start:

- default to first day of current month

### Currency

If user does not specify currency:

- if current dashboard clearly shows one dominant currency, use it
- otherwise ask follow-up

### Category resolution

AI should resolve category intent using the small primary category list.

If:

- one clear match -> proceed
- multiple plausible matches -> ask follow-up
- no good match -> ask follow-up

---

## Safety Rules

The assistant must be conservative.

### Never write if intent is ambiguous

Do **not** create/update/delete budgets unless all required parameters are clear.

### Ask follow-up when needed

Ask if missing:

- amount
- category
- currency (if ambiguous)
- budget to modify/delete

### Use tools over guessing

Do not hallucinate category IDs or budget IDs.

Always resolve through tools.

### Confirm when prompt mixes analysis + action

If the user asks both:

- “analyze”
- and “create”

the assistant should analyze first, then confirm before calling write tools.

---

## API Design

### Assistant endpoint

```http
POST /api/assistant/chat
```

### Request

```json
{
  "conversationId": "optional-uuid",
  "message": "create me 500 dollar food budget"
}
```

### Response

For direct answer:

```json
{
  "conversationId": "uuid",
  "message": "I created a monthly Food And Drink budget of 500 USD for this month.",
  "requiresFollowUp": false
}
```

For clarification:

```json
{
  "conversationId": "uuid",
  "message": "Which currency should I use for this budget?",
  "requiresFollowUp": true
}
```

Keep this response very simple in v1.

---

## Memory / Conversation Strategy

### v1 recommendation

Use:

- in-memory conversation storage
- keyed by `conversationId`
- keep only recent turns, e.g. last 10

This is enough to support follow-up questions.

### Do not add yet

- persistent conversation DB
- vector memory
- semantic retrieval
- long-term user profile memory

If the service restarts, losing temporary conversation state is acceptable in v1.

---

## Spring AI implementation style

Use:

- `ChatClient`
- tool beans with `@Tool`
- typed parameters with `@ToolParam`

Suggested tool classes:

- `DashboardTools`
- `BudgetTools`
- `LookupTools`

Keep tools very thin:

- one tool -> one downstream service call
- no business logic duplication in tool classes

Business rules stay in the services you already have.

---

## Model strategy

Again:

## do not download or choose a model yet

The service should be prepared so that later we can plug in:

- Ollama
- another local provider
- maybe another Spring AI-supported provider

without changing tool/business code.

So implementation should separate:

- tool definitions
- assistant orchestration
- model/provider configuration

Only the last part should change when you choose the model.

---

## Dependencies to add later when implementing

### Core dependencies

- spring web
- spring security oauth2 resource server
- openfeign
- lombok

### AI dependencies

Add Spring AI core/tooling integration.

But do **not** add provider-specific starter until model choice is made.

If provider starter is required for compilation in the chosen style, add it only once the model decision is made.

---

## One small service change recommended before AI

Add a simple "get all primary categories" endpoint in `transaction-service`.

Recommended endpoint:

```http
GET /transactions/categories/primary
```

Response:

```json
[
  {
    "id": "uuid",
    "code": "FOOD_AND_DRINK",
    "displayName": "Food And Drink",
    "iconUrl": "default"
  }
]
```

This is enough because the number of primary categories is very small.

The assistant can inspect the full category list and choose the closest match without requiring search or pagination.

Optional later:

- account search helper if account disambiguation becomes important

---

## Implementation phases

### Phase 1

- initialize `ai-assistant-service`
- add security
- add Feign clients
- add simple `/api/assistant/chat`
- add read tools:
  - `getDashboardSummary`
  - `getBudgets`
  - `getAllPrimaryCategories`
- no write tools yet

### Phase 2

- add write tools:
  - `createBudget`
  - `updateBudget`
  - `deactivateBudget`

### Phase 3

- add account-specific tools:
  - `getAccountDashboardSummary`
  - maybe `searchAccounts`

### Phase 4

- improve follow-up handling
- improve prompt rules
- maybe add lightweight in-memory conversation service

---

## Final recommendation

The best v1 AI assistant for this project is:

- Spring AI tool-calling service
- local model later
- no DB
- no RAG
- no memory persistence
- few deterministic tools
- reads mainly from `dashboard-service`
- writes through `budgeting-service`
- category lookup through `transaction-service`

This is the simplest useful AI layer for your current architecture.
