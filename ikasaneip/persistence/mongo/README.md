# Ikasan MongoDB Persistence Module

This module provides MongoDB-based persistence for the Ikasan Dashboard, storing all operational data in a single collection with document type discrimination.

## Overview

All documents are stored in a single collection named `ikasan` with a `type` field used to discriminate between different document types. This design allows for efficient querying across related data types and simplifies index management.

## Configuration Properties

### Connection Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.uri` | *required* | MongoDB connection URI (e.g., `mongodb://localhost:27017/ikasan`) |
| `mongodb.database` | `test` | Database name |
| `mongodb.username` | *null* | Username for authentication (overrides URI credentials) |
| `mongodb.password` | *null* | Password for authentication |
| `mongodb.authentication-database` | `admin` | Authentication database |
| `mongodb.auth-mechanism` | *null* | Authentication mechanism: `SCRAM-SHA-1`, `SCRAM-SHA-256`, `GSSAPI`, `PLAIN`, `MONGODB-X509` |

### SSL/TLS Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.ssl.enabled` | `false` | Enable SSL/TLS |
| `mongodb.ssl.invalid-hostname-allowed` | `false` | Allow invalid hostnames in SSL certificates |

### Connection Pool Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.connection.pool.min-size` | `10` | Minimum number of connections |
| `mongodb.connection.pool.max-size` | `100` | Maximum number of connections |
| `mongodb.connection.pool.max-wait-time-ms` | `120000` | Maximum wait time for a connection (ms) |
| `mongodb.connection.pool.max-connection-life-time-ms` | `0` | Maximum connection lifetime (ms), 0 = unlimited |
| `mongodb.connection.pool.max-connection-idle-time-ms` | `0` | Maximum idle time (ms), 0 = unlimited |
| `mongodb.connection.pool.maintenance-initial-delay-ms` | `0` | Initial delay before pool maintenance starts (ms) |
| `mongodb.connection.pool.maintenance-frequency-ms` | `60000` | Frequency of pool maintenance (ms) |

### Socket Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.socket.connect-timeout-ms` | `10000` | Socket connect timeout (ms) |
| `mongodb.socket.read-timeout-ms` | `0` | Socket read timeout (ms), 0 = unlimited |
| `mongodb.socket.receive-buffer-size` | `0` | Receive buffer size (bytes), 0 = OS default |
| `mongodb.socket.send-buffer-size` | `0` | Send buffer size (bytes), 0 = OS default |

### Server Monitoring Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.server.heartbeat-frequency-ms` | `10000` | Frequency of server heartbeat checks (ms) |
| `mongodb.server.min-heartbeat-frequency-ms` | `500` | Minimum heartbeat frequency (ms) |
| `mongodb.server.selection-timeout-ms` | `30000` | Server selection timeout (ms) |

### Cluster Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.cluster.local-threshold-ms` | `15` | Local threshold for server selection (ms) |

### Retry Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.retry.writes` | `true` | Enable retryable writes |
| `mongodb.retry.reads` | `true` | Enable retryable reads |

### Compression Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.compression.enabled` | `false` | Enable network compression |
| `mongodb.compression.compressors` | `snappy,zlib,zstd` | Comma-separated list of compressors to use |

### Atlas Search Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongodb.atlas.search.enabled` | `true` | Enable MongoDB Atlas Lucene-based $search. When `true`, attempts Atlas Search first and falls back to regex if unavailable. When `false`, always uses regex-based search. |
| `mongodb.atlas.search.index-name` | `default` | Name of the Atlas Search index to use |

### Entity Retention Settings

| Property | Default | Description |
|----------|---------|-------------|
| `entity.retention.days` | `30` | Number of days to retain error, exclusion, hospital, and system event records |
| `scheduler.instance.entity.retention.days` | `90` | Number of days to retain scheduler instance records |
| `wiretap.housekeep.daysToKeep` | `90` | Number of days to retain wiretap records |
| `scheduled.process.event.housekeep.daysToKeep` | `90` | Number of days to retain scheduled process events |
| `mongo.replay.audit.days.to.keep` | `7` | Number of days to retain replay audit records |

### Metrics Settings

| Property | Default | Description |
|----------|---------|-------------|
| `mongo.metrics.query.limit` | `1000` | Maximum number of metrics records to return per query |

