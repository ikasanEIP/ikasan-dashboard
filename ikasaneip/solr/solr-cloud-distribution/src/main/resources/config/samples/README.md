![IKASAN](../../../../../../../developer/docs/quickstart-images/Ikasan-title-transparent.png)

# Ikasan SolrCloud Configuration Samples

This directory contains sample environment configuration files for deploying Ikasan SolrCloud clusters.

## Overview

The Ikasan SolrCloud distribution uses a **single unified distribution** that can be configured for any node through environment files.

This eliminates the need for separate distributions per node.

## Quick Start

### Localhost Deployment (3 nodes on one machine)

For testing or development on a single machine:

```bash
# For Node 1
cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh

# For Node 2 (in a separate copy of the distribution)
cp config/samples/localhost/localhost-node2.env.sh config/ikasan-env.sh

# For Node 3 (in a separate copy of the distribution)
cp config/samples/localhost/localhost-node3.env.sh config/ikasan-env.sh
```

### Multi-Host Deployment (3 separate servers)

For production deployment across multiple hosts:

1. **On each host**, copy the appropriate sample file:
   ```bash
   # On server 1
   cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh

   # On server 2
   cp config/samples/multihost/multihost-node2.env.sh config/ikasan-env.sh

   # On server 3
   cp config/samples/multihost/multihost-node3.env.sh config/ikasan-env.sh
   ```

2. **Edit the hostnames** in `config/ikasan-env.sh` on each host:
   ```bash
   NODE1_HOST="server1.example.com"  # Update with actual hostnames
   NODE2_HOST="server2.example.com"
   NODE3_HOST="server3.example.com"
   ```

## Configuration Files

### Localhost Samples

| File | Node ID | Solr Port | ZK Client Port | Use Case |
|------|---------|-----------|----------------|----------|
| `localhost-node1.env.sh` | 1 | 8983 | 2181 | Single host, node 1 |
| `localhost-node2.env.sh` | 2 | 8984 | 2182 | Single host, node 2 |
| `localhost-node3.env.sh` | 3 | 8985 | 2183 | Single host, node 3 |

**Characteristics:**
- All nodes use `localhost` as hostname
- Different ports for each service to avoid conflicts
- Data directories are node-specific: `data/solr-node1`, `data/solr-node2`, etc.

### Multi-Host Samples

| File | Node ID | Solr Port | ZK Client Port | Use Case |
|------|---------|-----------|----------------|----------|
| `multihost-node1.env.sh` | 1 | 8983 | 2181 | Separate host, node 1 |
| `multihost-node2.env.sh` | 2 | 8983 | 2181 | Separate host, node 2 |
| `multihost-node3.env.sh` | 3 | 8983 | 2181 | Separate host, node 3 |

**Characteristics:**
- Each node on a different host (update `NODE*_HOST` values)
- Same ports can be used on each host (no conflicts)
- Production-ready paths: `/opt/ikasan`
- Larger heap sizes for production

## Port Configuration

### Localhost Deployment Ports

Each node uses different ports to avoid conflicts:

| Service | Node 1 | Node 2 | Node 3 |
|---------|--------|--------|--------|
| **Solr HTTP** | 8983 | 8984 | 8985 |
| **ZK Client** | 2181 | 2182 | 2183 |
| **ZK Peer** | 2888 | 2889 | 2890 |
| **ZK Election** | 3888 | 3889 | 3890 |
| **ZK Admin** | 8080 | 8081 | 8082 |

### Multi-Host Deployment Ports

All nodes can use the same ports (running on different hosts):

| Service | All Nodes |
|---------|-----------|
| **Solr HTTP** | 8983 |
| **ZK Client** | 2181 |
| **ZK Peer** | 2888 |
| **ZK Election** | 3888 |
| **ZK Admin** | 8080 |

## Key Configuration Parameters

### Node Identity
```bash
NODE_ID="1"  # Must be 1, 2, or 3 (unique per node)
```

### Hostnames
```bash
# Localhost deployment
NODE1_HOST="localhost"
NODE2_HOST="localhost"
NODE3_HOST="localhost"

# Multi-host deployment
NODE1_HOST="server1.example.com"
NODE2_HOST="server2.example.com"
NODE3_HOST="server3.example.com"
```

### Data Directories
```bash
# Localhost (relative paths)
IKASAN_BASE_DIR="${SOLR_INSTALL_DIR}/data"

# Multi-host (absolute paths)
IKASAN_BASE_DIR="/opt/ikasan"
```

### Memory Settings
```bash
# Development/Testing
SOLR_HEAP="2g"
ZK_HEAP_SIZE="512m"

# Production
SOLR_HEAP="4g"  # or higher
ZK_HEAP_SIZE="1g"
```

