#!/bin/bash

# Debezium Connector Setup Script
# This script creates a PostgreSQL connector for Debezium

set -e  # Exit on any error

# Configuration
DEBEZIUM_URL="http://localhost:48083"

# Define all connectors
declare -A CONNECTORS
CONNECTORS["shipment-connector"]='{"name": "shipment-connector", "config": {"connector.class": "io.debezium.connector.postgresql.PostgresConnector", "database.hostname": "db", "database.port": "5432", "database.user": "postgres", "database.password": "changeme", "database.dbname": "shipment", "database.server.name": "shipment-server", "table.include.list": "shipment.shipment", "plugin.name": "pgoutput", "slot.name": "debezium_shipment", "publication.name": "dbz_shipment_publication", "publication.autocreate.mode": "filtered", "topic.prefix": "shipment", "key.converter": "org.apache.kafka.connect.json.JsonConverter", "value.converter": "org.apache.kafka.connect.json.JsonConverter", "key.converter.schemas.enable": false, "value.converter.schemas.enable": false, "transforms": "unwrap", "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState", "transforms.unwrap.drop.tombstones": false, "transforms.unwrap.delete.handling.mode": "rewrite", "decimal.handling.mode": "string"}}'
CONNECTORS["inventory-connector"]='{"name": "inventory-connector", "config": {"connector.class": "io.debezium.connector.postgresql.PostgresConnector", "database.hostname": "db", "database.port": "5432", "database.user": "postgres", "database.password": "changeme", "database.dbname": "inventory", "database.server.name": "inventory-server", "table.include.list": "inventory.inventory", "plugin.name": "pgoutput", "slot.name": "debezium_inventory", "publication.name": "dbz_inventory_publication", "publication.autocreate.mode": "filtered", "topic.prefix": "inventory", "key.converter": "org.apache.kafka.connect.json.JsonConverter", "value.converter": "org.apache.kafka.connect.json.JsonConverter", "key.converter.schemas.enable": false, "value.converter.schemas.enable": false, "transforms": "unwrap", "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState", "transforms.unwrap.drop.tombstones": false, "transforms.unwrap.delete.handling.mode": "rewrite", "decimal.handling.mode": "string"}}'
CONNECTORS["orders-connector"]='{"name": "orders-connector", "config": {"connector.class": "io.debezium.connector.postgresql.PostgresConnector", "database.hostname": "db", "database.port": "5432", "database.user": "postgres", "database.password": "changeme", "database.dbname": "ordering", "database.server.name": "orders-server", "table.include.list": "ordering.orders,ordering.order_items,ordering.cart_items", "plugin.name": "pgoutput", "slot.name": "debezium_orders", "publication.name": "dbz_orders_publication", "publication.autocreate.mode": "filtered", "topic.prefix": "orders", "key.converter": "org.apache.kafka.connect.json.JsonConverter", "value.converter": "org.apache.kafka.connect.json.JsonConverter", "key.converter.schemas.enable": false, "value.converter.schemas.enable": false, "transforms": "unwrap", "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState", "transforms.unwrap.drop.tombstones": false, "transforms.unwrap.delete.handling.mode": "rewrite", "decimal.handling.mode": "string"}}'

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Setting up Debezium PostgreSQL Connectors...${NC}"
echo "Connectors to create: shipment-connector, inventory-connector, orders-connector"
echo ""

# Check if Debezium Connect is running
echo "Checking Debezium Connect status..."
if ! curl -s -f "${DEBEZIUM_URL}/connectors" > /dev/null; then
    echo -e "${RED}Error: Cannot connect to Debezium at ${DEBEZIUM_URL}${NC}"
    echo "Make sure Debezium Connect is running and accessible"
    exit 1
fi

echo -e "${GREEN}Debezium Connect is accessible${NC}"

# Function to create a connector
create_connector() {
    local connector_name=$1
    local connector_config=$2

    echo -e "${YELLOW}Processing connector: ${connector_name}${NC}"

    # Check if connector already exists
    echo "Checking if connector '${connector_name}' already exists..."
    if curl -s "${DEBEZIUM_URL}/connectors/${connector_name}" | grep -q "error_code"; then
        echo "Connector does not exist, proceeding with creation..."
    else
        echo -e "${YELLOW}Connector '${connector_name}' already exists${NC}"
        read -p "Do you want to delete and recreate it? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "Deleting existing connector..."
            curl -s -X DELETE "${DEBEZIUM_URL}/connectors/${connector_name}"
            echo -e "${GREEN}Connector deleted${NC}"
            sleep 2
        else
            echo "Skipping ${connector_name}..."
            return 0
        fi
    fi

    # Create the connector
    echo "Creating connector '${connector_name}'..."

    RESPONSE=$(curl -s -X POST "${DEBEZIUM_URL}/connectors" \
      -H "Content-Type: application/json" \
      -w "HTTPSTATUS:%{http_code}" \
      -d "${connector_config}")

    # Extract HTTP status code
    HTTP_STATUS=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    # Extract response body
    RESPONSE_BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

    # Check response
    if [ "$HTTP_STATUS" -eq 201 ] || [ "$HTTP_STATUS" -eq 200 ]; then
        echo -e "${GREEN}✓ Connector '${connector_name}' created successfully!${NC}"
        echo "Response:"
        echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
    else
        echo -e "${RED}✗ Failed to create connector '${connector_name}' (HTTP $HTTP_STATUS)${NC}"
        echo "Response:"
        echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
        return 1
    fi

    # Verify connector status
    echo "Checking connector status..."
    sleep 3

    STATUS_RESPONSE=$(curl -s "${DEBEZIUM_URL}/connectors/${connector_name}/status")
    echo "Connector Status:"
    echo "$STATUS_RESPONSE" | jq '.' 2>/dev/null || echo "$STATUS_RESPONSE"

    # Check if connector is running
    if echo "$STATUS_RESPONSE" | grep -q '"state":"RUNNING"'; then
        echo -e "${GREEN}✓ Connector '${connector_name}' is running successfully!${NC}"
    else
        echo -e "${YELLOW}⚠ Connector '${connector_name}' may not be running properly. Check the status above.${NC}"
    fi

    echo ""
    echo "----------------------------------------"
    echo ""
}

# Process all connectors
SUCCESS_COUNT=0
TOTAL_COUNT=0

for connector_name in "${!CONNECTORS[@]}"; do
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
    if create_connector "$connector_name" "${CONNECTORS[$connector_name]}"; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    fi
done

echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo "Summary: ${SUCCESS_COUNT}/${TOTAL_COUNT} connectors created successfully"

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    echo -e "${GREEN}✓ All connectors are set up!${NC}"
else
    echo -e "${YELLOW}⚠ Some connectors may need attention${NC}"
fi

echo ""
echo "Useful commands:"
echo "Check all connectors: curl ${DEBEZIUM_URL}/connectors"
echo "Check connector status: curl ${DEBEZIUM_URL}/connectors/{connector-name}/status"
echo "Delete a connector: curl -X DELETE ${DEBEZIUM_URL}/connectors/{connector-name}"