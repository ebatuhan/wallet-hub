#!/bin/sh

# Exit immediately if a command exits with a non-zero status (optional safety)
# set -e 

PORT=${PLAID_SERVICE_PORT:-8082}

echo "--- Initializing Plaid Adapter Service (Dev Mode) ---"

# 1. REFRESH SHARED LIBRARY
# Since we mount ./shared to /shared, we must re-install it 
# so the local .m2 repository inside the container gets the latest JAR.
if [ -d "/shared" ]; then
    echo "🔄 Detected Shared Library. Installing..."
    cd /shared
    # Install quietly to reduce log noise, skip tests for speed
    mvn clean install -DskipTests > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Shared Library installed successfully."
    else
        echo "❌ Failed to install Shared Library. Check logs."
    fi
    cd /app
else
    echo "⚠️  Warning: /shared directory not found. Skipping library refresh."
fi

# 2. SETUP TUNA TUNNEL
echo "Saving Tuna Token..."
# Ensure you really want to hardcode the token here, 
# usually better to use ${TUNA_TOKEN} env var, but keeping as requested:
tuna config save-token tt_2dlldzwyqqe3l8tpulop64jzdnoo9e3x

echo "Starting Tuna tunnel for localhost:$PORT..."
# Run in background
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

# Extract the URL
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

# 3. START SPRING BOOT
# We use exec so the java process becomes PID 1 (receives stop signals correctly)
echo "🚀 Starting Spring Boot Application..."
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev