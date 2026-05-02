# Wallet Hub Bruno Collection

## Environment

Use the `api` environment for local Docker Compose:

```bash
bru run bruno/wallet-hub --env api
```

Plaid sandbox values are read from process environment variables:

```bash
export PLAID_CLIENT_ID="..."
export PLAID_SECRET="..."
export PLAID_ACCESS_TOKEN="..."
export PLAID_PUBLIC_TOKEN="..."
```

## Useful Runs

Run Plaid adapter connection requests:

```bash
bru run bruno/wallet-hub/plaid-adapter-service --env api
```

Run error contract checks:

```bash
bru run bruno/wallet-hub/error-contract --env api
```

Generate an HTML report:

```bash
bru run bruno/wallet-hub --env api --reporter-html reports/bruno.html
```

## Runtime Variables

Some requests save runtime variables for follow-up requests:

- `connectionId` from Plaid adapter connection requests
- `accountId` and `accountCursor` from `account-service/getAccounts`
- `transactionId` and `transactionCursor` from `transaction-service/getTransactions`
