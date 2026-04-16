#!/usr/bin/env bash

set -e

CLIENT_ID="68f013c09bda9e001fac9053"
SECRET="2a1cf9a2fc0dfb5152c552709accfc"

echo "🔹 Creating sandbox public token..."

PUBLIC_TOKEN=$(curl -s https://sandbox.plaid.com/sandbox/public_token/create \
  -H "Content-Type: application/json" \
  -d "{
    \"client_id\": \"$CLIENT_ID\",
    \"secret\": \"$SECRET\",
    \"institution_id\": \"ins_109508\",
    \"initial_products\": [\"transactions\", \"auth\"],
    \"options\": {
      \"override_username\": \"user_transactions_dynamic\",
      \"override_password\": \"user_good\"
    }
  }" | jq -r '.public_token')

echo "🔹 Exchanging token..."

ACCESS_TOKEN=$(curl -s https://sandbox.plaid.com/item/public_token/exchange \
  -H "Content-Type: application/json" \
  -d "{
    \"client_id\": \"$CLIENT_ID\",
    \"secret\": \"$SECRET\",
    \"public_token\": \"$PUBLIC_TOKEN\"
  }" | jq -r '.access_token')

echo "🔹 Fetching accounts..."

ACCOUNTS_COUNT=$(curl -s https://sandbox.plaid.com/accounts/get \
  -H "Content-Type: application/json" \
  -d "{
    \"client_id\": \"$CLIENT_ID\",
    \"secret\": \"$SECRET\",
    \"access_token\": \"$ACCESS_TOKEN\"
  }" | jq '.accounts | length')

echo "✅ Accounts fetched: $ACCOUNTS_COUNT"

echo "🔹 Syncing transactions..."

CURSOR=""
HAS_MORE="true"

TOTAL_ADDED=0
TOTAL_MODIFIED=0
TOTAL_REMOVED=0
PAGE=1

while [ "$HAS_MORE" = "true" ]; do
  echo "➡️  Page $PAGE..."

  if [ -z "$CURSOR" ]; then
    RESPONSE=$(curl -s https://sandbox.plaid.com/transactions/sync \
      -H "Content-Type: application/json" \
      -d "{
        \"client_id\": \"$CLIENT_ID\",
        \"secret\": \"$SECRET\",
        \"access_token\": \"$ACCESS_TOKEN\"
      }")
  else
    RESPONSE=$(curl -s https://sandbox.plaid.com/transactions/sync \
      -H "Content-Type: application/json" \
      -d "{
        \"client_id\": \"$CLIENT_ID\",
        \"secret\": \"$SECRET\",
        \"access_token\": \"$ACCESS_TOKEN\",
        \"cursor\": \"$CURSOR\"
      }")
  fi

  ADDED=$(echo "$RESPONSE" | jq '.added | length')
  MODIFIED=$(echo "$RESPONSE" | jq '.modified | length')
  REMOVED=$(echo "$RESPONSE" | jq '.removed | length')

  TOTAL_ADDED=$((TOTAL_ADDED + ADDED))
  TOTAL_MODIFIED=$((TOTAL_MODIFIED + MODIFIED))
  TOTAL_REMOVED=$((TOTAL_REMOVED + REMOVED))

  echo "   + added: $ADDED | modified: $MODIFIED | removed: $REMOVED"

  CURSOR=$(echo "$RESPONSE" | jq -r '.next_cursor')
  HAS_MORE=$(echo "$RESPONSE" | jq -r '.has_more')

  PAGE=$((PAGE + 1))
done

echo ""
echo "✅ FINAL TOTALS"
echo "   🟢 Added:    $TOTAL_ADDED"
echo "   🔄 Modified: $TOTAL_MODIFIED"
echo "   ❌ Removed:  $TOTAL_REMOVED"