## Required Indexes

All documents are stored in the `ikasan` collection. The following indexes should be created for optimal performance.

### Standard Indexes

These indexes are automatically created by Spring Data MongoDB based on `@Indexed` annotations:

```javascript
// Error Occurrences
db.ikasan.createIndex({ "errorUri": 1 })
db.ikasan.createIndex({ "moduleName": 1 })
db.ikasan.createIndex({ "flowName": 1 })
db.ikasan.createIndex({ "componentName": 1 })
db.ikasan.createIndex({ "event": 1 })
db.ikasan.createIndex({ "expiry": 1 })
db.ikasan.createIndex({ "createdDateTime": 1 })
db.ikasan.createIndex({ "harvestReceivedTimestamp": 1 })

// Wiretap Events
db.ikasan.createIndex({ "eventId": 1 })
db.ikasan.createIndex({ "payloadId": 1 })

// System Events
db.ikasan.createIndex({ "actor": 1 })
db.ikasan.createIndex({ "subject": 1 })
db.ikasan.createIndex({ "action": 1 })

// Exclusion Events
db.ikasan.createIndex({ "errorUri": 1 })

// Hospital (Exclusion Event Actions)
db.ikasan.createIndex({ "actionedBy": 1 })

// Replay Events
db.ikasan.createIndex({ "eventId": 1 })
db.ikasan.createIndex({ "eventAsString": 1 })

// Security - Users
db.ikasan.createIndex({ "username": 1 })
db.ikasan.createIndex({ "email": 1 })

// Scheduled Context
db.ikasan.createIndex({ "contextName": 1 }, { unique: true })
db.ikasan.createIndex({ "contextDescription": 1 })
db.ikasan.createIndex({ "disabled": 1 })

// Scheduler Jobs (all types)
db.ikasan.createIndex({ "contextName": 1 })
db.ikasan.createIndex({ "jobName": 1 })

// Scheduler Job Instances
db.ikasan.createIndex({ "contextInstanceId": 1 })

// Job Lock Cache
db.ikasan.createIndex({ "lockName": 1 })
db.ikasan.createIndex({ "contextName": 1 })

// Notifications
db.ikasan.createIndex({ "notificationName": 1 })

// Business Streams
db.ikasan.createIndex({ "name": 1 })
db.ikasan.createIndex({ "moduleName": 1 })

// Module Metadata
db.ikasan.createIndex({ "name": 1 })

// Metrics
db.ikasan.createIndex({ "harvestTime": 1 })

// Context Profiles
db.ikasan.createIndex({ "contextName": 1 })
db.ikasan.createIndex({ "profileName": 1 })
```

### Compound Indexes (Recommended)

For better query performance, create these compound indexes:

```javascript
// Common query pattern: filter by type and timestamp range
db.ikasan.createIndex({ "type": 1, "createdDateTime": -1 })

// Error queries: module + flow + time
db.ikasan.createIndex({ "type": 1, "moduleName": 1, "flowName": 1, "createdDateTime": -1 })

// Wiretap queries
db.ikasan.createIndex({ "type": 1, "moduleName": 1, "flowName": 1, "componentName": 1, "createdDateTime": -1 })

// System event queries
db.ikasan.createIndex({ "type": 1, "actor": 1, "createdDateTime": -1 })
db.ikasan.createIndex({ "type": 1, "subject": 1, "createdDateTime": -1 })

// Scheduler job queries
db.ikasan.createIndex({ "type": 1, "contextName": 1, "jobName": 1 })

// Scheduler instance queries
db.ikasan.createIndex({ "type": 1, "contextInstanceId": 1, "createdDateTime": -1 })

// Housekeeping: expiry-based TTL queries
db.ikasan.createIndex({ "type": 1, "expiry": 1 })
```

### TTL Index (Optional)

If you want MongoDB to automatically delete expired documents:

```javascript
// Automatically delete documents after expiry timestamp
db.ikasan.createIndex(
  { "expiry": 1 },
  { expireAfterSeconds: 0 }
)
```

**Note:** Only use this if all document types use the `expiry` field consistently. The Ikasan housekeeping service handles deletion programmatically, so this index is optional.

## Atlas Search Index

