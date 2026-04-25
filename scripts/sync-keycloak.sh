#!/usr/bin/env bash

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-keycloak-realm}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

require_bin() {
  command -v "$1" >/dev/null 2>&1 || {
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  }
}

require_bin curl
require_bin jq

ADMIN_TOKEN="$({
  curl -fsS -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=admin-cli" \
    -d "username=${ADMIN_USER}" \
    -d "password=${ADMIN_PASSWORD}" \
    -d "grant_type=password"
} | jq -r '.access_token')"

kc_get() {
  curl -fsS -H "Authorization: Bearer ${ADMIN_TOKEN}" "$1"
}

kc_post() {
  local url="$1"
  local payload="$2"
  curl -fsS -X POST "$url" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$payload" >/dev/null
}

kc_put() {
  local url="$1"
  local payload="$2"
  curl -fsS -X PUT "$url" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$payload" >/dev/null
}

client_uuid() {
  kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=$1" | jq -r '.[0].id // empty'
}

mapper_id() {
  local client_id="$1"
  local mapper_name="$2"
  local id
  id="$(client_uuid "$client_id")"
  kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}/protocol-mappers/models" \
    | jq -r --arg name "$mapper_name" '.[] | select(.name == $name) | .id // empty'
}

ensure_confidential_service_client() {
  local client_id="$1"
  local secret="$2"
  local id
  local payload

  payload="$(jq -n \
    --arg clientId "$client_id" \
    --arg secret "$secret" \
    '{
      clientId: $clientId,
      name: $clientId,
      enabled: true,
      protocol: "openid-connect",
      publicClient: false,
      bearerOnly: false,
      serviceAccountsEnabled: true,
      standardFlowEnabled: false,
      directAccessGrantsEnabled: false,
      implicitFlowEnabled: false,
      fullScopeAllowed: false,
      secret: $secret,
      redirectUris: [],
      webOrigins: []
    }')"

  id="$(client_uuid "$client_id")"
  if [ -z "$id" ]; then
    kc_post "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" "$payload"
    id="$(client_uuid "$client_id")"
    printf 'Created client %s\n' "$client_id"
  else
    kc_put "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}" "$payload"
    printf 'Updated client %s\n' "$client_id"
  fi
}

ensure_client_role() {
  local client_id="$1"
  local role_name="$2"
  local id

  id="$(client_uuid "$client_id")"
  if ! kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}/roles" \
    | jq -e --arg role "$role_name" '.[] | select(.name == $role)' >/dev/null; then
    kc_post "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}/roles" "$(jq -n --arg name "$role_name" '{name: $name}')"
    printf 'Created role %s on %s\n' "$role_name" "$client_id"
  fi
}

assign_client_role_to_service_account() {
  local caller_client_id="$1"
  local target_client_id="$2"
  local role_name="$3"
  local caller_uuid
  local target_uuid
  local service_account_user_id
  local role_payload

  caller_uuid="$(client_uuid "$caller_client_id")"
  target_uuid="$(client_uuid "$target_client_id")"
  service_account_user_id="$(kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${caller_uuid}/service-account-user" | jq -r '.id')"

  if kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${service_account_user_id}/role-mappings/clients/${target_uuid}" \
    | jq -e --arg role "$role_name" '.[] | select(.name == $role)' >/dev/null; then
    return
  fi

  role_payload="$(kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${target_uuid}/roles/${role_name}")"
  kc_post \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${service_account_user_id}/role-mappings/clients/${target_uuid}" \
    "[$role_payload]"
  printf 'Granted %s on %s to %s service account\n' "$role_name" "$target_client_id" "$caller_client_id"
}

ensure_client_role_scope_mapping() {
  local caller_client_id="$1"
  local target_client_id="$2"
  local role_name="$3"
  local caller_uuid
  local target_uuid
  local role_payload

  caller_uuid="$(client_uuid "$caller_client_id")"
  target_uuid="$(client_uuid "$target_client_id")"

  if kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${caller_uuid}/scope-mappings/clients/${target_uuid}" \
    | jq -e --arg role "$role_name" '.[] | select(.name == $role)' >/dev/null; then
    return
  fi

  role_payload="$(kc_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${target_uuid}/roles/${role_name}")"
  kc_post \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${caller_uuid}/scope-mappings/clients/${target_uuid}" \
    "[$role_payload]"
  printf 'Added scope %s on %s to %s\n' "$role_name" "$target_client_id" "$caller_client_id"
}

