![IKASAN](../../../../../developer/docs/quickstart-images/Ikasan-title-transparent.png)

# Ikasan SolrCloud Configuration Guide

This document explains how to configure data directories, log locations, ports, and other settings for your Ikasan SolrCloud deployment.

## Configuration System

The Ikasan SolrCloud distribution uses a **single unified distribution** configured through environment files.

### Configuration File

All configuration is centralized in `config/ikasan-env.sh`. This file is sourced by all scripts (Solr and ZooKeeper) to provide consistent configuration.

**Quick Start**: Copy a sample configuration:

```bash
# For localhost deployment (3 nodes on one machine)
cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh

# For multi-host deployment (3 separate servers)
cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh
```

Each node has its own copy of the distribution with its own `config/ikasan-env.sh` file.

## Configuration Options

### Base Directory

```bash
# Solr installation directory (automatically detected)
export SOLR_INSTALL_DIR="${SOLR_INSTALL_DIR:-<detected>}"

# Base directory for all Ikasan data and logs
export IKASAN_BASE_DIR="${IKASAN_BASE_DIR:-${SOLR_INSTALL_DIR}/data}"
```

**Defaults**:
- `SOLR_INSTALL_DIR`: Automatically detected from script location
- `IKASAN_BASE_DIR`: `${SOLR_INSTALL_DIR}/data` (relative to installation)

All other paths default to locations under this base directory. The default configuration keeps all data within the Solr installation directory for easy management.

### Solr Configuration

```bash
# Solr data directory (where indexes are stored)
export SOLR_DATA_DIR="${SOLR_DATA_DIR:-${IKASAN_BASE_DIR}/solr/data}"

# Solr log directory
export SOLR_LOG_DIR="${SOLR_LOG_DIR:-${IKASAN_BASE_DIR}/solr/logs}"

# Solr PID file directory
export SOLR_PID_DIR="${SOLR_PID_DIR:-${IKASAN_BASE_DIR}/solr/pids}"
```

**Defaults** (relative to installation):
- Data: `${SOLR_INSTALL_DIR}/data/solr/data`
- Logs: `${SOLR_INSTALL_DIR}/data/solr/logs`
- PIDs: `${SOLR_INSTALL_DIR}/data/solr/pids`

### ZooKeeper Configuration

```bash
# ZooKeeper data directory (where snapshots are stored)
export ZK_DATA_DIR="${ZK_DATA_DIR:-${IKASAN_BASE_DIR}/zookeeper/data}"

# ZooKeeper transaction log directory
export ZK_LOG_DIR="${ZK_LOG_DIR:-${IKASAN_BASE_DIR}/zookeeper/logs}"

# ZooKeeper PID file directory
export ZK_PID_DIR="${ZK_PID_DIR:-${IKASAN_BASE_DIR}/zookeeper/pids}"
```

**Defaults** (relative to installation):
- Data: `${SOLR_INSTALL_DIR}/data/zookeeper/data`
- Logs: `${SOLR_INSTALL_DIR}/data/zookeeper/logs`
- PIDs: `${SOLR_INSTALL_DIR}/data/zookeeper/pids`

### Network and Port Configuration

```bash
# Node identity (1, 2, or 3)
export NODE_ID="1"

# Hostnames for all nodes in the cluster
export NODE1_HOST="localhost"
export NODE2_HOST="localhost"
export NODE3_HOST="localhost"

# Solr ports for each node
export NODE1_SOLR_PORT="8983"
export NODE2_SOLR_PORT="8984"
export NODE3_SOLR_PORT="8985"

# ZooKeeper client ports
export NODE1_ZK_CLIENT_PORT="2181"
export NODE2_ZK_CLIENT_PORT="2182"
export NODE3_ZK_CLIENT_PORT="2183"

# ZooKeeper peer ports
export NODE1_ZK_PEER_PORT="2888"
export NODE2_ZK_PEER_PORT="2889"
export NODE3_ZK_PEER_PORT="2890"

# ZooKeeper election ports
export NODE1_ZK_ELECTION_PORT="3888"
export NODE2_ZK_ELECTION_PORT="3889"
export NODE3_ZK_ELECTION_PORT="3890"

# ZooKeeper admin server ports
export NODE1_ZK_ADMIN_PORT="8080"
export NODE2_ZK_ADMIN_PORT="8081"
export NODE3_ZK_ADMIN_PORT="8082"
```

**Localhost vs Multi-Host Deployment**:

| Deployment | Hostnames | Port Strategy |
|------------|-----------|---------------|
| **Localhost** | All nodes use `localhost` | Different ports per node to avoid conflicts |
| **Multi-Host** | Each node uses unique hostname | All nodes can use same ports (8983, 2181, etc.) |

### Memory Configuration

```bash
# Solr JVM heap size
SOLR_HEAP="2g"

# ZooKeeper JVM heap size
ZK_HEAP_SIZE="512m"
```

## Configuration Workflow

### Step 1: Edit ikasan-env.sh

On each node, edit `config/ikasan-env.sh`:

```bash
vi config/ikasan-env.sh
```

Update the settings according to your environment:

```bash
# Example: Custom data location on SSD
IKASAN_BASE_DIR="/data/ikasan"

# Example: For multi-host deployment, update host configuration
HOST1="solr1.example.com"
HOST2="solr2.example.com"
HOST3="solr3.example.com"
ZK_HOSTS="solr1.example.com:2181,solr2.example.com:2181,solr3.example.com:2181"

# Example: This node's hostname (different on each node)
SOLR_HOST="solr1.example.com"    # On node1
# SOLR_HOST="solr2.example.com"  # On node2
# SOLR_HOST="solr3.example.com"  # On node3

# Example: Increased memory for production
SOLR_HEAP="8g"
ZK_HEAP_SIZE="1g"
```

**Important**: Configuration values do not respect environment variables. Edit the values directly in `config/ikasan-env.sh` without using `${VAR:-default}` syntax.

### Step 2: Generate ZooKeeper Configuration

After editing `ikasan-env.sh`, generate the ZooKeeper configuration file:

```bash
./zookeeper-scripts/configure-zookeeper.sh
```

This script:
- Reads settings from `config/ikasan-env.sh`
- Generates `zookeeper/conf/zoo.cfg` from `zookeeper-configs/zoo.cfg.template`
- Substitutes all placeholders with actual values from your environment

**Output**:
```
Configuring ZooKeeper for Node 1...
  Source Template: zookeeper-configs/zoo.cfg.template
  Output File: zookeeper/conf/zoo.cfg

Configuration values:
  Node ID: 1
  ZK Data Dir: /data/ikasan/zookeeper/data
  ZK Log Dir: /data/ikasan/zookeeper/logs
  ...

✓ ZooKeeper configuration generated successfully
```

### Step 3: Create Directories

**Directories are automatically created** when you source `ikasan-env.sh` or start the services.

If you want to pre-create them manually or use custom locations:

```bash
# Using defaults (relative to installation directory)
mkdir -p data/solr/{data,logs,pids}
mkdir -p data/zookeeper/{data,logs,pids}

# Or if you customized IKASAN_BASE_DIR to an absolute path
sudo mkdir -p /data/ikasan/solr/{data,logs,pids}
sudo mkdir -p /data/ikasan/zookeeper/{data,logs,pids}

# Set ownership if needed
sudo chown -R $(whoami):$(id -gn) /data/ikasan
```

The configuration will automatically create missing directories when scripts are run.

## Common Configuration Scenarios

### Scenario 1: Single-Host Deployment (Default)

All three nodes running on localhost with different ports:

