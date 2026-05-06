# API Gateway Test Plan

Module: `api-gateway`

Primary purpose: route requests to backend services, enforce gateway security, and expose consistent gateway error handling and observability configuration.

## Targets

- `ApiGatewayApplication`
- `KeycloakGatewayConfiguration`
- `GatewayErrorHandler`
- `ObservationConfiguration`
- Route configuration in gateway properties or YAML.

## Unit And Slice Tests

- `KeycloakGatewayConfigurationTest`
- `GatewayErrorHandlerTest`
- `GatewayRoutesTest`

## Cases

- Protected routes reject unauthenticated requests with 401.
- Valid JWT allows protected route access.
- Required scopes or roles are enforced if configured.
- JWT with missing subject, malformed subject, expired token, wrong issuer, and wrong audience is rejected.
- Keycloak realm/client role conversion is compatible with shared converter behavior where applicable.
- Public actuator or health routes remain accessible if intended.
- Gateway error handler returns the documented error response shape.
- Downstream 4xx and 5xx responses are preserved or transformed as intended.
- Route predicates match expected path prefixes.
- Route filters rewrite or preserve paths as configured.
- Authorization header is preserved for downstream services if expected.

## Integration Tests

- `ApiGatewayApplicationIT` for startup with test profile.
- `GatewayRoutesIT` using WebTestClient and WireMock downstream services.

Cover:

- Account route.
- Transaction route.
- Budgeting route.
- Plaid route.
- Insights route.
- Dashboard route.
- AI assistant route.
- Config-server dependency disabled or replaced under test profile.

## Testing Techniques

- Equivalence Class Partitioning: public/protected route, authenticated/unauthenticated, valid/invalid JWT.
- Pairwise Testing: path prefix plus HTTP method plus authentication state.
- Error Guessing: unknown route, downstream unavailable, invalid token, missing issuer config.

## Reports

- Surefire: `api-gateway/target/surefire-reports`
- Failsafe: `api-gateway/target/failsafe-reports`
- Coverage: `api-gateway/target/site/jacoco`
- Every API gateway test batch must add or update a report under `docs/test-reports/api-gateway/` using the standard template from `00-testing-standard.md`.
- The report must explicitly state which routes, JWT/Keycloak cases, downstream failures, and header propagation cases were tested.
