#!/usr/bin/env bash

set -euo pipefail

echo "Setting up secrets..."
export INCOMING_MQTT_HOST=$(cat /run/secrets/incoming_mqtt_host)
export INCOMING_MQTT_PORT=$(cat /run/secrets/incoming_mqtt_port)
export RABBITMW_CLIENT_USERNAME=$(cat /run/secrets/rabbitmq_client_username)
export RABBITMW_CLIENT_PASSWORD=$(cat /run/secrets/rabbitmq_client_password)

# Call the original SpringBoot entrypoint
java -jar /app/ingester-ms.jar --spring.config.additional-location=file:/config/app
