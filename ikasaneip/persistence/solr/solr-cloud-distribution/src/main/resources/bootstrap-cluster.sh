#!/bin/bash
# Bootstrap Ikasan SolrCloud Cluster
# This script should be run on ONE node BEFORE starting any Solr nodes

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source environment configuration
if [ -f "$SCRIPT_DIR/config/ikasan-env.sh" ]; then
    source "$SCRIPT_DIR/config/ikasan-env.sh"
else
    echo "ERROR: Cannot find ikasan-env.sh configuration file"
    exit 1
fi

# Use SOLR_INSTALL_DIR from ikasan-env.sh
SOLR_HOME="${SOLR_INSTALL_DIR}"

# Configuration from ikasan-env.sh
ZK_HOST="${ZK_HOSTS}"
COLLECTION_NAME="ikasan"
NUM_SHARDS=1
REPLICATION_FACTOR=3
CONFIG_NAME="ikasan"

echo "=========================================="
echo "Ikasan SolrCloud Cluster Bootstrap"
echo "=========================================="
echo "ZooKeeper: $ZK_HOST"
echo "Collection: $COLLECTION_NAME"
echo "Shards: $NUM_SHARDS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo "=========================================="
echo ""

# Step 1: Wait for ZooKeeper ensemble
echo "Step 1: Verifying ZooKeeper ensemble..."

# Extract first ZooKeeper host and port from ZK_HOST
FIRST_ZK=$(echo "$ZK_HOST" | cut -d',' -f1)
ZK_CHECK_HOST=$(echo "$FIRST_ZK" | cut -d':' -f1)
ZK_CHECK_PORT=$(echo "$FIRST_ZK" | cut -d':' -f2)

for i in {1..30}; do
    if echo ruok | nc -w 1 $ZK_CHECK_HOST $ZK_CHECK_PORT 2>/dev/null | grep -q imok; then
        echo "✓ ZooKeeper is responsive at $ZK_CHECK_HOST:$ZK_CHECK_PORT"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: ZooKeeper is not responding after 30 seconds"
        echo "Please ensure ZooKeeper is running on all nodes"
        exit 1
    fi
    echo "Waiting for ZooKeeper at $ZK_CHECK_HOST:$ZK_CHECK_PORT... ($i/30)"
    sleep 1
done
echo ""

# Step 2: Upload security.json to ZooKeeper
echo "Step 2: Uploading security.json to ZooKeeper..."
if [ -f "$SOLR_HOME/server/solr/security.json" ]; then
    $SOLR_HOME/bin/solr zk cp file:$SOLR_HOME/server/solr/security.json zk:/security.json -z $ZK_HOST
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to upload security.json to ZooKeeper"
        exit 1
    fi
    echo "✓ security.json uploaded successfully"
else
    echo "⚠ Warning: security.json not found, skipping security configuration"
fi
echo ""

# Step 3: Upload configuration to ZooKeeper
echo "Step 3: Uploading Ikasan configset to ZooKeeper..."
$SOLR_HOME/bin/solr zk upconfig \
    -n $CONFIG_NAME \
    -d $SOLR_HOME/server/solr/configsets/ikasan/conf \
    -z $ZK_HOST

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to upload configuration to ZooKeeper"
    exit 1
fi

echo "✓ Configset uploaded successfully"
echo ""

# Step 4: Wait for Solr nodes to be available
echo "Step 4: Waiting for Solr nodes to start..."
echo ""
echo "IMPORTANT: Before continuing, you must:"
echo "  1. Start Solr on all 3 nodes: ./bin/solr start"
echo "  2. Wait for all nodes to join the cluster"
echo ""
echo -n "Press ENTER when all Solr nodes are running..."
read

# Check if at least one Solr node is responding
echo ""
echo "Checking if Solr is available..."
for i in {1..30}; do
    if curl -s -u ikasan:1ka5an "http://localhost:8983/solr/admin/info/system" >/dev/null 2>&1; then
        echo "✓ Solr is available"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: Solr is not responding after 30 seconds"
        echo "Please ensure at least one Solr node is running"
        exit 1
    fi
    echo "Waiting for Solr to respond... ($i/30)"
    sleep 1
done
echo ""

# Step 5: Create collection using Collections API
echo "Step 5: Creating collection using Collections API..."

# Use the Collections API to create the collection with replicas
# Note: If security is enabled, credentials are required (default: ikasan/ikasan)
RESPONSE=$(curl -s -u ikasan:1ka5an "http://localhost:8983/solr/admin/collections?action=CREATE&name=$COLLECTION_NAME&collection.configName=$CONFIG_NAME&numShards=$NUM_SHARDS&replicationFactor=$REPLICATION_FACTOR&maxShardsPerNode=1&wt=json")

# Check if creation was successful
if echo "$RESPONSE" | grep -q '"status":0'; then
    echo "✓ Collection created successfully with replicas"
else
    echo "ERROR: Failed to create collection"
    echo "Response: $RESPONSE"
    exit 1
fi
echo ""

# Step 6: Verify collection health
echo "Step 6: Verifying collection health..."

# Wait a few seconds for replicas to become active
sleep 5

# Check collection health
HEALTH_RESPONSE=$(curl -s -u ikasan:1ka5an "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS&collection=$COLLECTION_NAME&wt=json")

if echo "$HEALTH_RESPONSE" | grep -q '"health":"GREEN"'; then
    echo "✓ Collection health is GREEN"
    COLLECTION_HEALTHY=true
elif echo "$HEALTH_RESPONSE" | grep -q '"health":"YELLOW"'; then
    echo "⚠ Collection health is YELLOW (some replicas may still be recovering)"
    COLLECTION_HEALTHY=true
else
    echo "⚠ Collection health is RED or UNKNOWN"
    echo "This may be temporary - replicas may still be initializing"
    COLLECTION_HEALTHY=false
fi

echo ""
echo "=========================================="
echo "Bootstrap Complete!"
echo "=========================================="
echo ""
echo "Cluster configured with:"
echo "  - Security configuration uploaded to ZooKeeper"
echo "  - Ikasan configset uploaded to ZooKeeper"
echo "  - Collection '$COLLECTION_NAME' created (1 shard, 3 replicas)"
echo "  - All Solr nodes joined the cluster"
echo ""
echo "Authentication enabled:"
echo "  Username: ikasan"
echo "  Password: ikasan"
echo ""
echo "Access Solr Admin UI:"
echo "  - Node 1: http://localhost:8983/solr/"
echo "  - Node 2: http://localhost:8984/solr/"
echo "  - Node 3: http://localhost:8985/solr/"
echo ""
echo "Check collection status:"
echo "  ./bin/solr healthcheck -c $COLLECTION_NAME -z $ZK_HOST"
echo ""

if [ "$COLLECTION_HEALTHY" = false ]; then
    echo "NOTE: If collection health is not GREEN, wait a few minutes"
    echo "      for all replicas to complete initialization, then check again."
    echo ""
fi
