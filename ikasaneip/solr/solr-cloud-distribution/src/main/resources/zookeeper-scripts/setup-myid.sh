#!/bin/bash
# Setup ZooKeeper myid file
# This script creates the myid file required for ZooKeeper ensemble

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source environment configuration
if [ -f "$SCRIPT_DIR/../config/ikasan-env.sh" ]; then
    source "$SCRIPT_DIR/../config/ikasan-env.sh"
else
    echo "ERROR: config/ikasan-env.sh not found"
    echo "Please create it from one of the sample files:"
    echo "  For localhost: cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh"
    echo "  For multi-host: cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh"
    exit 1
fi

# Check if NODE_ID is set in the environment file
if [ -z "$NODE_ID" ]; then
    echo "ERROR: NODE_ID not set in config/ikasan-env.sh"
    exit 1
fi

# Check if ZK_DATA_DIR is set
if [ -z "$ZK_DATA_DIR" ]; then
    echo "ERROR: ZK_DATA_DIR not set in config/ikasan-env.sh"
    exit 1
fi

# Validate node ID
if ! [[ "$NODE_ID" =~ ^[1-3]$ ]]; then
    echo "ERROR: NODE_ID in ikasan-env.sh must be 1, 2, or 3 (found: $NODE_ID)"
    exit 1
fi

# Create data directory if it doesn't exist
mkdir -p "$ZK_DATA_DIR"

# Create myid file
echo "$NODE_ID" > "$ZK_DATA_DIR/myid"

if [ $? -eq 0 ]; then
    echo "✓ Successfully created myid file for node $NODE_ID"
    echo "  Location: $ZK_DATA_DIR/myid"
    echo "  Content: $(cat $ZK_DATA_DIR/myid)"
else
    echo "ERROR: Failed to create myid file"
    exit 1
fi
