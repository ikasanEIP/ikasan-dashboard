#!/bin/bash
# Ikasan SolrCloud Environment - Localhost Node 3
#
# This configuration is for running a 3-node SolrCloud cluster on a single localhost machine.
# All nodes run on the same host but use different ports.
#
# To use this configuration:
# 1. Copy this file to ../../ikasan-env.sh (config/ikasan-env.sh)
# 2. Run ./zookeeper-scripts/configure-zookeeper.sh to generate zoo.cfg
# 3. Run ./zookeeper-scripts/setup-myid.sh to create ZooKeeper node ID
# 4. Start ZooKeeper: ./zookeeper/bin/zkServer.sh start
# 5. Start Solr: ./bin/solr start (solr.in.sh is already in bin/)

# ===========================================
# Node Identity
# ===========================================

NODE_ID="3"
export NODE_ID

# ===========================================
# Base Directories Configuration
# ===========================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOLR_INSTALL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
export SOLR_INSTALL_DIR

# For localhost testing, use relative data directory
IKASAN_BASE_DIR="${SOLR_INSTALL_DIR}/data"
export IKASAN_BASE_DIR

# ===========================================
# Solr Configuration
# ===========================================

SOLR_DATA_DIR="${IKASAN_BASE_DIR}/solr-node${NODE_ID}/data"
export SOLR_DATA_DIR

SOLR_LOG_DIR="${IKASAN_BASE_DIR}/solr-node${NODE_ID}/logs"
export SOLR_LOG_DIR

SOLR_PID_DIR="${IKASAN_BASE_DIR}/solr-node${NODE_ID}/pids"
export SOLR_PID_DIR

# ===========================================
# ZooKeeper Configuration
# ===========================================

ZK_DATA_DIR="${IKASAN_BASE_DIR}/zookeeper-node${NODE_ID}/data"
export ZK_DATA_DIR

ZK_LOG_DIR="${IKASAN_BASE_DIR}/zookeeper-node${NODE_ID}/logs"
export ZK_LOG_DIR

ZK_PID_DIR="${IKASAN_BASE_DIR}/zookeeper-node${NODE_ID}/pids"
export ZK_PID_DIR

# ===========================================
# Network Configuration - Hostnames
# ===========================================

# All nodes on localhost
NODE1_HOST="localhost"
NODE2_HOST="localhost"
NODE3_HOST="localhost"
export NODE1_HOST NODE2_HOST NODE3_HOST

SOLR_HOST="${NODE3_HOST}"
export SOLR_HOST

# ===========================================
# Network Configuration - Solr Ports
# ===========================================

# Different Solr ports for each node on localhost
NODE1_SOLR_PORT="8983"
NODE2_SOLR_PORT="8984"
NODE3_SOLR_PORT="8985"
export NODE1_SOLR_PORT NODE2_SOLR_PORT NODE3_SOLR_PORT

SOLR_PORT="${NODE3_SOLR_PORT}"
export SOLR_PORT

# ===========================================
# Network Configuration - ZooKeeper Ports
# ===========================================

# Different ZK client ports for each node on localhost
NODE1_ZK_CLIENT_PORT="2181"
NODE2_ZK_CLIENT_PORT="2182"
NODE3_ZK_CLIENT_PORT="2183"
export NODE1_ZK_CLIENT_PORT NODE2_ZK_CLIENT_PORT NODE3_ZK_CLIENT_PORT

# Different ZK peer ports for each node on localhost
NODE1_ZK_PEER_PORT="2888"
NODE2_ZK_PEER_PORT="2889"
NODE3_ZK_PEER_PORT="2890"
export NODE1_ZK_PEER_PORT NODE2_ZK_PEER_PORT NODE3_ZK_PEER_PORT

# Different ZK election ports for each node on localhost
NODE1_ZK_ELECTION_PORT="3888"
NODE2_ZK_ELECTION_PORT="3889"
NODE3_ZK_ELECTION_PORT="3890"
export NODE1_ZK_ELECTION_PORT NODE2_ZK_ELECTION_PORT NODE3_ZK_ELECTION_PORT

