#!/bin/bash
# Ikasan ZooKeeper Management Script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ZK_HOME="$SCRIPT_DIR/.."

# Source environment configuration
if [ -f "$ZK_HOME/../config/ikasan-env.sh" ]; then
    source "$ZK_HOME/../config/ikasan-env.sh"
fi

# Use values from ikasan-env.sh, no fallback to environment variables
ZK_DATA_DIR="${ZK_DATA_DIR}"
ZK_LOG_DIR="${ZK_LOG_DIR}"
ZK_PID_DIR="${ZK_PID_DIR}"
ZK_PID_FILE="$ZK_PID_DIR/zookeeper.pid"
ZK_HEAP_SIZE="${ZK_HEAP_SIZE}"

# Java settings
JAVA_HOME="${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}"
JAVA="$JAVA_HOME/bin/java"

# ZooKeeper JVM settings
JVMFLAGS="-Xmx$ZK_HEAP_SIZE -Xms$ZK_HEAP_SIZE"

# Create directories if they don't exist
mkdir -p "$ZK_DATA_DIR"
mkdir -p "$ZK_LOG_DIR"
mkdir -p "$ZK_PID_DIR"

case "$1" in
start)
    echo "Starting ZooKeeper..."

    # Check if already running
    if [ -f "$ZK_PID_FILE" ]; then
        PID=$(cat "$ZK_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "ZooKeeper is already running (PID: $PID)"
            exit 1
        fi
    fi

    # Check for myid file
    if [ ! -f "$ZK_DATA_DIR/myid" ]; then
        echo "ERROR: $ZK_DATA_DIR/myid file not found!"
        echo "Please create this file with the node ID (1, 2, or 3)"
        exit 1
    fi

    # Start ZooKeeper
    cd "$ZK_HOME"
    nohup $JAVA \
        $JVMFLAGS \
        -Dzookeeper.log.dir="$ZK_LOG_DIR" \
        -Dzookeeper.log.file=zookeeper.log \
        -cp "$ZK_HOME/lib/*:$ZK_HOME/*" \
        org.apache.zookeeper.server.quorum.QuorumPeerMain \
        "$ZK_HOME/conf/zoo.cfg" \
        > "$ZK_LOG_DIR/zookeeper.out" 2>&1 &

    echo $! > "$ZK_PID_FILE"
    echo "ZooKeeper started (PID: $!)"
    echo "Logs: $ZK_LOG_DIR/zookeeper.log"
    ;;

stop)
    echo "Stopping ZooKeeper..."

    if [ ! -f "$ZK_PID_FILE" ]; then
        echo "ZooKeeper is not running (no PID file found)"
        exit 1
    fi

    PID=$(cat "$ZK_PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID

        # Wait for process to stop
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                echo "ZooKeeper stopped"
                rm -f "$ZK_PID_FILE"
                exit 0
            fi
            sleep 1
        done

        # Force kill if still running
        echo "ZooKeeper did not stop gracefully, forcing..."
        kill -9 $PID
        rm -f "$ZK_PID_FILE"
        echo "ZooKeeper stopped (forced)"
    else
        echo "ZooKeeper is not running"
        rm -f "$ZK_PID_FILE"
    fi
    ;;

restart)
    $0 stop
    sleep 2
    $0 start
    ;;

status)
    if [ -f "$ZK_PID_FILE" ]; then
        PID=$(cat "$ZK_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "ZooKeeper is running (PID: $PID)"

            # Try to get status from ZooKeeper
            echo "srvr" | nc localhost 2181 2>/dev/null
            exit 0
        else
            echo "ZooKeeper is not running (stale PID file)"
            exit 1
        fi
    else
        echo "ZooKeeper is not running"
        exit 1
    fi
    ;;

*)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac
