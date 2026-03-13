![IKASAN](../../../../../developer/docs/quickstart-images/Ikasan-title-transparent.png)

# Ikasan SolrCloud 3-Node Cluster Deployment Guide

This distribution creates a complete SolrCloud cluster with:
- **3 separate nodes** (can run on same host or different hosts)
- **1 shard** with **3 replicas** (one on each node)
- **Bundled ZooKeeper ensemble** (3 nodes for high availability)
- **Full configuration** included in each node's zip file
- **No manual API calls** required for collection creation
- **Default configuration supports single-host deployment** for development/testing

## Architecture

### Single-Host Deployment (Default)

```
localhost
┌──────────────────────────────────────────────────────────┐
│  Solr Node 1        Solr Node 2        Solr Node 3       │
│  Port: 8983         Port: 8984         Port: 8985        │
│  Replica 1          Replica 2          Replica 3         │
│  (Leader)                                                 │
│                                                           │
│  ZooKeeper 1        ZooKeeper 2        ZooKeeper 3       │
│  Port: 2181         Port: 2182         Port: 2183        │
│  Peer: 2888         Peer: 2889         Peer: 2890        │
│  Elect: 3888        Elect: 3889        Elect: 3890       │
│  Admin: 8080        Admin: 8081        Admin: 8082       │
│  (myid: 1)          (myid: 2)          (myid: 3)         │
└──────────────────────────────────────────────────────────┘
```

### Multi-Host Deployment

```
Host 1                  Host 2                  Host 3
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Solr Node 1    │     │  Solr Node 2    │     │  Solr Node 3    │
│  Port: 8983     │◄────┤  Port: 8983     │────►│  Port: 8983     │
│  Replica 1      │     │  Replica 2      │     │  Replica 3      │
│  (Leader)       │     │                 │     │                 │
│                 │     │                 │     │                 │
│  ZooKeeper 1    │     │  ZooKeeper 2    │     │  ZooKeeper 3    │
│  Port: 2181     │◄────┤  Port: 2181     │────►│  Port: 2181     │
│  (myid: 1)      │     │  (myid: 2)      │     │  (myid: 3)      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Distribution Contents

After building, you'll have 3 separate zip files:
- `ikasan-solr-cloud-node1-{version}.zip` - Node 1
- `ikasan-solr-cloud-node2-{version}.zip` - Node 2
- `ikasan-solr-cloud-node3-{version}.zip` - Node 3

**Note**: Each node can be deployed on the same host (default) or different hosts.

Each zip contains:
- **Complete Solr installation** (version 9.10.1)
- **Complete ZooKeeper installation** (version 3.9.3)
- **Ikasan schema** and configuration in configsets
- **Node-specific configuration** files for both Solr and ZooKeeper
- **Startup and management scripts**
- **Complete documentation**

## Prerequisites

### Single-Host Deployment (Default)
- Java 11 or later installed
- Minimum 7GB RAM available (6GB for 3 Solr nodes, 1.5GB for 3 ZooKeeper nodes)
- Disk space: 30GB+ for installation and data
- Available ports: 8983-8985, 2181-2183, 2888-2890, 3888-3890, 8080-8082

### Multi-Host Deployment
- Java 11 or later installed on each host
- Minimum 3GB RAM per host (2GB for Solr, 512MB for ZooKeeper)
- Disk space: 10GB+ per host for installation and data
- Network connectivity between all hosts on the following ports:
  - **8983** - Solr HTTP
  - **2181** - ZooKeeper client connections
  - **2888** - ZooKeeper peer communication
  - **3888** - ZooKeeper leader election

### Firewall Configuration (Multi-Host Only)
Ensure the following ports are open between all three hosts:
```bash
# Example firewall rules (adjust for your firewall)
# Allow Solr
firewall-cmd --permanent --add-port=8983/tcp

# Allow ZooKeeper
firewall-cmd --permanent --add-port=2181/tcp
firewall-cmd --permanent --add-port=2888/tcp
firewall-cmd --permanent --add-port=3888/tcp

firewall-cmd --reload
```

## Deployment Steps

Choose your deployment type:

### Option A: Single-Host Deployment (Default - No Configuration Needed)

**1. Extract all three nodes to the same host:**

```bash
# Extract all nodes
unzip ikasan-solr-cloud-node1-{version}.zip -d /opt/
unzip ikasan-solr-cloud-node2-{version}.zip -d /opt/
unzip ikasan-solr-cloud-node3-{version}.zip -d /opt/
```

**2. Skip to Step 4 (Setup ZooKeeper myid Files)**

The default configuration is ready for single-host deployment with unique ports for each node.

### Option B: Multi-Host Deployment

**1. Deploy to each host:**

```bash
# Host 1
unzip ikasan-solr-cloud-node1-{version}.zip -d /opt/
cd /opt/ikasan-solr-node1

