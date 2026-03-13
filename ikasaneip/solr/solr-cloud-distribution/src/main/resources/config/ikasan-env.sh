#!/bin/bash
# Ikasan SolrCloud Environment Configuration
# Edit this file to customize data and log directory locations

# ===========================================
# Base Directories Configuration
# ===========================================

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Solr installation directory (parent of config directory)
# Change this value in this file to customize - does not respect environment variables
SOLR_INSTALL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
export SOLR_INSTALL_DIR

# Base directory for all Ikasan data and logs (relative to Solr installation)
# Change this value in this file to customize - does not respect environment variables
IKASAN_BASE_DIR="${SOLR_INSTALL_DIR}/data"
export IKASAN_BASE_DIR

# ===========================================
# Solr Configuration
# ===========================================

# Solr data directory (where indexes are stored)
SOLR_DATA_DIR="${IKASAN_BASE_DIR}/solr/data"
export SOLR_DATA_DIR

# Solr log directory
SOLR_LOG_DIR="${IKASAN_BASE_DIR}/solr/logs"
export SOLR_LOG_DIR

# Solr PID file directory
SOLR_PID_DIR="${IKASAN_BASE_DIR}/solr/pids"
export SOLR_PID_DIR

# ===========================================
# ZooKeeper Configuration
# ===========================================

# ZooKeeper data directory (where snapshots are stored)
ZK_DATA_DIR="${IKASAN_BASE_DIR}/zookeeper/data"
export ZK_DATA_DIR

# ZooKeeper transaction log directory
ZK_LOG_DIR="${IKASAN_BASE_DIR}/zookeeper/logs"
export ZK_LOG_DIR

# ZooKeeper PID file directory
ZK_PID_DIR="${IKASAN_BASE_DIR}/zookeeper/pids"
export ZK_PID_DIR

# ===========================================
# Network Configuration
# ===========================================

# ZooKeeper ensemble hosts (update with your actual hostnames)
# Default supports running all nodes on localhost with different ports
ZK_HOSTS="localhost:2181,localhost:2182,localhost:2183"
export ZK_HOSTS

# Host configuration for ZooKeeper ensemble
# When running on same host, use localhost for all; when separate hosts, use actual hostnames
HOST1="localhost"
HOST2="localhost"
HOST3="localhost"
export HOST1 HOST2 HOST3

# This node's hostname (update with actual hostname or IP)
SOLR_HOST="localhost"
export SOLR_HOST

# Note: Ports are configured per-node in solr.in.sh and zoo.cfg.template
# Node 1: Solr=8983, ZK Client=2181, ZK Peer=2888, ZK Election=3888
# Node 2: Solr=8984, ZK Client=2182, ZK Peer=2889, ZK Election=3889
# Node 3: Solr=8985, ZK Client=2183, ZK Peer=2890, ZK Election=3890

# ===========================================
# Memory Configuration
# ===========================================

# Solr JVM heap size
SOLR_HEAP="2g"
export SOLR_HEAP

# ZooKeeper JVM heap size
ZK_HEAP_SIZE="512m"
export ZK_HEAP_SIZE

# ===========================================
# Advanced Configuration
# ===========================================

# Solr GC tuning
GC_TUNE="-XX:+UseG1GC -XX:+PerfDisableSharedMem -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=250 -XX:+AlwaysPreTouch"
export GC_TUNE

# Additional Solr options
SOLR_OPTS="-Dsolr.autoSoftCommit.maxTime=3000"
export SOLR_OPTS

# ===========================================
# Do not modify below this line
# ===========================================

# Create required directories if they don't exist
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
                echo "Please create it manually with: sudo mkdir -p $dir && sudo chown \$(whoami) $dir"
                return 1
            fi
        fi
    done
    return 0
}

# Validate that required directories exist or can be created
validate_directories() {
    create_directories
}

# Export all configuration for use by other scripts
export_config() {
    echo "Ikasan Environment Configuration:"
    echo "  Base Directory: $IKASAN_BASE_DIR"
    echo "  Solr Data: $SOLR_DATA_DIR"
    echo "  Solr Logs: $SOLR_LOG_DIR"
    echo "  ZooKeeper Data: $ZK_DATA_DIR"
    echo "  ZooKeeper Logs: $ZK_LOG_DIR"
    echo "  ZooKeeper Hosts: $ZK_HOSTS"
    echo "  Solr Host: $SOLR_HOST"
}
