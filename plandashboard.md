# Dashboard Service Plan

## Goal

Build a very simple `dashboard-service` that serves two read-only summaries for end users:

1. user-level dashboard summary
2. account-level dashboard summary

Rules:

- no database
- no event consumers
- no projections
- no category metadata persistence
- no per-row service calls
- only orchestration/composition of existing services
- if something is complex, do not include it in v1

---

## Core Design

`dashboard-service` is a **stateless BFF/orchestrator**.

It only:

- receives user request
- calls internal services
- aggregates responses
- enriches category IDs with one batch metadata lookup
- returns ready-to-render JSON

It does **not**:

- own data
- store anything
- subscribe to RabbitMQ
- compute long-lived analytics

---

## Services it will use

### `account-service`

Use for:

- account details
- account list
- account totals / active count summary

### `transaction-service`

Use for:

- recent transactions
- batch primary-category metadata lookup by IDs

### `insights-service`

Use for:

- spending per category
- spending per category for one account
- account balance history

### `budgeting-service`

Use for:

- current budgets / budget highlights

---

## One small recommended internal API addition

### Add to `account-service`

Create:

```http
GET /accounts/summary
```

Reason:

Dashboard should **not** try to sum balances from paginated account lists.

If dashboard fetches only first page:
- total balance is wrong

If dashboard fetches all pages:
- wasteful
- ugly
- unnecessary

So the best place to compute totals is `account-service`, which owns the accounts table.

### Proposed response

```json
{
  "userId": "uuid",
  "activeAccountCount": 4,
  "totalsByCurrency": [
    {
      "isoCurrencyCode": "USD",
      "currentBalanceTotal": 4200.12,
      "availableBalanceTotal": 3900.00
    },
    {
      "isoCurrencyCode": "EUR",
      "currentBalanceTotal": 350.00,
      "availableBalanceTotal": 350.00
    }
  ]
}
```

Important:

- totals should be grouped by currency
- no fake merged single total across mixed currencies

---

## Category enrichment rule

`insights-service` stores and returns only `primaryCategoryId`.

So dashboard must enrich category metadata like this:

1. call insights endpoint
2. collect unique `primaryCategoryId`s
3. one batch call to `transaction-service`
4. build map in memory
5. enrich response

Never do per-row metadata fetches.

That means:

- no N+1
- one insights call
- one category metadata call

Recent transactions do not need enrichment because `transaction-service` already returns category display fields in `TransactionViewResponseDto`.

Budgeting responses also do not need dashboard-side enrichment because `budgeting-service` already enriches its own responses.

---

## Default period behavior

For both dashboard endpoints:

- if `from` and `to` are not given,
  - `from` = first day of current month
  - `to` = today

This means dashboard is monthly by default.

That is the simplest and most expected UX.

---

## Endpoint 1: User Dashboard Summary

### Endpoint

```http
GET /api/dashboard/summary
```

### Optional query params

- `from`
- `to`
- `recentLimit`

Defaults:

- `from` = first day of current month
- `to` = today
- `recentLimit` = `5`

### What to include

#### 1. Account snapshot

From `account-service`:

- `GET /accounts/summary`

Return:

- `activeAccountCount`
- `totalsByCurrency`

Optional additionally:

- top 3 accounts by balance from `GET /accounts?limit=3...`

If that sort path is annoying in practice, skip top accounts in v1.

#### 2. Recent transactions

From `transaction-service`:

- `GET /transactions?limit=5`

Return latest user transactions.

Use `recentLimit` if provided.

Important:

- dashboard returns only the first slice
- if user presses **load more**, frontend should call the normal paginated endpoint directly:
  - `GET /api/transactions?cursor=...&limit=...`
- dashboard should not own or reimplement cursor progression logic

Recommended dashboard shape for this block:

```json
{
  "recentTransactions": {
    "items": [...],
    "hasNext": true,
    "nextCursor": "..."
  }
}
```

#### 3. Spending by category

From `insights-service`:

- `GET /api/insights/spendings?from=...&to=...`

Then enrich category metadata using:

- `POST /transactions/categories/primary/by-ids`

#### 4. Budget highlights

Optional but easy enough:

From `budgeting-service`:

- `GET /budgets`

Return only lightweight highlights:

- `activeBudgetCount`
- `overBudgetCount`
- maybe top 3 budgets by usage percentage

If this feels noisy, include only counts in v1.

### Suggested response shape

```json
{
  "userId": "uuid",
  "period": {
    "from": "2026-04-01",
    "to": "2026-04-12"
  },
  "accounts": {
    "activeAccountCount": 4,
    "totalsByCurrency": [
      {
        "isoCurrencyCode": "USD",
        "currentBalanceTotal": 4200.12,
        "availableBalanceTotal": 3900.00
      }
    ]
  },
  "recentTransactions": [
    {
      "transactionId": "uuid",
      "amount": -12.65,
      "transactionName": "Market",
      "isoCurrencyCode": "USD",
      "categoryDisplayName": "Food And Drink",
      "detailedCategoryName": "Restaurant",
      "accountId": "uuid",
      "accountName": "Checking"
    }
  ],
  "spending": {
    "categories": [
      {
        "primaryCategoryId": "uuid",
        "primaryCategoryCode": "FOOD_AND_DRINK",
        "primaryCategoryDisplayName": "Food And Drink",
        "primaryCategoryIconUrl": "default",
        "percentage": 42.5,
        "totalAmount": 1200.00
      }
    ]
  },
  "budgets": {
    "activeBudgetCount": 3,
    "overBudgetCount": 1,
    "items": [
      {
        "id": "uuid",
        "categoryId": "uuid",
        "categoryCode": "FOOD_AND_DRINK",
        "categoryDisplayName": "Food And Drink",
        "categoryIconUrl": "default",
        "limitAmount": 500.00,
        "spentAmount": 520.00,
        "isoCurrencyCode": "USD",
        "period": "MONTHLY",
        "periodStart": "2026-04-01",
        "periodEnd": "2026-04-30",
        "active": true
      }
    ]
  }
}
```

---

## Endpoint 2: Account Dashboard Summary

### Endpoint

```http
GET /api/dashboard/accounts/{accountId}/summary
```

### Optional query params

- `from`
- `to`
- `recentLimit`

Defaults:

- `from` = first day of current month
- `to` = today
- `recentLimit` = `10`

### What to include

#### 1. Account details

From `account-service`:

- `GET /accounts/{accountId}`

Return:

- institution name
- account name
- type/subtype
- mask
- current/available balance
- currency

#### 2. Balance history

From `insights-service`:

- `GET /api/insights/accounts/{accountId}/balance-history?from=...&to=...`

#### 3. Spending by category for this account

From `insights-service`:

- `GET /api/insights/spendings/{accountId}?from=...&to=...`

Then enrich category metadata with one batch call to `transaction-service`.

#### 4. Recent transactions for this account

From `transaction-service`:

- `GET /transactions?accountId={accountId}&limit=10`

Important:

- dashboard returns only the first slice for account transactions too
- if user presses **load more**, frontend should call:
  - `GET /api/transactions?accountId={accountId}&cursor=...&limit=...`
- account transaction pagination remains owned by `transaction-service`

### Suggested response shape

```json
{
  "userId": "uuid",
  "period": {
    "from": "2026-04-01",
    "to": "2026-04-12"
  },
  "account": {
    "accountId": "uuid",
    "institutionName": "First Platypus Bank",
    "accountName": "Checking",
    "accountType": "depository",
    "accountSubtype": "checking",
    "accountMask": "0000",
    "currentBalance": 2300.50,
    "availableBalance": 2200.10,
    "isoCurrencyCode": "USD"
  },
  "balanceHistory": [
    {
      "date": "2026-04-01",
      "balance": 2100.00
    }
  ],
  "spending": {
    "categories": [
      {
        "primaryCategoryId": "uuid",
        "primaryCategoryCode": "FOOD_AND_DRINK",
        "primaryCategoryDisplayName": "Food And Drink",
        "primaryCategoryIconUrl": "default",
        "percentage": 55.2,
        "totalAmount": 350.00
      }
    ]
  },
  "recentTransactions": [
    {
      "transactionId": "uuid",
      "amount": -12.65,
      "transactionName": "Market",
      "isoCurrencyCode": "USD",
      "categoryDisplayName": "Food And Drink",
      "detailedCategoryName": "Restaurant",
      "accountId": "uuid",
      "accountName": "Checking"
    }
  ]
}
```

---

## Simplicity rules

To keep dashboard simple:

- no DB
- no cache in v1
- no events
- no persistence
- no per-row service calls
- no extra enrichment for recent transactions
- no complex balance aggregation in dashboard itself
- no cursor orchestration in dashboard beyond returning the first page metadata

If a feature needs too much work, skip it from v1.

---

## Internal call pattern

### User summary calls

- `account-service` → account summary
- `transaction-service` → recent transactions
- `insights-service` → spending summary
- `transaction-service` → batch category metadata
- `budgeting-service` → budgets (optional highlight block)

### Account summary calls

- `account-service` → account details
- `insights-service` → balance history
- `insights-service` → account spending summary
- `transaction-service` → batch category metadata
- `transaction-service` → account transactions

---

## Main conclusion

The easiest and cleanest v1 dashboard is:

### User summary
- active account count
- totals by currency
- recent transactions
- spending by category
- optional light budget highlights

### Account summary
- account details
- balance history
- spending by category for account
- recent account transactions

Only one internal service change is strongly recommended:

- add `GET /accounts/summary` to `account-service`

Everything else can already be built from the current setup.

For pagination:

- dashboard returns only first-page slices
- further pagination stays on the owning endpoints:
  - `/api/transactions`
  - `/api/accounts`
