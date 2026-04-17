#!/bin/bash
# Ikasan SolrCloud Environment - Multi-Host Node 3
#
# This configuration is for running a 3-node SolrCloud cluster across 3 separate hosts.
# Each node runs on its own dedicated host.
#
# IMPORTANT: Update the NODE*_HOST values below with your actual hostnames or IP addresses
#
# To use this configuration:
# 1. Update hostnames below (node1.example.com, node2.example.com, node3.example.com)
# 2. Copy this file to ../../ikasan-env.sh (config/ikasan-env.sh) on the first host
# 3. Copy multihost-node2.env.sh to config/ikasan-env.sh on the second host
# 4. Copy multihost-node3.env.sh to config/ikasan-env.sh on the third host
# 5. On each host, run ./zookeeper-scripts/configure-zookeeper.sh
# 6. On each host, run ./zookeeper-scripts/setup-myid.sh
# 7. On each host, start ZooKeeper: ./zookeeper/bin/zkServer.sh start
# 8. On each host, start Solr: ./bin/solr start (solr.in.sh is already in bin/)

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

# For production multi-host, use absolute system paths
IKASAN_BASE_DIR="/opt/ikasan"
export IKASAN_BASE_DIR

# ===========================================
# Solr Configuration
# ===========================================

SOLR_DATA_DIR="${IKASAN_BASE_DIR}/solr/data"
export SOLR_DATA_DIR

SOLR_LOG_DIR="${IKASAN_BASE_DIR}/solr/logs"
export SOLR_LOG_DIR

SOLR_PID_DIR="${IKASAN_BASE_DIR}/solr/pids"
export SOLR_PID_DIR

# ===========================================
# ZooKeeper Configuration
# ===========================================

ZK_DATA_DIR="${IKASAN_BASE_DIR}/zookeeper/data"
export ZK_DATA_DIR

ZK_LOG_DIR="${IKASAN_BASE_DIR}/zookeeper/logs"
export ZK_LOG_DIR

ZK_PID_DIR="${IKASAN_BASE_DIR}/zookeeper/pids"
export ZK_PID_DIR

# ===========================================
# Network Configuration - Hostnames
# ===========================================

# *** UPDATE THESE WITH YOUR ACTUAL HOSTNAMES ***
NODE1_HOST="node1.example.com"  # This host
NODE2_HOST="node2.example.com"
NODE3_HOST="node3.example.com"
export NODE1_HOST NODE2_HOST NODE3_HOST

SOLR_HOST="${NODE3_HOST}"
export SOLR_HOST

# ===========================================
# Network Configuration - Solr Ports
# ===========================================

# All nodes can use the same Solr port on different hosts
NODE1_SOLR_PORT="8983"
NODE2_SOLR_PORT="8983"
NODE3_SOLR_PORT="8983"
export NODE1_SOLR_PORT NODE2_SOLR_PORT NODE3_SOLR_PORT

SOLR_PORT="${NODE3_SOLR_PORT}"
export SOLR_PORT

# ===========================================
# Network Configuration - ZooKeeper Ports
# ===========================================

# All nodes can use the same ZK ports on different hosts
NODE1_ZK_CLIENT_PORT="2181"
NODE2_ZK_CLIENT_PORT="2181"
NODE3_ZK_CLIENT_PORT="2181"
export NODE1_ZK_CLIENT_PORT NODE2_ZK_CLIENT_PORT NODE3_ZK_CLIENT_PORT

NODE1_ZK_PEER_PORT="2888"
NODE2_ZK_PEER_PORT="2888"
NODE3_ZK_PEER_PORT="2888"
export NODE1_ZK_PEER_PORT NODE2_ZK_PEER_PORT NODE3_ZK_PEER_PORT

NODE1_ZK_ELECTION_PORT="3888"
NODE2_ZK_ELECTION_PORT="3888"
NODE3_ZK_ELECTION_PORT="3888"
export NODE1_ZK_ELECTION_PORT NODE2_ZK_ELECTION_PORT NODE3_ZK_ELECTION_PORT

NODE1_ZK_ADMIN_PORT="8080"
NODE2_ZK_ADMIN_PORT="8080"
NODE3_ZK_ADMIN_PORT="8080"
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

# Production heap sizes
SOLR_HEAP="4g"
export SOLR_HEAP

ZK_HEAP_SIZE="1g"
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
                echo "Please create manually with: sudo mkdir -p $dir && sudo chown \$(whoami) $dir"
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
    echo "Ikasan SolrCloud - Multi-Host Node 3"
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
