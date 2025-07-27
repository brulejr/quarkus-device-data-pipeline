#!/bin/sh

export INCOMING_MQTT_HOST=$(cat /run/secrets/incoming_mqtt_host)
export INCOMING_MQTT_PORT=$(cat /run/secrets/incoming_mqtt_port)
export RABBITMQ_CLIENT_USERNAME=$(cat /run/secrets/rabbitmq_client_username)
export RABBITMQ_CLIENT_PASSWORD=$(cat /run/secrets/rabbitmq_client_password)

java -jar /app/ingester-ms.jar --spring.config.additional-location=file:/config/app