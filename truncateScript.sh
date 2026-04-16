#!/usr/bin/env bash
set -euo pipefail

# ----------------------------------------
# Config
# ----------------------------------------

POSTGRES_USER="postgres"

# postgres container -> db -> tables
POSTGRES_TARGETS=(
  "postgres-transaction:transaction_db:transactions"
  "postgres-account:account_db:accounts"
  "postgres-budgeting:budgeting_db:budgets,processed_transactions"
  "postgres-plaid:plaid_db:connection,plaid_account_lookup,plaid_transaction_lookup,sync_outbox_message"
)

# clickhouse container -> db -> tables
CLICKHOUSE_CONTAINER="clickhouse-insights"
CLICKHOUSE_DB="clickhouse"
CLICKHOUSE_TABLES=(
  "transactions"
  "account_balance_history"
)

# ----------------------------------------
# Helpers
# ----------------------------------------

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

require_container_running() {
  local container="$1"
  if ! docker ps --format '{{.Names}}' | grep -Fxq "$container"; then
    echo "ERROR: container '$container' is not running." >&2
    exit 1
  fi
}

truncate_postgres_tables() {
  local container="$1"
  local db="$2"
  local tables_csv="$3"

  require_container_running "$container"

  IFS=',' read -r -a tables <<< "$tables_csv"

  for table in "${tables[@]}"; do
    log "Truncating PostgreSQL table ${db}.${table} in container ${container} ..."
    docker exec -i "$container" \
      psql -U "$POSTGRES_USER" -d "$db" -v ON_ERROR_STOP=1 \
      -c "TRUNCATE TABLE public.${table} RESTART IDENTITY CASCADE;"
  done
}

truncate_clickhouse_tables() {
  local container="$1"
  local db="$2"

  require_container_running "$container"

  for table in "${CLICKHOUSE_TABLES[@]}"; do
    log "Truncating ClickHouse table ${db}.${table} in container ${container} ..."
    docker exec -i "$container" \
      clickhouse-client --query="TRUNCATE TABLE ${db}.${table};"
  done
}

# ----------------------------------------
# Main
# ----------------------------------------

log "Starting truncate job..."

for target in "${POSTGRES_TARGETS[@]}"; do
  IFS=':' read -r container db tables <<< "$target"
  truncate_postgres_tables "$container" "$db" "$tables"
done

truncate_clickhouse_tables "$CLICKHOUSE_CONTAINER" "$CLICKHOUSE_DB"

log "All target tables truncated successfully."