ensure_hardcoded_audience_mapper() {
  local client_id="$1"
  local audience="$2"
  local id
  local current_mapper_id
  local mapper_name
  local payload

  id="$(client_uuid "$client_id")"
  mapper_name="aud-${audience}"
  payload="$(jq -n \
    --arg name "$mapper_name" \
    --arg audience "$audience" \
    '{
      name: $name,
      protocol: "openid-connect",
      protocolMapper: "oidc-audience-mapper",
      consentRequired: false,
      config: {
        "included.client.audience": $audience,
        "id.token.claim": "false",
        "access.token.claim": "true",
        "introspection.token.claim": "true"
      }
    }')"

  current_mapper_id="$(mapper_id "$client_id" "$mapper_name")"
  if [ -z "$current_mapper_id" ]; then
    kc_post "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}/protocol-mappers/models" "$payload"
    printf 'Added %s audience to %s\n' "$audience" "$client_id"
  else
    kc_put "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${id}/protocol-mappers/models/${current_mapper_id}" "$payload"
  fi
}

ensure_confidential_service_client "account-service" "account-service-secret"
ensure_confidential_service_client "transaction-service" "nWXtEyXO1hLTU1XfQYuLn74qHDoQjmMv"
ensure_confidential_service_client "consent-service" "consent-service-secret"
ensure_confidential_service_client "budgeting-service" "budgeting-service-secret"
ensure_confidential_service_client "insights-service" "insights-service-secret"
ensure_confidential_service_client "dashboard-service" "dashboard-service-secret"
ensure_confidential_service_client "ai-assistant-service" "ai-assistant-service-secret"
ensure_confidential_service_client "plaid-adapter-service" "BeFZtmL7fXpYG7QsOJdIQ6voP71alW1z"

ensure_client_role "account-service" "ACCOUNT_SERVICE_INTERNAL"
ensure_client_role "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
ensure_client_role "budgeting-service" "BUDGETING_SERVICE_INTERNAL"
ensure_client_role "insights-service" "INSIGHTS_SERVICE_INTERNAL"
ensure_client_role "dashboard-service" "DASHBOARD_SERVICE_INTERNAL"
ensure_client_role "plaid-adapter-service" "PLAID_ADAPTER_SERVICE_INTERNAL"

assign_client_role_to_service_account "transaction-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
assign_client_role_to_service_account "plaid-adapter-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
assign_client_role_to_service_account "plaid-adapter-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
assign_client_role_to_service_account "consent-service" "plaid-adapter-service" "PLAID_ADAPTER_SERVICE_INTERNAL"
assign_client_role_to_service_account "budgeting-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
assign_client_role_to_service_account "dashboard-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
assign_client_role_to_service_account "dashboard-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
assign_client_role_to_service_account "dashboard-service" "budgeting-service" "BUDGETING_SERVICE_INTERNAL"
assign_client_role_to_service_account "dashboard-service" "insights-service" "INSIGHTS_SERVICE_INTERNAL"
assign_client_role_to_service_account "ai-assistant-service" "dashboard-service" "DASHBOARD_SERVICE_INTERNAL"
assign_client_role_to_service_account "ai-assistant-service" "budgeting-service" "BUDGETING_SERVICE_INTERNAL"
assign_client_role_to_service_account "ai-assistant-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"

ensure_client_role_scope_mapping "transaction-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "plaid-adapter-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "plaid-adapter-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "consent-service" "plaid-adapter-service" "PLAID_ADAPTER_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "budgeting-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "dashboard-service" "account-service" "ACCOUNT_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "dashboard-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "dashboard-service" "budgeting-service" "BUDGETING_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "dashboard-service" "insights-service" "INSIGHTS_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "ai-assistant-service" "dashboard-service" "DASHBOARD_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "ai-assistant-service" "budgeting-service" "BUDGETING_SERVICE_INTERNAL"
ensure_client_role_scope_mapping "ai-assistant-service" "transaction-service" "TRANSACTION_SERVICE_INTERNAL"

for client_id in bruno wallet-hub-web; do
  for audience in api-gateway gateway account-service transaction-service consent-service budgeting-service insights-service dashboard-service ai-assistant-service; do
    ensure_hardcoded_audience_mapper "$client_id" "$audience"
  done
done

printf 'Keycloak realm sync complete.\n'
