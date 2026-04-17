![IKASAN](../../developer/docs/quickstart-images/Ikasan-title-transparent.png)
# Solr Client Configuration Guide

The Ikasan Solr client can be configured to work with either **Standalone Solr** or **SolrCloud** mode.

## Configuration Properties

### Common Properties (Both Modes)

```properties
# Solr authentication (optional)
solr.username=
solr.password=

# Retention settings (days)
solr.retention.days=30
solr.joblockcacheaudit.retention.days=30
solr.scheduler.instance.retention.days=90

# Connection timeouts (milliseconds)
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000

# Metrics settings
solr.metrics.query.limit=200

# Audit settings
solr.save.context.instance.audits=true
solr.save.context.instance.audit.deltas=true
solr.save.joblockcache.audits=true

# Batch insert notification
notify.scheduled.events.batch.insert.listeners=false

# Legacy job status count
ikasan.enterprise.scheduler.use.legacy.job.status.count=false

# Scheduler job execution environment labels
scheduler.job.execution.environment.label={}
```

## Standalone Solr Configuration

For standalone Solr instances (single node, no clustering):

```properties
# Solr mode
solr.mode=standalone

# Solr URL
solr.url=http://localhost:8983/solr/ikasan
```

### Example: Local Development

```properties
solr.mode=standalone
solr.url=http://localhost:8983/solr/ikasan
solr.username=
solr.password=
```

### Example: Production Standalone

```properties
solr.mode=standalone
solr.url=https://solr.example.com:8983/solr/ikasan
solr.username=solr_user
solr.password=secret_password
solr.retention.days=90
```

## SolrCloud Configuration

For SolrCloud deployments (clustered, high availability):

```properties
# Solr mode
solr.mode=cloud

# ZooKeeper connection string (comma-separated list of ZK hosts)
solr.cloud.zk.hosts=localhost:2181,localhost:2182,localhost:2183
```

### Example: Local SolrCloud (3 nodes on same host)

```properties
solr.mode=cloud
solr.cloud.zk.hosts=localhost:2181,localhost:2182,localhost:2183
solr.username=
solr.password=
```

### Example: Production SolrCloud (3 nodes on separate hosts)

```properties
solr.mode=cloud
solr.cloud.zk.hosts=zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181
solr.username=solr_user
solr.password=secret_password
solr.retention.days=90
```

## Important Notes

1. **Collection Name**: The SolrCloud client automatically connects to the `ikasan` collection. Ensure this collection exists in your SolrCloud cluster.

2. **ZooKeeper Hosts**: For SolrCloud mode, provide the ZooKeeper connection string, not the Solr URLs. The client will discover Solr nodes through ZooKeeper.

3. **Authentication**: If your Solr instance uses Basic Authentication, provide `solr.username` and `solr.password`.

4. **Migration from Standalone to Cloud**:
   - Change `solr.mode` from `standalone` to `cloud`
   - Remove `solr.url` property
   - Add `solr.cloud.zk.hosts` property with your ZooKeeper ensemble

5. **Required Properties**:
   - For `standalone` mode: `solr.url` is required
   - For `cloud` mode: `solr.cloud.zk.hosts` is required

## Configuration Validation

The client will validate configuration on startup:
- If `solr.mode=standalone` and `solr.url` is missing, an exception will be thrown
- If `solr.mode=cloud` and `solr.cloud.zk.hosts` is missing, an exception will be thrown

## Default Mode

If `solr.mode` is not specified, the client defaults to `standalone` mode for backward compatibility.

## Complete Example Configuration Files

### application.properties (Standalone)

```properties
# Solr Configuration - Standalone Mode
solr.mode=standalone
solr.url=http://localhost:8983/solr/ikasan
solr.username=
solr.password=

# Retention Configuration
solr.retention.days=30
solr.joblockcacheaudit.retention.days=30
solr.scheduler.instance.retention.days=90

# Connection Configuration
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000
solr.metrics.query.limit=200

# Audit Configuration
solr.save.context.instance.audits=true
solr.save.context.instance.audit.deltas=true
solr.save.joblockcache.audits=true

# Batch Insert Configuration
notify.scheduled.events.batch.insert.listeners=false

# Scheduler Configuration
ikasan.enterprise.scheduler.use.legacy.job.status.count=false
scheduler.job.execution.environment.label={}
```

### application.properties (SolrCloud)

```properties
# Solr Configuration - Cloud Mode
solr.mode=cloud
solr.cloud.zk.hosts=localhost:2181,localhost:2182,localhost:2183
solr.username=
solr.password=

# Retention Configuration
solr.retention.days=30
solr.joblockcacheaudit.retention.days=30
solr.scheduler.instance.retention.days=90

# Connection Configuration
solr.connection.timeout.milli=15000
solr.socket.timeout.milli=15000
solr.metrics.query.limit=200

# Audit Configuration
solr.save.context.instance.audits=true
solr.save.context.instance.audit.deltas=true
solr.save.joblockcache.audits=true

# Batch Insert Configuration
notify.scheduled.events.batch.insert.listeners=false

# Scheduler Configuration
ikasan.enterprise.scheduler.use.legacy.job.status.count=false
scheduler.job.execution.environment.label={}
```