# Host 2
unzip ikasan-solr-cloud-node2-{version}.zip -d /opt/
cd /opt/ikasan-solr-node2

# Host 3
unzip ikasan-solr-cloud-node3-{version}.zip -d /opt/
cd /opt/ikasan-solr-node3
```

**2. Configure for multi-host deployment:**

Edit `config/ikasan-env.sh` on **all nodes** with your actual hostnames:

```bash
vi config/ikasan-env.sh

# Update these values:
HOST1="solr1.example.com"
HOST2="solr2.example.com"
HOST3="solr3.example.com"
ZK_HOSTS="solr1.example.com:2181,solr2.example.com:2181,solr3.example.com:2181"
```

Update `SOLR_HOST` to be node-specific:
```bash
# On node1:
SOLR_HOST="solr1.example.com"

# On node2:
SOLR_HOST="solr2.example.com"

# On node3:
SOLR_HOST="solr3.example.com"
```

**3. Generate ZooKeeper configuration:**

On each node, run:
```bash
./zookeeper/bin/configure-zookeeper.sh
```

### 3. Configure Environment (Optional)

By default, all data directories are created relative to the installation directory under `data/`.

If you want to customize locations, edit `config/ikasan-env.sh` on each node:

```bash
vi config/ikasan-env.sh

# Example: Use a different base directory
export IKASAN_BASE_DIR="/mnt/data/ikasan"
```

**Directories are automatically created** when you start the services. See CONFIGURATION.md for details.

### 4. Setup ZooKeeper myid Files

On each host, run the setup script with the appropriate node ID:

**Host1:**
```bash
./zookeeper/bin/setup-myid.sh 1
```

**Host2:**
```bash
./zookeeper/bin/setup-myid.sh 2
```

**Host3:**
```bash
./zookeeper/bin/setup-myid.sh 3
```

### 5. Start ZooKeeper Ensemble

Start ZooKeeper on **all 3 hosts** (can be done in parallel):

**All Hosts:**
```bash
./zookeeper/bin/zkServer.sh start
```

Verify ZooKeeper is running:
```bash
./zookeeper/bin/zkServer.sh status
```

You should see one leader and two followers.

### 6. Bootstrap the Cluster

On **host1** only, upload the Ikasan configuration and pre-create the collection in ZooKeeper:

**IMPORTANT: Run this BEFORE starting any Solr nodes**

```bash
cd /opt/ikasan-solr-node1
./bootstrap-cluster.sh
```

This script:
- Uploads the Ikasan configset to ZooKeeper
- **Pre-creates the 'ikasan' collection** (1 shard, 3 replicas)
- Verifies ZooKeeper connectivity
- Verifies collection definition is in ZooKeeper

### 7. Start Solr Nodes

Start Solr on **all 3 hosts** (can be done in parallel):

**All Hosts:**
```bash
./bin/solr start
```

**The collection will be automatically created** when the Solr nodes start. They will:
- Discover the pre-defined collection in ZooKeeper
- Automatically create replicas according to the configuration
- Elect a leader for the shard
- Distribute the 3 replicas across the 3 nodes

Check Solr status:
```bash
./bin/solr status
```

### 8. Verify Deployment

Check cluster health:
```bash
./bin/solr healthcheck -c ikasan
```

Check ZooKeeper ensemble:
```bash
echo stat | nc localhost 2181
```

Access Solr Admin UI on any node:
- http://host1:8983/solr/#/~cloud
- http://host2:8983/solr/#/~cloud
- http://host3:8983/solr/#/~cloud

## Management Operations

### ZooKeeper Operations

**Start ZooKeeper:**
```bash
./zookeeper/bin/zkServer.sh start
```

**Stop ZooKeeper:**
```bash
./zookeeper/bin/zkServer.sh stop
```

**Check ZooKeeper Status:**
```bash
./zookeeper/bin/zkServer.sh status
```

**Check if ZooKeeper is responding:**
```bash
echo ruok | nc localhost 2181
# Should return: imok
```

### Solr Operations

**Start Solr:**
```bash
./bin/solr start
```

**Stop Solr:**
```bash
./bin/solr stop
```

**Restart Solr:**
```bash
./bin/solr restart
```

**Check Solr Status:**
```bash
./bin/solr status
```

**Check Collection Health:**
```bash
./bin/solr healthcheck -c ikasan
```

### View Logs

**Solr Logs:**
```bash
tail -f data/solr/logs/solr.log
```

**ZooKeeper Logs:**
```bash
tail -f data/zookeeper/logs/zookeeper.log
```

## Startup Order

**Recommended startup sequence:**

1. Start ZooKeeper on all 3 nodes (can be parallel)
2. Wait for ZooKeeper ensemble to elect a leader (~30 seconds)
3. Start Solr on all 3 nodes (can be parallel)
4. Verify cluster health

**Shutdown sequence:**

1. Stop Solr on all nodes
2. Stop ZooKeeper on all nodes

## Troubleshooting

### ZooKeeper Won't Start

**Check myid file exists:**
```bash
cat data/zookeeper/data/myid
```

**Check zoo.cfg configuration:**
```bash
cat zookeeper/conf/zoo.cfg
```

**Check ZooKeeper logs:**
```bash
tail -100 data/zookeeper/logs/zookeeper.log
```

**Verify network connectivity:**
```bash
# From each host, check connectivity to other hosts
nc -zv host1 2181
nc -zv host2 2181
nc -zv host3 2181
```

### Solr Won't Start

**Check ZooKeeper connectivity:**
```bash
echo ruok | nc host1 2181
echo ruok | nc host2 2181
echo ruok | nc host3 2181
```

**Check Solr logs:**
```bash
tail -100 data/solr/logs/solr.log
```

**Verify Java version:**
```bash
java -version
# Should be 11 or later
```

### Collection Not Creating

**Verify configset in ZooKeeper:**
```bash
./bin/solr zk ls /configs -z host1:2181,host2:2181,host3:2181
```

**Re-upload configset if needed:**
```bash
./bin/solr zk upconfig -n ikasan \
    -d server/solr/configsets/ikasan/conf \
    -z host1:2181,host2:2181,host3:2181
