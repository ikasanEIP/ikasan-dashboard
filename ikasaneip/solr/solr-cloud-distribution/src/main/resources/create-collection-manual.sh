#!/bin/bash
# Manual collection creation script
# Use this only if automatic collection creation didn't work

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration - update these for your environment
ZK_HOST="${ZK_HOST:-host1:2181,host2:2181,host3:2181}"
SOLR_URL="${SOLR_URL:-http://localhost:8983/solr}"
COLLECTION_NAME="ikasan"
NUM_SHARDS=1
REPLICATION_FACTOR=3
CONFIG_NAME="ikasan"

echo "Manual Collection Creation"
echo "=========================="
echo ""

# First ensure config is uploaded
echo "Uploading configuration..."
$SCRIPT_DIR/bin/solr zk upconfig \
    -n $CONFIG_NAME \
    -d $SCRIPT_DIR/server/solr/configsets/ikasan/conf \
    -z $ZK_HOST

# Use Solr's Collections API via local command
echo "Creating collection using Solr CLI..."
$SCRIPT_DIR/bin/solr create -c $COLLECTION_NAME \
    -d $CONFIG_NAME \
    -s $NUM_SHARDS \
    -rf $REPLICATION_FACTOR

echo ""
echo "Collection created successfully!"
echo "Access at: $SOLR_URL/$COLLECTION_NAME"
