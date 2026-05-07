#!/bin/sh

PORT=${PLAID_SERVICE_PORT:-8082}

echo "--- Initializing Plaid Adapter Service (Dev Mode) ---"

if [ -d "/shared" ]; then
    echo "Detected Shared Library. Installing..."
    cd /shared
    mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "Shared Library installed successfully."
    else
        echo "Failed to install Shared Library. Check logs."
    fi
    cd /app
else
    echo "Warning: /shared directory not found. Skipping library refresh."
fi

if [ -z "$TUNA_TOKEN" ]; then
    echo "TUNA_TOKEN is not configured. Starting without a public webhook tunnel."
else
    echo "Starting Tuna tunnel for localhost:$PORT..."
    tuna config save-token "$TUNA_TOKEN"
    tuna http $PORT > /tmp/tuna.log 2>&1 &

    TUNA_PID=$!
    sleep 5

    ATTEMPTS=0
    MAX_ATTEMPTS=30
    RAW_URL=""

    while [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
        RAW_URL=$(grep -o 'https://[^ ]*.tuna.am' /tmp/tuna.log | head -n 1)
        if [ ! -z "$RAW_URL" ]; then
            break
        fi
        sleep 1
        ATTEMPTS=$((ATTEMPTS+1))
    done

    if [ -z "$RAW_URL" ]; then
        echo "Could not start Tuna tunnel. Starting without a public webhook tunnel."
        cat /tmp/tuna.log
        kill "$TUNA_PID" 2>/dev/null || true
    else
        export PLAID_WEBHOOK_URL="${RAW_URL}/api/plaid/webhook"
        echo "------------------------------------------------"
        echo "Tunnel Active: $RAW_URL"
        echo "Webhook URL: $PLAID_WEBHOOK_URL"
        echo "------------------------------------------------"
    fi
fi

echo "Starting Spring Boot Application..."
exec mvn clean spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true