```bash
# Start from a sample
cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh

# Key settings in the sample:
NODE_ID="1"
NODE1_HOST="localhost"
NODE2_HOST="localhost"
NODE3_HOST="localhost"
NODE1_SOLR_PORT="8983"
NODE2_SOLR_PORT="8984"
NODE3_SOLR_PORT="8985"
```

Each node uses unique ports to avoid conflicts on the same host.

### Scenario 2: Multi-Host Deployment

Three nodes running on separate hosts:

```bash
# Start from a sample
cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh

# Edit to update hostnames
vi config/ikasan-env.sh

# Update these values:
NODE_ID="1"                           # Different on each node: 1, 2, or 3
NODE1_HOST="server1.example.com"      # Update with actual hostnames
NODE2_HOST="server2.example.com"
NODE3_HOST="server3.example.com"

# All nodes can use same ports (running on different hosts)
NODE1_SOLR_PORT="8983"
NODE2_SOLR_PORT="8983"
NODE3_SOLR_PORT="8983"
```

**Important**: Update `NODE_ID` to match the node number (1, 2, or 3) on each server.

### Scenario 3: All Data on Separate Disk

```bash
# config/ikasan-env.sh
IKASAN_BASE_DIR="/mnt/data-disk/ikasan"
```

This changes:
- Solr data → `/mnt/data-disk/ikasan/solr/data`
- ZooKeeper data → `/mnt/data-disk/ikasan/zookeeper/data`
- ZooKeeper logs → `/mnt/data-disk/ikasan/zookeeper/logs`

### Scenario 4: Separate Disks for Solr and ZooKeeper

```bash
# config/ikasan-env.sh
SOLR_DATA_DIR="/mnt/ssd1/solr/data"
ZK_DATA_DIR="/mnt/ssd2/zookeeper/data"
ZK_LOG_DIR="/mnt/ssd2/zookeeper/logs"
```

### Scenario 5: Production Memory Settings

```bash
# config/ikasan-env.sh
SOLR_HEAP="16g"           # 16GB for Solr
ZK_HEAP_SIZE="2g"         # 2GB for ZooKeeper
```

## Validation

After configuration, validate your settings:

```bash
# Source the configuration
source config/ikasan-env.sh

# Check configuration
echo "Base Directory: $IKASAN_BASE_DIR"
echo "Solr Data: $SOLR_DATA_DIR"
echo "ZooKeeper Data: $ZK_DATA_DIR"
echo "Solr Heap: $SOLR_HEAP"
```

## Configuration Precedence

Configuration is applied as follows:

1. **`config/ikasan-env.sh`** - Primary configuration source for all settings (ports, directories, memory, etc.)
2. **`bin/solr.in.sh`** - Sources `ikasan-env.sh` and applies Solr-specific configuration
3. **`zookeeper/conf/zoo.cfg`** - Generated from template using values from `ikasan-env.sh`

All configuration originates from `config/ikasan-env.sh`. The Solr configuration (`bin/solr.in.sh`) sources this file directly at startup, while the ZooKeeper configuration is generated from it via the `configure-zookeeper.sh` script.

## Troubleshooting

### Configuration not taking effect

**Problem**: Changed `ikasan-env.sh` but settings not applied.

**Solution**:
1. Regenerate ZooKeeper config: `./zookeeper-scripts/configure-zookeeper.sh`
2. Restart services to pick up changes

### Permission denied errors

**Problem**: Services can't write to configured directories.

**Solution**: Ensure directories exist and have correct ownership:
```bash
sudo chown -R $(whoami) $SOLR_DATA_DIR $ZK_DATA_DIR
```

### Path not found errors

**Problem**: Scripts can't find `ikasan-env.sh`.

**Solution**: Ensure `config/ikasan-env.sh` exists in the installation directory.

## Best Practices

1. **Use absolute paths** - Always use full paths, not relative paths
2. **Separate data and logs** - Consider using different disks for performance
3. **Plan for growth** - Allocate sufficient disk space for index growth
4. **Consistent configuration** - Use the same `IKASAN_BASE_DIR` across all nodes
5. **Version control** - Keep a copy of your `ikasan-env.sh` in version control
6. **Test changes** - Validate configuration before applying to production

## Reference: All Configurable Variables

### Directory Configuration (in `config/ikasan-env.sh`)

| Variable | Purpose | Default |
|----------|---------|---------|
| `SOLR_INSTALL_DIR` | Solr installation directory | Auto-detected |
| `IKASAN_BASE_DIR` | Base directory for all data | `${SOLR_INSTALL_DIR}/data` |
| `SOLR_DATA_DIR` | Solr index data | `${IKASAN_BASE_DIR}/solr/data` |
| `SOLR_LOG_DIR` | Solr logs | `${IKASAN_BASE_DIR}/solr/logs` |
| `SOLR_PID_DIR` | Solr PID files | `${IKASAN_BASE_DIR}/solr/pids` |
| `ZK_DATA_DIR` | ZooKeeper snapshots | `${IKASAN_BASE_DIR}/zookeeper/data` |
| `ZK_LOG_DIR` | ZooKeeper transaction logs | `${IKASAN_BASE_DIR}/zookeeper/logs` |
| `ZK_PID_DIR` | ZooKeeper PID files | `${IKASAN_BASE_DIR}/zookeeper/pids` |

### Network Configuration (in `config/ikasan-env.sh`)

| Variable | Purpose | Example Value |
|----------|---------|---------------|
| `NODE_ID` | This node's identity (1, 2, or 3) | `1` |
| `NODE1_HOST` | Node 1 hostname | `localhost` or `server1.example.com` |
| `NODE2_HOST` | Node 2 hostname | `localhost` or `server2.example.com` |
| `NODE3_HOST` | Node 3 hostname | `localhost` or `server3.example.com` |
| `NODE1_SOLR_PORT` | Node 1 Solr port | `8983` |
| `NODE2_SOLR_PORT` | Node 2 Solr port | `8984` (localhost) or `8983` (multi-host) |
| `NODE3_SOLR_PORT` | Node 3 Solr port | `8985` (localhost) or `8983` (multi-host) |
| `NODE1_ZK_CLIENT_PORT` | Node 1 ZK client port | `2181` |
| `NODE2_ZK_CLIENT_PORT` | Node 2 ZK client port | `2182` (localhost) or `2181` (multi-host) |
| `NODE3_ZK_CLIENT_PORT` | Node 3 ZK client port | `2183` (localhost) or `2181` (multi-host) |

### Memory Configuration (in `config/ikasan-env.sh`)

| Variable | Purpose | Default |
|----------|---------|---------|
| `SOLR_HEAP` | Solr JVM heap | `2g` |
| `ZK_HEAP_SIZE` | ZooKeeper JVM heap | `512m` |
| `GC_TUNE` | JVM GC tuning options | G1GC settings |
| `SOLR_OPTS` | Additional Solr JVM options | Auto-commit settings |

### Port Configuration Variables

All ports are configurable via `config/ikasan-env.sh`:

| Variable Pattern | Purpose | Example |
|------------------|---------|---------|
| `NODE{1,2,3}_SOLR_PORT` | Solr HTTP port | `8983`, `8984`, `8985` |
| `NODE{1,2,3}_ZK_CLIENT_PORT` | ZooKeeper client port | `2181`, `2182`, `2183` |
| `NODE{1,2,3}_ZK_PEER_PORT` | ZooKeeper peer communication | `2888`, `2889`, `2890` |
| `NODE{1,2,3}_ZK_ELECTION_PORT` | ZooKeeper leader election | `3888`, `3889`, `3890` |
| `NODE{1,2,3}_ZK_ADMIN_PORT` | ZooKeeper admin server | `8080`, `8081`, `8082` |

See sample files in `config/samples/` for complete port configurations.