If using MongoDB Atlas with Atlas Search enabled (`mongodb.atlas.search.enabled=true`), create the following search index:

### Search Index Definition

Create a search index named `default` (or the name specified in `mongodb.atlas.search.index-name`):

```json
{
  "name": "default",
  "definition": {
    "mappings": {
      "dynamic": false,
      "fields": {
        "errorDetail": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "errorMessage": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "errorUri": {
          "type": "string",
          "analyzer": "lucene.keyword"
        },
        "event": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "payloadContent": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "actor": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "subject": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "action": {
          "type": "string",
          "analyzer": "lucene.standard"
        },
        "moduleName": {
          "type": "string",
          "analyzer": "lucene.keyword"
        },
        "flowName": {
          "type": "string",
          "analyzer": "lucene.keyword"
        },
        "componentName": {
          "type": "string",
          "analyzer": "lucene.keyword"
        },
        "type": {
          "type": "string",
          "analyzer": "lucene.keyword"
        },
        "createdDateTime": {
          "type": "number"
        },
        "expiry": {
          "type": "number"
        }
      }
    }
  }
}
```

### Creating the Search Index via Atlas UI

1. Navigate to your MongoDB Atlas cluster
2. Go to **Search** tab
3. Click **Create Search Index**
4. Select **JSON Editor**
5. Select the `ikasan` collection
6. Paste the index definition above
7. Click **Create Search Index**

### Creating the Search Index via Atlas CLI

```bash
atlas clusters search indexes create \
  --clusterName <cluster-name> \
  --db <database-name> \
  --collection ikasan \
  --file search-index-definition.json
```

## Document Types

The `type` field discriminates between document types:

| Type Value | Description |
|------------|-------------|
| `ERROR` | Error occurrences |
| `WIRETAP` | Wiretap events |
| `EXCLUSION` | Exclusion events |
| `EXCLUSION_EVENT_ACTION` | Hospital/exclusion event actions |
| `REPLAY` | Replay events |
| `REPLAY_AUDIT` | Replay audit events |
| `SYSTEM_EVENT` | System events |
| `METRICS` | Flow invocation metrics |
| `MODULE_METADATA` | Module metadata |
| `BUSINESS_STREAM` | Business stream metadata |
| `COMPONENT_CONFIGURATION` | Component configuration metadata |
| `SCHEDULED_CONTEXT` | Scheduled contexts |
| `SCHEDULED_CONTEXT_VIEW` | Scheduled context views |
| `SCHEDULED_CONTEXT_INSTANCE` | Scheduled context instances |
| `SCHEDULED_CONTEXT_INSTANCE_AUDIT` | Scheduled context instance audits |
| `SCHEDULER_JOB` | Scheduler jobs |
| `SCHEDULER_JOB_INSTANCE` | Scheduler job instances |
| `QUARTZ_SCHEDULE_DRIVEN_JOB` | Quartz schedule-driven jobs |
| `FILE_EVENT_DRIVEN_JOB` | File event-driven jobs |
| `INTERNAL_EVENT_DRIVEN_JOB` | Internal event-driven jobs |
| `GLOBAL_EVENT_JOB` | Global event jobs |
| `CONTEXT_START_JOB` | Context start jobs |
| `CONTEXT_TERMINAL_JOB` | Context terminal jobs |
| `BRIDGING_JOB` | Bridging jobs |
| `JOB_LOCK` | Job locks |
| `JOB_LOCK_AUDIT` | Job lock audits |
| `EMAIL_NOTIFICATION_CONTEXT` | Email notification contexts |
| `EMAIL_NOTIFICATION_DETAILS` | Email notification details |
| `NOTIFICATION_SEND_AUDIT` | Notification send audits |
| `CONTEXT_PROFILE` | Context profiles |
| `USER` | User records |
| `PRINCIPAL` | Principal records |
| `ROLE` | Role records |
| `POLICY` | Policy records |
| `AUTHENTICATION_METHOD` | Authentication method records |
| `PLATFORM_SETUP` | Platform setup records |

## Example Configuration

### application.properties

```properties
# MongoDB Connection
mongodb.uri=mongodb+srv://cluster.mongodb.net/ikasan?retryWrites=true&w=majority
mongodb.database=ikasan
mongodb.username=ikasan_user
mongodb.password=${MONGODB_PASSWORD}

# Connection Pool
mongodb.connection.pool.min-size=10
mongodb.connection.pool.max-size=50

# SSL (for Atlas)
mongodb.ssl.enabled=true

# Atlas Search
mongodb.atlas.search.enabled=true
mongodb.atlas.search.index-name=default

# Retention
entity.retention.days=30
scheduler.instance.entity.retention.days=90
```

### application.yml

```yaml
mongodb:
  uri: mongodb+srv://cluster.mongodb.net/ikasan?retryWrites=true&w=majority
  database: ikasan
  username: ikasan_user
  password: ${MONGODB_PASSWORD}

  connection:
    pool:
      min-size: 10
      max-size: 50

  ssl:
    enabled: true

  atlas:
    search:
      enabled: true
      index-name: default

entity:
  retention:
    days: 30

scheduler:
  instance:
    entity:
      retention:
        days: 90
```

## Non-Atlas Deployments

For non-Atlas MongoDB deployments (standalone, replica set, or on-premises):

1. Set `mongodb.atlas.search.enabled=false` to disable Atlas Search
2. The module will automatically use regex-based search for full-text queries
3. Ensure standard indexes are created for acceptable query performance

```properties
# Disable Atlas Search for on-premises MongoDB
mongodb.atlas.search.enabled=false
```

Even with Atlas Search enabled, the module will automatically fall back to regex-based search if it detects that Atlas Search is not available (e.g., when connecting to a non-Atlas MongoDB instance).

## Atlas Search Query Helper

The `AtlasSearchQueryHelper` class provides methods for executing Atlas Search queries with automatic fallback to regex-based search when Atlas Search is unavailable.

### Search Methods

| Method | Description |
|--------|-------------|
| `searchWithWildcard()` | Searches across multiple fields using compound OR wildcard. Best for full-text search across multiple fields. |
| `searchWithText()` | Searches using Atlas text search across multiple fields. |
| `searchWithFieldSpecificWildcards()` | Searches with field-specific AND (must) wildcard clauses. Each field-value pair must match (AND logic). |

### Field-Specific Search with AND Logic

For scenarios requiring each filter to match its specific field (AND semantics), use `searchWithFieldSpecificWildcards()`:

```java
// Build field-value pairs (all must match)
List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = new ArrayList<>();
fieldValuePairs.add(new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "myModule"));
fieldValuePairs.add(new AtlasSearchAggregationOperation.FieldValuePair("flowName", "myFlow"));
fieldValuePairs.add(new AtlasSearchAggregationOperation.FieldValuePair("status", "COMPLETE"));

// Execute search with AND logic
AtlasSearchQueryHelper.SearchQueryResult<MyEntity> result =
    atlasSearchQueryHelper.searchWithFieldSpecificWildcards(
        fieldValuePairs,           // Field-specific filters (AND logic)
        additionalCriteria,        // Optional additional match criteria
        Sort.by(Sort.Direction.DESC, "createdDateTime"),
        offset,
        limit,
        MyEntity.class
    );

List<MyEntity> results = result.getResults();
long totalCount = result.getTotalCount();
```

### Multi-Field Text Search with OR Logic

For full-text search across multiple fields (OR semantics), use `searchWithWildcard()`:

```java
// Define fields to search
List<String> searchFields = List.of("errorMessage", "errorDetail", "event");

// Execute search - matches if search term found in ANY of the fields
AtlasSearchQueryHelper.SearchQueryResult<MyEntity> result =
    atlasSearchQueryHelper.searchWithWildcard(
        "search term",             // Search string
        searchFields,              // Fields to search (OR across all)
        additionalCriteria,        // Optional additional match criteria
        Sort.by(Sort.Direction.DESC, "createdDateTime"),
        offset,
        limit,
        MyEntity.class
    );
```

### Automatic Fallback

Both methods automatically fall back to regex-based search when:
- Atlas Search is disabled via configuration (`mongodb.atlas.search.enabled=false`)
- Atlas Search is unavailable (e.g., connecting to non-Atlas MongoDB)
- Atlas Search index does not exist

The fallback uses equivalent regex patterns to maintain the same search semantics.
