#!/bin/sh


PORT=${PLAID_SERVICE_PORT:-8082}

echo "--- Initializing Plaid Adapter Service (Dev Mode) ---"


echo "Saving Tuna Token..."
tuna config save-token tt_2dlldzwyqqe3l8tpulop64jzdnoo9e3x


echo "Starting Tuna tunnel for localhost:$PORT..."

tuna http $PORT > /tmp/tuna.log 2>&1 &


echo "Waiting for Tuna URL..."
ATTEMPTS=0
MAX_ATTEMPTS=30

while ! grep -q "tuna.am" /tmp/tuna.log; do
  sleep 1
  ATTEMPTS=$((ATTEMPTS+1))
  if [ $ATTEMPTS -ge $MAX_ATTEMPTS ]; then
    echo "Error: Tuna failed to generate a URL within 30 seconds."
    echo "--- TUNA LOGS ---"
    cat /tmp/tuna.log
    exit 1
  fi
done

RAW_URL=$(grep -o 'https://[^ ]*\.tuna\.am' /tmp/tuna.log | head -n 1)

if [ -z "$RAW_URL" ]; then
  echo "Error: Could not extract URL from logs."
  cat /tmp/tuna.log
  exit 1
fi


export PLAID_WEBHOOK_URL="${RAW_URL}/api/plaid/webhook"

echo "------------------------------------------------"
echo "✅ Tunnel Active: $RAW_URL"
echo "✅ Webhook URL:   $PLAID_WEBHOOK_URL"
echo "------------------------------------------------"

# 7. Start Spring Boot via Maven


exec mvn spring-boot:run -Dspring-boot.run.profiles=dev