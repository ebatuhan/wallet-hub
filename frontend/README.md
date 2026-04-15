# Wallet Hub Frontend

Authenticated Next.js frontend for the `wallet-hub` backend.

## Features

- Keycloak sign in and register entry points
- protected application shell
- real account, dashboard, transaction, budget, and AI assistant calls through the API gateway
- `Link account` wired to `POST /api/plaid/mock`
- compact dark UI with a spending donut and account balance chart

## Run

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

## Environment

```bash
API_GATEWAY_URL=http://localhost:8085
NEXT_PUBLIC_API_GATEWAY_URL=http://localhost:8085
KEYCLOAK_ISSUER=http://localhost:8080/realms/keycloak-realm
KEYCLOAK_CLIENT_ID=wallet-hub-web
AUTH_SECRET=replace-this-in-real-use
```

## Keycloak Note

This frontend assumes a **public** Keycloak client exists.

Required client setup:

- Client ID: `wallet-hub-web`
- Standard flow enabled
- Valid redirect URI: `http://localhost:3000/api/auth/callback`
- Valid post logout redirect URI: `http://localhost:3000/sign-in`
- Web origin: `http://localhost:3000`

No backend code was changed for Keycloak. If you create this client manually for testing, that is the only external auth setup to undo later.