## Deployment Steps

### 1. Choose and Copy Configuration

```bash
# For localhost
cp config/samples/localhost/localhost-node1.env.sh config/ikasan-env.sh

# For multi-host
cp config/samples/multihost/multihost-node1.env.sh config/ikasan-env.sh
```

### 2. Edit Configuration (if needed)

```bash
# Edit hostnames for multi-host deployment
vi config/ikasan-env.sh

# View configuration
source config/ikasan-env.sh && show_config
```

### 3. Generate ZooKeeper Configuration

```bash
./zookeeper-scripts/configure-zookeeper.sh
```

This creates `zookeeper/conf/zoo.cfg` from the template using your environment settings.

### 4. Setup ZooKeeper Node ID

```bash
./zookeeper-scripts/setup-myid.sh
```

This creates the `myid` file in your ZooKeeper data directory.

### 5. Create Required Directories

```bash
source config/ikasan-env.sh && create_directories
```

### 6. Start Services

```bash
# Start ZooKeeper
./zookeeper/bin/zkServer.sh start

# Start Solr (solr.in.sh is already in place)
./bin/solr start
```

**Note**: Both `solr.in.sh` and `zkServer.sh` are already in their correct locations in the distribution. No manual copying is required.

## Customization

You can customize any configuration parameter by:

1. Copying a sample file to `config/ikasan-env.sh`
2. Editing the values in `ikasan-env.sh`
3. Re-running `configure-zookeeper.sh` to regenerate configs

### Common Customizations

**Change data directories:**
```bash
IKASAN_BASE_DIR="/custom/path/ikasan"
```

**Adjust memory:**
```bash
SOLR_HEAP="8g"
ZK_HEAP_SIZE="2g"
```

**Change ports (localhost only):**
```bash
NODE1_SOLR_PORT="9983"
NODE1_ZK_CLIENT_PORT="3181"
```

## Troubleshooting

### Configuration Validation

Check your configuration:
```bash
source config/ikasan-env.sh && show_config
```

### View Generated Files

```bash
# ZooKeeper configuration
cat zookeeper/conf/zoo.cfg

# ZooKeeper node ID
cat $ZK_DATA_DIR/myid

# Solr configuration
cat $SOLR_INSTALL_DIR/bin/solr.in.sh
```

### Common Issues

**Error: "NODE_ID not set"**
- Make sure you've copied a sample to `config/ikasan-env.sh`
- Verify `NODE_ID="1"` is set in the file

**Error: "Failed to create directory"**
- Check permissions on `IKASAN_BASE_DIR`
- Use `sudo` if needed: `sudo mkdir -p /opt/ikasan && sudo chown $USER /opt/ikasan`

**ZooKeeper won't start**
- Verify `myid` file exists: `cat $ZK_DATA_DIR/myid`
- Check port conflicts: `lsof -i :2181`
- Review logs in `$ZK_LOG_DIR`

## Architecture

The new configuration system works as follows:

1. **Single Distribution**: One distribution package for all nodes
2. **Environment-Based**: Node identity and ports defined in `ikasan-env.sh`
3. **Template-Based**: Configuration files generated from templates
4. **Deployment Flexible**: Same distribution works for localhost or multi-host

### File Structure

```
solr-cloud-distribution/
├── config/
│   ├── ikasan-env.sh               # Your configuration (create from samples)
│   └── samples/                     # Pre-configured samples
│       ├── localhost/
│       │   ├── localhost-node1.env.sh
│       │   ├── localhost-node2.env.sh
│       │   └── localhost-node3.env.sh
│       ├── multihost/
│       │   ├── multihost-node1.env.sh
│       │   ├── multihost-node2.env.sh
│       │   └── multihost-node3.env.sh
│       └── README.md (this file)
├── node-configs/
│   └── solr.in.sh                  # Solr config (sources ikasan-env.sh)
├── zookeeper-configs/
│   └── zoo.cfg.template            # ZooKeeper config template
└── zookeeper-scripts/
    ├── configure-zookeeper.sh      # Generates zoo.cfg
    ├── setup-myid.sh               # Creates myid file
    └── zkServer.sh                 # ZooKeeper start/stop
```

## Migration from Old Structure

If you're migrating from the old node-specific structure:

1. The old `node-configs/node1/`, `node-configs/node2/`, etc. directories are removed
2. Use `node-configs/solr.in.sh` for all nodes (sources environment directly)
3. The old `zookeeper-configs/node1/`, etc. directories are removed
4. Use `zookeeper-configs/zoo.cfg.template` for all nodes
5. All node-specific settings now come from `config/ikasan-env.sh`

## See Also

- `../../DEPLOYMENT.md` - Deployment guide
- `../../CONFIGURATION.md` - Configuration reference
