# SolrCloud Migration Guide

## Table of Contents
1. [Current Standalone Architecture](#current-standalone-architecture)
2. [SolrCloud Architecture Overview](#solrcloud-architecture-overview)
3. [Data Replication in SolrCloud](#data-replication-in-solrcloud)
4. [Migration Requirements](#migration-requirements)
5. [Code Changes Required](#code-changes-required)
6. [Data Migration Steps](#data-migration-steps)
7. [Testing & Validation](#testing--validation)
8. [Rollback Plan](#rollback-plan)

---

## Current Standalone Architecture

### Overview
The Ikasan platform currently uses Apache Solr in **standalone mode**, which means:
- Single Solr instance handling all indexing and search operations
- Direct HTTP client connections via `Http2SolrClient`
- No automatic failover or high availability
- All data resides on a single node

### Current Implementation Details

**Location**: `ikasaneip/solr/solr-client/`

**Key Classes**:
- `SolrDaoBase.java` - Base class for all Solr DAO operations
- `SolrClientAutoConfiguration.java` - Spring Boot auto-configuration

**Current Connection Setup** (`SolrDaoBase.java:112-120`):
```java
@Override
public void initStandalone(String solrCloudUrl, int daysToKeep,
                           int socketTimeoutMilli, int connectionTimeoutMilli)
{
    solrClient = new Http2SolrClient.Builder(solrCloudUrl)
        .withConnectionTimeout(connectionTimeoutMilli, TimeUnit.MILLISECONDS)
        .build();

    this.daysToKeep = daysToKeep;
}
```

**Configuration Properties**:
```properties
solr.url=http://localhost:8983/solr
solr.username=<username>
solr.password=<password>
solr.retention.days=30
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000
```

**Collections Used**:
- `ikasan` - Default collection name (hardcoded in various places)

### Limitations of Current Setup
- **No High Availability**: Single point of failure
- **No Horizontal Scaling**: Cannot distribute load across multiple nodes
- **No Automatic Failover**: Manual intervention required if Solr instance fails
- **Limited Performance**: All queries and indexing operations handled by one instance
- **No Data Redundancy**: Data loss if the single node fails

---

## SolrCloud Architecture Overview

### What is SolrCloud?
SolrCloud is Apache Solr's distributed architecture that provides:
- **High Availability**: Multiple replicas ensure service continuity
- **Fault Tolerance**: Automatic failover when nodes fail
- **Horizontal Scalability**: Add nodes to handle increased load
- **Near Real-Time Replication**: Data replicated across nodes almost immediately
- **Distributed Indexing**: Index operations distributed across shards
- **Load Balancing**: Query routing across healthy replicas

### Core Components

#### 1. ZooKeeper Ensemble
- **Purpose**: Cluster coordination, configuration management, leader election
- **Recommended Setup**: 3-5 nodes (must be odd number)
- **Responsibilities**:
  - Maintains cluster state
  - Stores collection configurations
  - Manages shard leader election
  - Provides distributed locking

#### 2. Solr Nodes
- **Purpose**: Store data and handle search/indexing operations
- **Recommended Setup**: Minimum 2-3 nodes for production
- **Each Node Can**:
  - Host shard replicas
  - Handle both indexing and query operations
  - Act as a coordinator for distributed queries

#### 3. Collections
- **Definition**: Logical index spanning multiple nodes
- **Equivalent**: Similar to a standalone Solr core
- **Example**: `ikasan` collection in SolrCloud

#### 4. Shards
- **Definition**: Logical partition of a collection
- **Purpose**: Distribute data across multiple nodes (or keep unified in single shard)
- **Ikasan Use Case**: Single shard to maintain simplicity while gaining high availability

#### 5. Replicas
- **Definition**: Copy of a shard on different nodes
- **Purpose**: Provide redundancy and increased query throughput
- **Types**:
  - **NRT (Near Real-Time)**: Full functionality, can become leader
  - **TLOG (Transaction Log)**: Replicate using transaction logs, can become leader
  - **PULL**: Read-only, cannot become leader, lowest resource usage

### Architecture Diagram (Ikasan Configuration)

```
┌─────────────────────────────────────────────────────────────────┐
│                        ZooKeeper Ensemble                       │
│                    (3 nodes for coordination)                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────▼──────┐  ┌──────▼──────┐  ┌─────▼───────┐
    │  Solr Node 1 │  │ Solr Node 2 │  │ Solr Node 3 │
    │              │  │             │  │             │
    │ Shard1(L)    │  │ Shard1(R)   │  │ Shard1(R)   │
    │              │  │             │  │             │
    │              │  │             │  │             │
    └──────────────┘  └─────────────┘  └─────────────┘

    Single Shard (Shard1) replicated across all nodes
    L = Leader Replica
    R = Replica (Follower)
```

---

## Data Replication in SolrCloud

### Replication Types

#### 1. NRT (Near Real-Time) Replicas
**Characteristics**:
- Updates indexed locally and replicated from leader
- Can become shard leader
- Maintains full index locally
- Lowest query latency
- Highest resource usage (CPU, disk, RAM)

**Use Case**: Production environments requiring high availability and low latency

**Configuration Example**:
```bash
# Create collection with NRT replicas (single shard)
bin/solr create -c ikasan -s 1 -rf 3
# Creates 1 shard with 3 replicas (1 leader + 2 NRT replicas)
```

#### 2. TLOG (Transaction Log) Replicas
**Characteristics**:
- Replicates transaction logs from leader
- Can become shard leader
- Smaller index stored locally
- Medium resource usage
- Slightly higher query latency than NRT

**Use Case**: Balance between high availability and resource efficiency

**Configuration Example**:
```bash
# Add TLOG replica to existing collection
bin/solr create -c ikasan -s 1 -rf 1 -nrtReplicas 2 -tlogReplicas 1
```

#### 3. PULL Replicas
**Characteristics**:
- Pull full index from leader
- Cannot become shard leader
- Read-only for queries
- Lowest resource usage
- Eventual consistency (not real-time)

**Use Case**: Scale out query capacity without high indexing overhead

**Configuration Example**:
```bash
# Add PULL replica for query scaling
bin/solr create -c ikasan -s 1 -rf 1 -nrtReplicas 2 -pullReplicas 2
```

### Replication Strategies

#### Strategy 1: High Availability (Recommended for Production)
```
Configuration:
- Shards: 1
- NRT Replicas: 3
- Total nodes: 3

Benefits:
- Tolerates 2 node failures without data loss
- Fast query and indexing performance
- Automatic failover
- Simple architecture (no shard routing)

Resource Requirements:
- High (each replica maintains full index)
```

#### Strategy 2: Balanced (Cost-Effective)
```
Configuration:
- Shards: 1
- NRT Replicas: 2
- TLOG Replicas: 1
- Total nodes: 3

Benefits:
- Tolerates 1 node failure
- Lower resource usage than all-NRT
- Good query performance

Resource Requirements:
- Medium
```

#### Strategy 3: Query Scaling
```
Configuration:
- Shards: 1
- NRT Replicas: 2
- PULL Replicas: 2-4
- Total nodes: 4-6

Benefits:
- Massive query throughput
- Read-heavy workload optimization
- Cost-effective query scaling

Resource Requirements:
- Medium (PULL replicas are cheap)
```

### Replication Configuration for Ikasan

**Recommended Configuration**:
```properties
# For ikasan collection
shards: 1
nrtReplicas: 3
replicationFactor: 3

# This gives:
# - Single unified shard (no data partitioning)
# - 3 total replicas across 3 nodes
# - Can tolerate 2 node failures without data loss
# - Automatic leader election on failure
# - All data accessible from any node
```

---

## Migration Requirements

### Infrastructure Requirements

#### 1. ZooKeeper Ensemble
**Minimum Configuration**:
- **Nodes**: 3 (must be odd number for quorum)
- **RAM**: 2GB per node (minimum)
- **CPU**: 2 cores per node
- **Disk**: 10GB per node (for transaction logs)
- **Network**: Low latency between nodes (<5ms)

**Production Configuration**:
```properties
# zoo.cfg (on each ZooKeeper node)
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2

# Ensemble configuration
server.1=zk1.example.com:2888:3888
server.2=zk2.example.com:2888:3888
server.3=zk3.example.com:2888:3888
```

#### 2. Solr Nodes
**Minimum Configuration** (per node):
- **RAM**: 8GB (4GB heap + 4GB OS/cache)
- **CPU**: 4 cores
- **Disk**: 100GB SSD (depends on data size)
- **Network**: Gigabit ethernet

**Production Configuration** (per node):
- **RAM**: 32GB (16GB heap + 16GB OS/cache)
- **CPU**: 8+ cores
- **Disk**: 500GB+ SSD
- **Network**: 10Gbps

**Recommended Nodes**: 3 (for 1 shard with replication factor 3)

#### 3. Network Requirements
- **Latency**: <10ms between all nodes
- **Bandwidth**: Minimum 1Gbps, 10Gbps recommended
- **Firewall Rules**:
  - Solr: Port 8983 (or custom)
  - ZooKeeper: Ports 2181 (client), 2888 (follower), 3888 (election)

### Software Requirements

#### 1. Java
- **Version**: Java 11 or later
- **Memory Settings**: `-Xmx16g -Xms16g` (adjust based on RAM)
- **GC Settings**: `-XX:+UseG1GC` (recommended)

#### 2. Apache Solr
- **Version**: 9.x (match with current solrj client version)
- **Installation**: Binary distribution recommended
- **User**: Non-root user with appropriate permissions

#### 3. Apache ZooKeeper
- **Version**: 3.8.x or later
- **Installation**: Binary distribution
- **User**: Dedicated non-root user

### Capacity Planning

#### Calculate Required Storage
```
Formula:
Total Storage = (Index Size) × (Replication Factor) × 1.5

Example for Ikasan:
- Current index size: 50GB
- Shards: 1
- Replication factor: 3
- Growth buffer: 1.5

Total = 50GB × 3 × 1.5 = 225GB across cluster
Per node (3 nodes) = 75GB minimum
```

#### Calculate Required RAM
```
Formula:
RAM per node = (Heap Size) + (OS Cache) + (OS overhead)

Recommended:
- Heap: 16GB (don't exceed 31GB due to Java object pointers)
- OS Cache: 16GB (for Lucene file system cache)
- OS: 2-4GB
Total per node: 32-36GB
```

---

## Code Changes Required

### 1. Update SolrDaoBase.java

The `SolrDaoBase` class already has a `initCloud()` method but it needs to be fully integrated.

**Location**: `src/main/java/org/ikasan/spec/solr/SolrDaoBase.java:103-109`

**Current Cloud Method** (already exists):
```java
public void initCloud(List<String> solrCloudUrls, int daysToKeep)
{
    solrClient = new CloudSolrClient.Builder(solrCloudUrls).build();
    ((CloudSolrClient)solrClient).setDefaultCollection("ikasan");

    this.daysToKeep = daysToKeep;
}
```

**Enhanced Cloud Method** (recommended improvements):
```java
public void initCloud(List<String> zkHosts, String defaultCollection,
                     int daysToKeep, int connectionTimeoutMilli,
                     int socketTimeoutMilli)
{
    CloudSolrClient.Builder builder = new CloudSolrClient.Builder(zkHosts, Optional.empty());

    solrClient = builder
        .withConnectionTimeout(connectionTimeoutMilli, TimeUnit.MILLISECONDS)
        .withSocketTimeout(socketTimeoutMilli, TimeUnit.MILLISECONDS)
        .build();

    ((CloudSolrClient)solrClient).setDefaultCollection(defaultCollection);

    this.daysToKeep = daysToKeep;
}
```

### 2. Update SolrClientAutoConfiguration.java

**Location**: `src/main/java/org/ikasan/SolrClientAutoConfiguration.java`

**Add New Configuration Properties**:
```java
@Value("${solr.mode:standalone}")
private String solrMode; // "standalone" or "cloud"

@Value("${solr.cloud.zk.hosts:localhost:2181}")
private String zkHosts; // Comma-separated ZooKeeper hosts

@Value("${solr.cloud.collection:ikasan}")
private String defaultCollection;
```

**Create Conditional Initialization Method**:
```java
private void initializeSolrDao(SolrDaoBase<?> dao, int daysToKeep) {
    if ("cloud".equalsIgnoreCase(solrMode)) {
        // SolrCloud mode
        List<String> zkHostList = Arrays.asList(zkHosts.split(","));
        dao.initCloud(zkHostList, defaultCollection, daysToKeep,
                     solrSocketTimeoutMilli, solrConnectionTimeoutMilli);
    } else {
        // Standalone mode (default)
        dao.initStandalone(solrUrl, daysToKeep,
                          solrSocketTimeoutMilli, solrConnectionTimeoutMilli);
    }
    dao.setSolrUsername(solrUsername);
    dao.setSolrPassword(solrPassword);
}
```

**Update All Bean Creation Methods**:
```java
// Example: Update jobLockCacheService()
@Bean
public JobLockCacheService jobLockCacheService() {
    SolrJobLockCacheDaoImpl solrJobLockCacheDao = new SolrJobLockCacheDaoImpl();
    initializeSolrDao(solrJobLockCacheDao, SolrDaoBase.DO_NOT_EXPIRE);

    SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
    initializeSolrDao(solrJobLockCacheAuditDao, solrJobLockCacheAuditRetentionDays);

    return new SolrJobLockCacheServiceImpl(solrJobLockCacheDao,
                                          solrJobLockCacheAuditDao,
                                          saveJobLockCacheAudits);
}

// Repeat for all other beans (30+ beans to update)
```

### 3. Application Configuration Examples

#### Standalone Configuration (Current)
```properties
# application.properties - Standalone Mode

# Solr Configuration
solr.mode=standalone
solr.url=http://localhost:8983/solr
solr.username=admin
solr.password=admin
solr.retention.days=30
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000
```

#### SolrCloud Configuration (New)
```properties
# application.properties - SolrCloud Mode

# Solr Configuration
solr.mode=cloud
solr.cloud.zk.hosts=zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181
solr.cloud.collection=ikasan
solr.username=admin
solr.password=admin
solr.retention.days=30
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000

# Other configurations remain the same
solr.joblockcacheaudit.retention.days=30
solr.scheduler.instance.retention.days=90
solr.metrics.query.limit=200
```

### 4. Code Changes Summary

**Files to Modify**:
1. ✅ `SolrDaoBase.java` - Enhance `initCloud()` method (103-109)
2. ✅ `SolrClientAutoConfiguration.java` - Add conditional initialization (entire file)

**Changes Required**:
- Add 3 new configuration properties
- Create `initializeSolrDao()` helper method
- Update ~30 bean creation methods to use conditional initialization
- No changes to existing business logic
- Backward compatible with standalone mode (default)

**Estimated Effort**: 4-8 hours

---

## Data Migration Steps

### Pre-Migration Checklist

- [ ] Backup current Solr data
- [ ] Document current Solr configuration
- [ ] Test SolrCloud setup in non-production environment
- [ ] Verify network connectivity between all nodes
- [ ] Ensure sufficient disk space on all nodes
- [ ] Plan maintenance window
- [ ] Notify stakeholders

### Step 1: Backup Existing Data

#### Option A: Snapshot API (Recommended)
```bash
# Create backup directory
mkdir -p /backups/solr/ikasan

# Create snapshot
curl "http://localhost:8983/solr/admin/cores?action=BACKUP&core=ikasan&location=/backups/solr&name=ikasan-backup-$(date +%Y%m%d)"

# Verify backup
ls -lh /backups/solr/
```

#### Option B: File System Backup
```bash
# Stop Solr
systemctl stop solr

# Backup data directory
tar -czf /backups/ikasan-solr-$(date +%Y%m%d).tar.gz /var/solr/data/ikasan/

# Start Solr
systemctl start solr
```

#### Option C: Export to JSON
```bash
# Export all documents
curl "http://localhost:8983/solr/ikasan/select?q=*:*&rows=1000000&wt=json" > ikasan-export.json

# For large datasets, use streaming
curl "http://localhost:8983/solr/ikasan/select?q=*:*&rows=10000&wt=json&start=0" > ikasan-export-1.json
curl "http://localhost:8983/solr/ikasan/select?q=*:*&rows=10000&wt=json&start=10000" > ikasan-export-2.json
# ... continue as needed
```

### Step 2: Set Up ZooKeeper Ensemble

#### Install ZooKeeper on Each Node
```bash
# Download ZooKeeper
cd /opt
wget https://downloads.apache.org/zookeeper/zookeeper-3.8.3/apache-zookeeper-3.8.3-bin.tar.gz
tar -xzf apache-zookeeper-3.8.3-bin.tar.gz
ln -s apache-zookeeper-3.8.3-bin zookeeper

# Create data directory
mkdir -p /var/lib/zookeeper
```

#### Configure ZooKeeper
```bash
# Create zoo.cfg
cat > /opt/zookeeper/conf/zoo.cfg <<EOF
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2

# Ensemble configuration
server.1=zk1.example.com:2888:3888
server.2=zk2.example.com:2888:3888
server.3=zk3.example.com:2888:3888
EOF

# Set server ID (different on each node)
# On zk1:
echo "1" > /var/lib/zookeeper/myid
# On zk2:
echo "2" > /var/lib/zookeeper/myid
# On zk3:
echo "3" > /var/lib/zookeeper/myid
```

#### Start ZooKeeper
```bash
# On each ZooKeeper node
/opt/zookeeper/bin/zkServer.sh start

# Verify status
/opt/zookeeper/bin/zkServer.sh status

# Expected output: "Mode: leader" or "Mode: follower"
```

### Step 3: Set Up SolrCloud Cluster

#### Install Solr on Each Node
```bash
# Download Solr (use same version as solrj client)
cd /opt
wget https://downloads.apache.org/solr/solr-9.4.1/solr-9.4.1.tgz
tar -xzf solr-9.4.1.tgz
ln -s solr-9.4.1 solr

# Create solr user
useradd -r -s /bin/bash solr
chown -R solr:solr /opt/solr-9.4.1
```

#### Configure Solr for Cloud Mode
```bash
# Configure ZooKeeper connection
# Edit /opt/solr/server/solr/solr.xml or use environment variable

# Set environment variables
cat > /etc/default/solr.in.sh <<EOF
SOLR_JAVA_MEM="-Xms16g -Xmx16g"
SOLR_HEAP="16g"
ZK_HOST="zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181"
SOLR_HOST=$(hostname -f)
SOLR_PORT=8983
EOF
```

#### Start Solr Nodes
```bash
# On each Solr node
sudo -u solr /opt/solr/bin/solr start -c -z zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181

# Verify cluster status
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS"
```

### Step 4: Upload Configuration to ZooKeeper

#### Prepare Configuration
```bash
# Use existing schema from standalone Solr or create new configset
# Copy schema from current installation
cp -r /var/solr/data/ikasan/conf /tmp/ikasan-config/

# Or use default Solr configset as template
cp -r /opt/solr/server/solr/configsets/_default/conf /tmp/ikasan-config/
```

#### Upload to ZooKeeper
```bash
# Upload configset
/opt/solr/bin/solr zk upconfig -n ikasan-config -d /tmp/ikasan-config -z zk1.example.com:2181

# Verify upload
/opt/solr/bin/solr zk ls /configs -z zk1.example.com:2181
```

### Step 5: Create Collection

#### Create Collection with Replication
```bash
# Create ikasan collection
# 1 shard, replication factor 3, 3 total replicas
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=ikasan&numShards=1&replicationFactor=3&collection.configName=ikasan-config"

# Verify collection creation
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS&collection=ikasan"

# Expected structure:
# - 1 shard (shard1)
# - 3 replicas (1 leader + 2 replicas)
# - Replicas distributed across 3 nodes
```

#### Verify Shard Distribution
```bash
# Check shard placement
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS" | jq '.cluster.collections.ikasan.shards'

# Example output:
{
  "shard1": {
    "range": "80000000-7fffffff",
    "state": "active",
    "replicas": {
      "core_node1": {
        "core": "ikasan_shard1_replica_n1",
        "base_url": "http://solr1:8983/solr",
        "node_name": "solr1:8983_solr",
        "state": "active",
        "leader": "true"
      },
      "core_node2": {
        "core": "ikasan_shard1_replica_n2",
        "base_url": "http://solr2:8983/solr",
        "node_name": "solr2:8983_solr",
        "state": "active"
      },
      "core_node3": {
        "core": "ikasan_shard1_replica_n3",
        "base_url": "http://solr3:8983/solr",
        "node_name": "solr3:8983_solr",
        "state": "active"
      }
    }
  }
}
```

### Step 6: Import Data

#### Option A: Restore from Snapshot (Recommended)
```bash
# Copy backup to one of the SolrCloud nodes
scp /backups/solr/ikasan-backup-* solr1:/backups/

# Restore collection
curl "http://localhost:8983/solr/admin/collections?action=RESTORE&name=ikasan-backup-20240101&location=/backups/solr&collection=ikasan"

# Monitor restore status
curl "http://localhost:8983/solr/admin/collections?action=REQUESTSTATUS&requestid=<id>"
```

#### Option B: Reindex from JSON Export
```bash
# Import JSON documents
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8983/solr/ikasan/update?commit=true' \
  --data-binary @ikasan-export.json

# For multiple files
for file in ikasan-export-*.json; do
  echo "Importing $file..."
  curl -X POST -H 'Content-Type: application/json' \
    'http://localhost:8983/solr/ikasan/update?commit=true' \
    --data-binary @$file
done
```

#### Option C: Index Synchronization (Zero Downtime)
```bash
# Use Solr DataImportHandler or custom indexing
# This allows parallel indexing to both standalone and cloud

# 1. Configure application to dual-write (requires code changes)
# 2. Reindex historical data to SolrCloud
# 3. Switch read traffic to SolrCloud
# 4. Stop writing to standalone
```

### Step 7: Verify Data Integrity

```bash
# Check document count
curl "http://localhost:8983/solr/ikasan/select?q=*:*&rows=0"

# Compare with original
curl "http://old-solr:8983/solr/ikasan/select?q=*:*&rows=0"

# Sample queries to verify data
curl "http://localhost:8983/solr/ikasan/select?q=type:wiretap&rows=10"
curl "http://localhost:8983/solr/ikasan/select?q=moduleName:*&rows=0&facet=true&facet.field=moduleName"

# Check all shards are active
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS&collection=ikasan" | \
  jq '.cluster.collections.ikasan.shards[].replicas[].state'

# All should return "active"
```

### Step 8: Update Ikasan Application

#### Update Configuration
```properties
# application.properties
solr.mode=cloud
solr.cloud.zk.hosts=zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181
solr.cloud.collection=ikasan
solr.username=admin
solr.password=admin
```

#### Deploy Updated Code
```bash
# Build application with updated SolrClientAutoConfiguration
mvn clean package -DskipTests

# Deploy new version
# (deployment steps depend on your environment)
```

#### Verify Application Connectivity
```bash
# Start application
# Check logs for successful SolrCloud connection

# Expected log entries:
# "Initializing Solr client in cloud mode"
# "Connected to ZooKeeper: zk1.example.com:2181,..."
# "Using collection: ikasan"

# Test application queries
# Verify write operations create documents in SolrCloud
```

### Step 9: Monitoring and Validation

```bash
# Monitor cluster health
watch -n 5 'curl -s "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS" | jq ".cluster.collections.ikasan.shards[].replicas[].state"'

# Monitor indexing rate
curl "http://localhost:8983/solr/admin/metrics?group=core&prefix=UPDATE"

# Monitor query performance
curl "http://localhost:8983/solr/admin/metrics?group=core&prefix=QUERY"

# Check replication lag
curl "http://localhost:8983/solr/ikasan/replication?command=details"
```

---

## Testing & Validation

### Pre-Production Testing

#### 1. Functional Testing
```bash
# Test document indexing
curl -X POST -H 'Content-Type: application/json' \
  "http://localhost:8983/solr/ikasan/update?commit=true" \
  -d '[{"id":"test1","type":"wiretap","moduleName":"testModule"}]'

# Test search
curl "http://localhost:8983/solr/ikasan/select?q=id:test1"

# Test delete
curl "http://localhost:8983/solr/ikasan/update?commit=true" \
  -d '<delete><id>test1</id></delete>'
```

#### 2. Connection Testing
```bash
# Test ZooKeeper connectivity
echo ruok | nc zk1.example.com 2181
# Expected: "imok"

# Test Solr node connectivity
for node in solr1 solr2 solr3; do
  echo "Testing $node..."
  curl -f "http://$node:8983/solr/admin/ping" || echo "FAILED: $node"
done
```

#### 3. Load Testing
```bash
# Use Apache JMeter or similar tool
# Generate realistic query patterns
# Target: Match or exceed standalone performance

# Example using Apache Bench
ab -n 10000 -c 100 "http://localhost:8983/solr/ikasan/select?q=type:wiretap&rows=10"

# Monitor during load test
curl "http://localhost:8983/solr/admin/metrics?group=jvm"
```

### Failover Testing

#### Test 1: Node Failure
```bash
# Stop one Solr node
ssh solr2 "sudo systemctl stop solr"

# Verify cluster still serves requests
curl "http://solr1:8983/solr/ikasan/select?q=*:*&rows=1"

# Check shard status
curl "http://solr1:8983/solr/admin/collections?action=CLUSTERSTATUS&collection=ikasan" | \
  jq '.cluster.collections.ikasan.shards[] | select(.replicas[].state != "active")'

# Leader election should occur automatically
# Queries should continue without error

# Restart node
ssh solr2 "sudo systemctl start solr"

# Verify replica recovery
curl "http://solr1:8983/solr/admin/collections?action=CLUSTERSTATUS"
```

#### Test 2: ZooKeeper Failure
```bash
# Stop one ZK node (quorum should remain)
ssh zk2 "/opt/zookeeper/bin/zkServer.sh stop"

# Cluster should continue operating
curl "http://localhost:8983/solr/ikasan/select?q=*:*&rows=1"

# Restart ZK node
ssh zk2 "/opt/zookeeper/bin/zkServer.sh start"
```

#### Test 3: Network Partition
```bash
# Simulate network partition using iptables
ssh solr2 "sudo iptables -A INPUT -s solr1 -j DROP"

# Observe cluster behavior
# Shard leaders should remain stable
# Partitioned node should attempt to reconnect

# Remove partition
ssh solr2 "sudo iptables -D INPUT -s solr1 -j DROP"
```

### Performance Validation

#### Query Performance Comparison
```bash
# Baseline queries on standalone
for i in {1..100}; do
  curl -w "@curl-format.txt" -o /dev/null -s \
    "http://old-solr:8983/solr/ikasan/select?q=type:wiretap&rows=100"
done > standalone-results.txt

# Same queries on SolrCloud
for i in {1..100}; do
  curl -w "@curl-format.txt" -o /dev/null -s \
    "http://solr1:8983/solr/ikasan/select?q=type:wiretap&rows=100"
done > cloud-results.txt

# Compare results
# curl-format.txt:
time_namelookup:  %{time_namelookup}\n
time_connect:  %{time_connect}\n
time_starttransfer:  %{time_starttransfer}\n
time_total:  %{time_total}\n
```

#### Indexing Performance Comparison
```bash
# Bulk index test documents
# Generate test data
for i in {1..10000}; do
  echo "{\"id\":\"test$i\",\"type\":\"test\",\"timestamp\":$(date +%s)}"
done > test-docs.json

# Time standalone indexing
time curl -X POST -H 'Content-Type: application/json' \
  "http://old-solr:8983/solr/ikasan/update?commit=true" \
  --data-binary @test-docs.json

# Time SolrCloud indexing
time curl -X POST -H 'Content-Type: application/json' \
  "http://solr1:8983/solr/ikasan/update?commit=true" \
  --data-binary @test-docs.json

# Expected: SolrCloud similar or faster due to distributed indexing
```

### Validation Checklist

- [ ] All documents migrated successfully (count matches)
- [ ] Query results match between standalone and cloud
- [ ] Query performance meets requirements
- [ ] Indexing performance meets requirements
- [ ] Failover occurs automatically within acceptable time
- [ ] No data loss during node failure
- [ ] Application connects successfully to SolrCloud
- [ ] All application features work correctly
- [ ] Monitoring and alerting configured
- [ ] Backup/restore tested and documented

---

## Rollback Plan

### When to Rollback
- Critical issues discovered during migration
- Unacceptable performance degradation
- Data integrity problems
- Application compatibility issues

### Rollback Steps

#### 1. Immediate Rollback (Application Only)
```properties
# Revert application.properties
solr.mode=standalone
solr.url=http://old-solr:8983/solr

# Restart application
# No data loss if standalone Solr still running
```

#### 2. Full Rollback (with Data Recovery)

**Prerequisites**:
- Backup from Step 1 available
- Standalone Solr instance preserved

**Steps**:
```bash
# 1. Stop application
systemctl stop ikasan

# 2. Stop writing to SolrCloud
# No action needed if application stopped

# 3. Restore standalone Solr if needed
curl "http://old-solr:8983/solr/admin/cores?action=RESTORE&core=ikasan&location=/backups/solr&name=ikasan-backup-20240101"

# 4. Update application configuration
# Change to standalone mode

# 5. Restart application
systemctl start ikasan

# 6. Verify functionality
curl "http://localhost:8080/actuator/health"
```

#### 3. Preserve SolrCloud for Analysis
```bash
# Don't delete SolrCloud immediately
# Keep cluster running for troubleshooting

# Export recent documents for analysis
curl "http://localhost:8983/solr/ikasan/select?q=timestamp:[NOW-1DAY TO NOW]&rows=100000&wt=json" > recent-docs.json

# Document issues encountered
# Analyze logs from all nodes
```

### Post-Rollback Actions

1. **Root Cause Analysis**
   - Collect logs from all Solr and ZooKeeper nodes
   - Document specific errors encountered
   - Performance metrics comparison
   - Timeline of events

2. **Remediation Planning**
   - Identify specific issues
   - Determine fixes or optimizations needed
   - Plan second migration attempt
   - Update runbook with lessons learned

3. **Stakeholder Communication**
   - Notify of rollback completion
   - Explain reasons for rollback
   - Provide timeline for retry
   - Set expectations for next attempt

---

## Best Practices & Recommendations

### 1. Gradual Migration Strategy

**Phase 1: Read-Only Testing**
- Set up SolrCloud in parallel
- Dual-write to both standalone and cloud
- Read from standalone only
- Compare results and performance
- Duration: 2-4 weeks

**Phase 2: Partial Traffic Migration**
- Route 10% of read traffic to SolrCloud
- Monitor performance and errors
- Gradually increase to 50%, then 100%
- Continue dual-writes
- Duration: 2-4 weeks

**Phase 3: Full Migration**
- All reads from SolrCloud
- Stop writing to standalone
- Decommission standalone after validation period
- Duration: 1-2 weeks

### 2. Monitoring Setup

**Key Metrics to Monitor**:
```yaml
# Cluster Health
- Node status (up/down)
- Replica state (active/recovering/down)
- Leader election events
- ZooKeeper connection status

# Performance Metrics
- Query latency (p50, p95, p99)
- Indexing throughput (docs/sec)
- Commit time
- Cache hit rates

# Resource Utilization
- CPU usage per node
- Memory (heap and off-heap)
- Disk I/O and space
- Network throughput

# Error Rates
- Failed queries
- Failed commits
- Replica recovery failures
- ZooKeeper connection errors
```

**Alerting Thresholds**:
```yaml
Critical:
- More than 1 replica down per shard
- All ZooKeeper nodes unreachable
- Query error rate > 5%
- Any node disk > 90% full

Warning:
- Single replica down
- Query latency p95 > 500ms
- Heap usage > 80%
- Replication lag > 60 seconds
```

### 3. Maintenance Procedures

**Regular Tasks**:
```bash
# Weekly: Check cluster health
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS"

# Monthly: Optimize indexes
curl "http://localhost:8983/solr/ikasan/update?optimize=true"

# Quarterly: ZooKeeper log cleanup
/opt/zookeeper/bin/zkCleanup.sh /var/lib/zookeeper -n 10

# As needed: Add nodes for capacity
curl "http://localhost:8983/solr/admin/collections?action=ADDREPLICA&collection=ikasan&shard=shard1"
```

**Backup Strategy**:
```bash
# Daily automated backups
0 2 * * * curl "http://localhost:8983/solr/admin/collections?action=BACKUP&collection=ikasan&location=/backups/daily&name=ikasan-$(date +\%Y\%m\%d)"

# Weekly full backups to remote storage
0 3 * * 0 rsync -av /backups/daily/ remote-backup:/solr-backups/
```

### 4. Security Considerations

**Authentication**:
```bash
# Enable Basic Authentication
# Add to security.json in ZooKeeper
/opt/solr/bin/solr auth enable -credentials admin:password -z zk1:2181,zk2:2181,zk3:2181
```

**SSL/TLS**:
```bash
# Generate keystore
keytool -genkeypair -alias solr-ssl -keyalg RSA -keysize 2048 -keystore /opt/solr/server/etc/solr-ssl.keystore.p12

# Configure solr.in.sh
SOLR_SSL_ENABLED=true
SOLR_SSL_KEY_STORE=/opt/solr/server/etc/solr-ssl.keystore.p12
SOLR_SSL_KEY_STORE_PASSWORD=secret
```

**Network Isolation**:
- Use private network for inter-node communication
- Firewall rules limiting access to necessary ports only
- VPN or bastion host for administrative access

### 5. Troubleshooting Common Issues

**Issue: Replica not recovering**
```bash
# Check replica status
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS"

# Force recovery
curl "http://localhost:8983/solr/admin/cores?action=REQUESTRECOVERY&core=ikasan_shard1_replica_n2"

# If still failing, delete and recreate replica
curl "http://localhost:8983/solr/admin/collections?action=DELETEREPLICA&collection=ikasan&shard=shard1&replica=core_node2"
curl "http://localhost:8983/solr/admin/collections?action=ADDREPLICA&collection=ikasan&shard=shard1"
```

**Issue: High query latency**
```bash
# Check query distribution
curl "http://localhost:8983/solr/admin/metrics?prefix=QUERY.ikasan"

# Enable query result caching
# Add to solrconfig.xml
<queryResultCache class="solr.LRUCache" size="512" initialSize="256"/>

# With single shard, check if leader is overloaded
# Queries should be distributed to all replicas
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS&collection=ikasan"
```

**Issue: Out of memory**
```bash
# Check heap usage
curl "http://localhost:8983/solr/admin/metrics?prefix=jvm.memory"

# Increase heap if needed
# Edit solr.in.sh
SOLR_HEAP="32g"

# Consider offheap caches
# Add to solrconfig.xml
<documentCache class="solr.CaffeineCache" size="10000" maxRamMB="512"/>
```

---

## Appendix

### A. Useful Commands Reference

```bash
# Cluster Management
curl "http://localhost:8983/solr/admin/collections?action=LIST"
curl "http://localhost:8983/solr/admin/collections?action=CLUSTERSTATUS"
curl "http://localhost:8983/solr/admin/zookeeper?detail=true&path=/collections"

# Collection Management
curl "http://localhost:8983/solr/admin/collections?action=CREATE&name=ikasan&numShards=1&replicationFactor=3"
curl "http://localhost:8983/solr/admin/collections?action=DELETE&name=ikasan"
curl "http://localhost:8983/solr/admin/collections?action=RELOAD&name=ikasan"

# Replica Management
curl "http://localhost:8983/solr/admin/collections?action=ADDREPLICA&collection=ikasan&shard=shard1"
curl "http://localhost:8983/solr/admin/collections?action=DELETEREPLICA&collection=ikasan&shard=shard1&replica=core_node2"

# Backup/Restore
curl "http://localhost:8983/solr/admin/collections?action=BACKUP&collection=ikasan&location=/backups&name=backup1"
curl "http://localhost:8983/solr/admin/collections?action=RESTORE&collection=ikasan&location=/backups&name=backup1"

# Monitoring
curl "http://localhost:8983/solr/admin/metrics"
curl "http://localhost:8983/solr/admin/info/system"
curl "http://localhost:8983/solr/ikasan/admin/ping"
```

### B. Configuration Templates

**ZooKeeper zoo.cfg**:
```properties
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
maxClientCnxns=100
admin.enableServer=false

server.1=zk1.example.com:2888:3888
server.2=zk2.example.com:2888:3888
server.3=zk3.example.com:2888:3888
```

**Solr solr.in.sh**:
```bash
SOLR_JAVA_MEM="-Xms16g -Xmx16g"
SOLR_HEAP="16g"
ZK_HOST="zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181"
SOLR_HOST=$(hostname -f)
SOLR_PORT=8983
SOLR_TIMEZONE="UTC"

# GC Settings
GC_TUNE="-XX:+UseG1GC -XX:MaxGCPauseMillis=250"

# GC Logging
GC_LOG_OPTS="-Xlog:gc*:file=/var/solr/logs/solr_gc.log:time,uptime:filecount=9,filesize=20M"

# Authentication
SOLR_AUTH_TYPE="basic"
SOLR_AUTHENTICATION_OPTS="-Dbasicauth=admin:password"
```

### C. Capacity Planning Calculator

```python
#!/usr/bin/env python3
"""
SolrCloud Capacity Planning Calculator
"""

def calculate_capacity(index_size_gb, replication_factor, growth_factor=1.5):
    """
    Calculate required storage capacity

    Args:
        index_size_gb: Current index size in GB
        replication_factor: Number of replicas (including leader)
        growth_factor: Buffer for growth (default 1.5 = 50% buffer)

    Returns:
        dict with capacity recommendations
    """
    # Total cluster storage (single shard replicated)
    total_storage = index_size_gb * replication_factor * growth_factor

    # Number of nodes
    num_nodes = replication_factor

    # Storage per node
    storage_per_node = total_storage / num_nodes

    # RAM per node (heap + OS cache + overhead)
    heap_per_node = 16  # GB
    os_cache_per_node = min(storage_per_node, 16)  # GB, capped at 16
    ram_per_node = heap_per_node + os_cache_per_node + 4  # 4GB overhead

    return {
        "total_storage_gb": round(total_storage, 2),
        "storage_per_node_gb": round(storage_per_node, 2),
        "num_nodes": num_nodes,
        "ram_per_node_gb": round(ram_per_node, 2),
        "heap_per_node_gb": heap_per_node,
        "recommended_cpu_cores": 8
    }

# Example usage
if __name__ == "__main__":
    # Ikasan example - single shard with replication
    result = calculate_capacity(
        index_size_gb=50,
        replication_factor=3
    )

    print("SolrCloud Capacity Requirements (Single Shard):")
    print(f"  Total Storage: {result['total_storage_gb']} GB")
    print(f"  Number of Nodes: {result['num_nodes']}")
    print(f"  Storage per Node: {result['storage_per_node_gb']} GB")
    print(f"  RAM per Node: {result['ram_per_node_gb']} GB")
    print(f"  CPU Cores per Node: {result['recommended_cpu_cores']}")
```

### D. Further Reading

**Official Documentation**:
- [SolrCloud Architecture](https://solr.apache.org/guide/solr/latest/deployment-guide/cluster-types.html)
- [ZooKeeper Administrator Guide](https://zookeeper.apache.org/doc/current/zookeeperAdmin.html)
- [Solr Scaling and Performance](https://solr.apache.org/guide/solr/latest/deployment-guide/solrcloud-autoscaling.html)

**Community Resources**:
- [Solr Users Mailing List](https://solr.apache.org/community.html#mailing-lists)
- [SolrCloud Best Practices (Lucidworks)](https://lucidworks.com/post/solrcloud-best-practices/)
- [Apache Solr Cookbook](https://www.oreilly.com/library/view/apache-solr-cookbook/9781783553655/)

**Monitoring Tools**:
- [Prometheus Solr Exporter](https://solr.apache.org/guide/solr/latest/deployment-guide/monitoring-with-prometheus-and-grafana.html)
- [Grafana Dashboards for Solr](https://grafana.com/grafana/dashboards?search=solr)

---

## Document Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Generated | Initial version |

---

**Questions or Issues?**

For questions about this migration guide or SolrCloud setup, please contact:
- Ikasan Development Team
- [GitHub Issues](https://github.com/ikasanEIP/ikasan/issues)