# Different ZK admin ports for each node on localhost
NODE1_ZK_ADMIN_PORT="8080"
NODE2_ZK_ADMIN_PORT="8081"
NODE3_ZK_ADMIN_PORT="8082"
export NODE1_ZK_ADMIN_PORT NODE2_ZK_ADMIN_PORT NODE3_ZK_ADMIN_PORT

# This node's ports (Node 1)
ZK_CLIENT_PORT="${NODE3_ZK_CLIENT_PORT}"
ZK_PEER_PORT="${NODE3_ZK_PEER_PORT}"
ZK_ELECTION_PORT="${NODE3_ZK_ELECTION_PORT}"
ZK_ADMIN_PORT="${NODE3_ZK_ADMIN_PORT}"
export ZK_CLIENT_PORT ZK_PEER_PORT ZK_ELECTION_PORT ZK_ADMIN_PORT

# ZooKeeper ensemble connection string
ZK_HOSTS="${NODE1_HOST}:${NODE1_ZK_CLIENT_PORT},${NODE2_HOST}:${NODE2_ZK_CLIENT_PORT},${NODE3_HOST}:${NODE3_ZK_CLIENT_PORT}"
export ZK_HOSTS

# ===========================================
# ZooKeeper Ensemble Bindings
# ===========================================

# Node 1 binds to 0.0.0.0 for its own peer communication
NODE1_BINDING="${NODE1_HOST}"
NODE2_BINDING="${NODE2_HOST}"
NODE3_BINDING="0.0.0.0"
export NODE1_BINDING NODE2_BINDING NODE3_BINDING

# ===========================================
# Memory Configuration
# ===========================================

SOLR_HEAP="2g"
export SOLR_HEAP

ZK_HEAP_SIZE="512m"
export ZK_HEAP_SIZE

# ===========================================
# Advanced Configuration
# ===========================================

GC_TUNE="-XX:+UseG1GC -XX:+PerfDisableSharedMem -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=250 -XX:+AlwaysPreTouch"
export GC_TUNE

SOLR_OPTS="-Dsolr.autoSoftCommit.maxTime=3000"
export SOLR_OPTS

# ===========================================
# Utility Functions
# ===========================================

create_directories() {
    local dirs=(
        "$SOLR_DATA_DIR"
        "$SOLR_LOG_DIR"
        "$SOLR_PID_DIR"
        "$ZK_DATA_DIR"
        "$ZK_LOG_DIR"
        "$ZK_PID_DIR"
    )

    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            echo "Creating directory: $dir"
            mkdir -p "$dir" 2>/dev/null
            if [ $? -ne 0 ]; then
                echo "ERROR: Failed to create directory: $dir"
                return 1
            fi
        fi
    done
    return 0
}

validate_directories() {
    create_directories
}

show_config() {
    echo "========================================"
    echo "Ikasan SolrCloud - Localhost Node 3"
    echo "========================================"
    echo ""
    echo "Node Information:"
    echo "  Node ID: $NODE_ID"
    echo "  Host: $SOLR_HOST"
    echo ""
    echo "Solr:"
    echo "  Port: $SOLR_PORT"
    echo "  URL: http://${SOLR_HOST}:${SOLR_PORT}/solr"
    echo ""
    echo "ZooKeeper:"
    echo "  Client Port: $ZK_CLIENT_PORT"
    echo "  Admin: http://${SOLR_HOST}:${ZK_ADMIN_PORT}"
    echo ""
    echo "Ensemble:"
    echo "  ${NODE1_HOST}:${NODE1_SOLR_PORT} / ZK:${NODE1_ZK_CLIENT_PORT}"
    echo "  ${NODE2_HOST}:${NODE2_SOLR_PORT} / ZK:${NODE2_ZK_CLIENT_PORT}"
    echo "  ${NODE3_HOST}:${NODE3_SOLR_PORT} / ZK:${NODE3_ZK_CLIENT_PORT}"
    echo "========================================"
}

if [ "${1}" = "show" ]; then
    show_config
fi
