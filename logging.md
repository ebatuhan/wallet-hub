# WalletHub Logging Style

Use this file as the logging directive for new features, bug fixes, exception handling, and observability changes.

## Core Rule

Logs must help us answer three questions quickly:

1. What happened?
2. Which business operation/request did it affect?
3. What should we inspect next in traces, metrics, or code?

Prefer fewer, higher-signal logs over noisy step-by-step narration.

## Current Stack

WalletHub uses Spring Boot OpenTelemetry with OTLP export into the local LGTM stack:

- Traces: Tempo
- Logs: Loki
- Metrics: Prometheus
- Dashboards: Grafana

Application logs are exported through the OpenTelemetry Logback appender and include trace correlation fields such as `trace_id`, `span_id`, `service_name`, and exception metadata.

## Log Levels

Use `ERROR` when the operation failed and needs investigation.

Use `WARN` when the operation completed with degraded behavior, fallback behavior, invalid external input, or a recoverable integration problem.

Use `INFO` for meaningful lifecycle or business events that operators may care about.

Use `DEBUG` for local diagnostic details only. Do not rely on debug logs for production incident diagnosis.

Do not use logs for normal control-flow breadcrumbs.

## Exception Logging

Log exceptions once at the boundary that handles or translates them.

For controller advice, message handlers, schedulers, and integration boundaries:

```java
logger.error("Unexpected error while <business operation>", ex);
```

Always pass the exception object as the final argument so stack traces and OpenTelemetry exception fields are preserved.

Do not log and rethrow the same exception unless adding essential boundary context. If you rethrow, prefer wrapping with context and let the outer boundary log once.

For handled server errors, record the exception on the current span:

```java
private void recordExceptionOnCurrentSpan(Exception ex) {
    Span span = Span.current();
    span.recordException(ex);
    span.setStatus(StatusCode.ERROR, ex.getMessage());
    span.setAttribute("error.type", ex.getClass().getName());
}
```

Use this for 5xx application exceptions, data access failures, unexpected exceptions, async handlers, and any boundary where Spring may resolve the exception before OpenTelemetry marks the span as failed.

Do not mark spans as error for expected 4xx validation, authorization, or not-found outcomes unless there is a real server-side failure.

## Message Style

Write log messages as stable event descriptions, not sentences with dynamic values embedded everywhere.

Good:

```java
logger.warn("Plaid account sync skipped: missing consent", kv("userId", userId), kv("institutionId", institutionId));
logger.error("Failed to list account views", ex);
```

Avoid:

```java
logger.info("starting method");
logger.info("user " + userId + " did thing with account " + accountId);
logger.error("error", ex);
```

Messages should be specific enough to search in Loki and useful enough to understand without opening the code immediately.

## Context

Prefer structured context over string concatenation when available.

Include IDs that help investigation:

- `userId`
- `accountId`
- `transactionId`
- `budgetId`
- `consentId`
- `institutionId`
- external provider request IDs

Do not log secrets, tokens, authorization headers, credentials, Plaid access tokens, refresh tokens, raw JWTs, or full bank account numbers.

Mask or omit sensitive financial data. Last four digits are acceptable only when already part of a user-facing identifier.

## Tracing Relationship

If an operation is important enough to need multiple logs, it probably deserves a span instead.

Use `@Observed` for meaningful business operations that appear in Grafana/Tempo:

```java
@Observed(name = "account.list", contextualName = "account list accounts")
public CursorResponse<AccountViewDto> getAccountsViewPaginated(...) {
    ...
}
```

Use concise names:

- Metric/span name: `domain.action`, for example `account.list`, `budget.create`, `plaid.exchange-token`
- Contextual name: readable operation, for example `account list accounts`

Do not create spans for trivial getters, simple mapping methods, or every repository call.

## New Feature Checklist

When implementing something new:

1. Add logs only at meaningful boundaries or state transitions.
2. Add `@Observed` to important service operations that should appear in traces.
3. Ensure exceptions are logged once at the handling boundary.
4. Record handled 5xx exceptions on the current span.
5. Include safe business identifiers as structured context.
6. Avoid actuator/noise-style logs and traces.
7. Verify logs can be found in Loki by `service_name` and correlated by `trace_id`.

## Anti-Patterns

Do not add logs like:

```java
logger.info("Entered method");
logger.info("Exiting method");
logger.error(ex.getMessage());
logger.error("Exception occurred");
```

Do not swallow exceptions after logging unless the fallback behavior is intentional and documented in the log message.

Do not log request/response bodies by default. If needed for local debugging, keep it temporary and remove it before committing.

Do not duplicate the same exception in multiple layers.

## Grafana Queries

Useful local queries:

```logql
{service_name="account-service"}
```

```logql
{service_name=~".+"} | trace_id="<trace-id>"
```

```traceql
{ event:name = "exception" }
```

```traceql
{ status = error }
```
