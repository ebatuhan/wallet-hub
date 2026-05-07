# Wallet Hub Backend

Wallet Hub is a personal finance backend built with Spring Boot microservices. It connects to Plaid, stores accounts and transactions, provides budgeting and dashboard data, exposes spending insights, and includes an AI assistant for finance-related chat.

The local stack uses Docker Compose with Keycloak for authentication, PostgreSQL and ClickHouse for storage, RabbitMQ for messaging, Spring Cloud Config for service configuration, and Grafana LGTM for observability.

## Services

- `api-gateway`: entry point for frontend/API clients.
- `config-service`: serves Spring Cloud Config from `config-repo/`.
- `plaid-adapter-service`: Plaid link-token, token exchange, connection, and webhook integration.
- `account-service`: account storage, account lookup, and balance summaries.
- `transaction-service`: transaction storage, category lookup, and transaction queries.
- `budgeting-service`: category budget management.
- `insights-service`: spending, income, graph, and account balance insights.
- `dashboard-service`: dashboard summary and drill-down endpoints.
- `ai-assistant-service`: AI chat and conversation history.
- `shared`: shared DTOs, messaging contracts, and cross-service code.

## Requirements

- Docker and Docker Compose.
- Java 21 and Maven, if running services manually outside Docker.
- A populated `.env` file for local ports.
- Local service config files under `config/` for Docker Compose env files.

## Run Locally

Start the full backend stack:

```bash
docker compose up --build
```

Start in the background:

```bash
docker compose up -d --build
```

Check service status:

```bash
docker compose ps
```

Stop the stack:

```bash
docker compose down
```

Stop and remove local volumes:

```bash
docker compose down -v
```

## Local URLs

- API Gateway: `http://localhost:8085`
- Keycloak: `http://localhost:8080`
- Config Service: `http://localhost:8888`
- Grafana LGTM: `http://localhost:3000`
- RabbitMQ Management: `http://localhost:15672`

## API Documentation

Each public service exposes its own OpenAPI documentation:

- Plaid Adapter: `http://localhost:8082/swagger-ui.html`
- Account Service: `http://localhost:8083/swagger-ui.html`
- Insights Service: `http://localhost:8084/swagger-ui.html`
- Transaction Service: `http://localhost:8086/swagger-ui.html`
- Budgeting Service: `http://localhost:8089/swagger-ui.html`
- Dashboard Service: `http://localhost:8090/swagger-ui.html`
- AI Assistant Service: `http://localhost:8091/swagger-ui.html`

OpenAPI JSON is available at `/v3/api-docs` on each service, and YAML is available at `/v3/api-docs.yaml`.
