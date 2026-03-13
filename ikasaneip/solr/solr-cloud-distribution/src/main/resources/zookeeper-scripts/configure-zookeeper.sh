#!/bin/bash
# Configure ZooKeeper from template and environment
# This script generates zoo.cfg from zoo.cfg.template using settings from ikasan-env.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ZK_HOME="$SCRIPT_DIR/.."

# Source environment configuration
if [ -f "$ZK_HOME/config/ikasan-env.sh" ]; then
    source "$ZK_HOME/config/ikasan-env.sh"
else
    echo "ERROR: config/ikasan-env.sh not found"
    echo ""
    echo "Please create config/ikasan-env.sh from one of the sample files:"
    echo "  For localhost testing: cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh"
    echo "  For multi-host: cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh"
    echo ""
    echo "Then edit config/ikasan-env.sh to match your deployment"
    exit 1
fi

# Unified template file and output
TEMPLATE_FILE="$ZK_HOME/zookeeper-configs/zoo.cfg.template"
OUTPUT_FILE="$ZK_HOME/zookeeper/conf/zoo.cfg"

if [ ! -f "$TEMPLATE_FILE" ]; then
    echo "ERROR: Template file not found: $TEMPLATE_FILE"
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$(dirname "$OUTPUT_FILE")"

echo "Configuring ZooKeeper for Node $NODE_ID..."
echo "  Source Template: $TEMPLATE_FILE"
echo "  Output File: $OUTPUT_FILE"
echo ""

# Validate required environment variables
if [ -z "$NODE_ID" ]; then
    echo "ERROR: NODE_ID not set in ikasan-env.sh"
    exit 1
fi

if [ -z "$ZK_DATA_DIR" ] || [ -z "$ZK_LOG_DIR" ]; then
    echo "ERROR: ZK_DATA_DIR and ZK_LOG_DIR must be set in ikasan-env.sh"
    exit 1
fi

echo "Configuration values:"
echo "  Node ID: $NODE_ID"
echo "  ZK Data Dir: $ZK_DATA_DIR"
echo "  ZK Log Dir: $ZK_LOG_DIR"
echo "  ZK Client Port: $ZK_CLIENT_PORT"
echo "  ZK Peer Port: $ZK_PEER_PORT"
echo "  ZK Election Port: $ZK_ELECTION_PORT"
echo "  ZK Admin Port: $ZK_ADMIN_PORT"
echo ""
echo "Ensemble configuration:"
echo "  Node 1: ${NODE1_BINDING}:${NODE1_ZK_PEER_PORT}:${NODE1_ZK_ELECTION_PORT}"
echo "  Node 2: ${NODE2_BINDING}:${NODE2_ZK_PEER_PORT}:${NODE2_ZK_ELECTION_PORT}"
echo "  Node 3: ${NODE3_BINDING}:${NODE3_ZK_PEER_PORT}:${NODE3_ZK_ELECTION_PORT}"
echo ""

# Generate zoo.cfg from template
sed -e "s|__ZK_DATA_DIR__|$ZK_DATA_DIR|g" \
    -e "s|__ZK_LOG_DIR__|$ZK_LOG_DIR|g" \
    -e "s|__ZK_CLIENT_PORT__|$ZK_CLIENT_PORT|g" \
    -e "s|__ZK_ADMIN_PORT__|$ZK_ADMIN_PORT|g" \
    -e "s|__NODE1_BINDING__|$NODE1_BINDING|g" \
    -e "s|__NODE2_BINDING__|$NODE2_BINDING|g" \
    -e "s|__NODE3_BINDING__|$NODE3_BINDING|g" \
    -e "s|__NODE1_PEER_PORT__|$NODE1_ZK_PEER_PORT|g" \
    -e "s|__NODE2_PEER_PORT__|$NODE2_ZK_PEER_PORT|g" \
    -e "s|__NODE3_PEER_PORT__|$NODE3_ZK_PEER_PORT|g" \
    -e "s|__NODE1_ELECTION_PORT__|$NODE1_ZK_ELECTION_PORT|g" \
    -e "s|__NODE2_ELECTION_PORT__|$NODE2_ZK_ELECTION_PORT|g" \
    -e "s|__NODE3_ELECTION_PORT__|$NODE3_ZK_ELECTION_PORT|g" \
    "$TEMPLATE_FILE" > "$OUTPUT_FILE"

echo "✓ ZooKeeper configuration generated successfully"
echo ""
echo "Generated configuration file: $OUTPUT_FILE"
echo ""
echo "Next steps:"
echo "  1. Review the generated configuration: cat $OUTPUT_FILE"
echo "  2. Ensure myid file is created: ./zookeeper-scripts/setup-myid.sh"
echo "  3. Create required directories: source config/ikasan-env.sh && validate_directories"
echo "  4. Start ZooKeeper: ./zookeeper-scripts/zkServer.sh start"
