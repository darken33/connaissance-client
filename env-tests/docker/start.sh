#!/bin/sh
docker compose up -d
echo COSMOSDB_DATABASE_NAME=admin
echo COSMOSDB_CONNECTION_STRING=mongodb://mongoadmin:secret@0.0.0.0:27017
echo KAFKA_BOOTSTRAP_SERVER=broker:9092
echo KAFKA_CONTROL_CENTER_URL=http://0.0.0.0:9021
echo KAFKA_SCHEMA_REGISTRY_URL=http://0.0.0.0:8090