```

**Check live nodes:**
```bash
./bin/solr zk ls /live_nodes -z host1:2181,host2:2181,host3:2181
```

### Split Brain Detection

**Check ZooKeeper ensemble status on all nodes:**
```bash
# Run on each host
./zookeeper/bin/zkServer.sh status
```

You should have:
- Exactly 1 leader
- Exactly 2 followers

If you have multiple leaders or all followers, you have split brain. To recover:

1. Stop all ZooKeeper nodes
2. Stop all Solr nodes
3. Clear ZooKeeper data (CAUTION: this removes all cluster state):
   ```bash
   rm -rf data/zookeeper/data/*
   rm -rf data/zookeeper/logs/*
   ```
4. Recreate myid files on each node
5. Start ZooKeeper ensemble
6. Re-bootstrap cluster
7. Start Solr nodes
8. Recreate collection

## High Availability

With this 3-node configuration:

**ZooKeeper:**
- Can tolerate **1 node failure** without losing quorum
- 2 out of 3 nodes must be running for writes
- All operations continue if 2+ nodes are healthy

**Solr:**
- Can tolerate **1 node failure** without data loss
- Automatic failover when leader goes down
- New leader elected from remaining replicas
- Queries distributed across available replicas

**Data Availability:**
- 1 node down: 100% available (2 replicas serving)
- 2 nodes down: Read-only mode (1 replica serving)
- 3 nodes down: Cluster offline

## Production Recommendations

1. **Resources**: Allocate 4-8GB RAM for Solr, 1-2GB for ZooKeeper
2. **Disk**: Use SSDs for better performance
3. **Network**: Ensure low latency (<1ms) between nodes
4. **Monitoring**: Set up monitoring for both Solr and ZooKeeper
5. **Backups**: Regular backups using Solr Backup API
6. **Security**: Configure authentication in `security.json`
7. **OS Tuning**: Increase file descriptors, disable swap
8. **Dedicated Hosts**: Don't co-locate with other heavy services

## Port Reference

| Port | Service | Purpose |
|------|---------|---------|
| 8983 | Solr | HTTP API and Admin UI |
| 2181 | ZooKeeper | Client connections |
| 2888 | ZooKeeper | Peer communication |
| 3888 | ZooKeeper | Leader election |
| 8080 | ZooKeeper | Admin server (optional) |

## Support

For issues specific to:
- **Ikasan integration**: Consult Ikasan documentation
- **SolrCloud**: https://solr.apache.org/guide/solr/latest/
- **ZooKeeper**: https://zookeeper.apache.org/doc/current/
