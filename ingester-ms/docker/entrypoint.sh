#!/bin/sh

export INCOMING_MQTT_HOST=$(cat /run/secrets/incoming_mqtt_host)
export INCOMING_MQTT_PORT=$(cat /run/secrets/incoming_mqtt_port)
export OUTGOING_RABBITMQ_HOST=$(cat /run/secrets/outgoing_rabbitmq_host)
export OUTGOING_RABBITMQ_PORT=$(cat /run/secrets/outgoing_rabbitmq_port)
export OUTGOING_RABBITMQ_USERNAME=$(cat /run/secrets/outgoing_rabbitmq_username)
export OUTGOING_RABBITMQ_PASSWORD=$(cat /run/secrets/outgoing_rabbitmq_password)

java -jar /app/ingester-ms.jar --spring.config.additional-location=file:/config/app