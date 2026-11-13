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

## Indexes

All index definitions are shipped as JSON in
[`src/main/resources/mongo/indexes`](src/main/resources/mongo/indexes) and applied with one script.

| File | Collection | Kind |
|------|------------|------|
| `ikasan.indexes.json` | `ikasan` (every dashboard entity) | standard (B-tree) indexes, as a `createIndexes` command |
| `contextualisedScheduledProcessEventImpl.indexes.json` | `contextualisedScheduledProcessEventImpl` (scheduled process events) | standard (B-tree) indexes, as a `createIndexes` command |
| `ikasan.search.json` | `ikasan` | Atlas Search index definition |
| `contextualisedScheduledProcessEventImpl.search.json` | `contextualisedScheduledProcessEventImpl` | Atlas Search index definition |
| `apply-indexes.js` | | mongosh script that applies all of the above |

The files are also packaged in the module jar under `mongo/indexes/`.

> **Indexes are not created automatically.** The entity classes carry no `@Indexed` annotations: all entities share
> the `ikasan` collection and reuse the same stored field names, so per-entity annotations cannot describe the right
> indexes (an earlier `@Indexed(unique = true)` on the scheduled context name would have become a unique index on
> `moduleName` across every document type). Create the indexes from the shipped files, and leave Spring Data's
> `auto-index-creation` off.

### Applying the indexes

```bash
cd src/main/resources/mongo/indexes      # or extract mongo/indexes/ from the jar

# MongoDB Atlas (standard + Atlas Search indexes)
mongosh "mongodb+srv://<user>@<cluster>/<database>" --file apply-indexes.js

# Self-managed MongoDB (standard indexes only)
ATLAS_SEARCH=false mongosh "mongodb://<host>:27017/<database>" --file apply-indexes.js
```

| Variable | Default | Description |
|----------|---------|-------------|
| `INDEX_DIR` | the script's directory | Directory holding the index JSON files |
| `ATLAS_SEARCH` | `true` | Set to `false` to skip the Atlas Search indexes |
| `SEARCH_INDEX` | `default` | Atlas Search index name; must match `mongodb.atlas.search.index-name` |

The script is idempotent and safe to re-run after an upgrade:
- **New standard indexes** are created, and existing identical ones are left alone. Index builds on a populated
  collection don't block reads or writes, but plan large builds outside peak hours.
- **Atlas Search indexes** are updated in place if they already exist; Atlas rebuilds them in the background and keeps
  serving the old version until the new one is ready.
- If a standard index with the same name but different keys or options already exists, the script reports the
  conflict and exits non-zero. Drop the old index (`db.ikasan.dropIndex("<name>")`) and re-run.

**Upgrading from the indexes previously listed in this README:** those used field names that do not exist in the
stored documents (`createdDateTime`, `subject`, `username`, `contextName`, `jobName`, ...), so they were never used by
any query. List the existing indexes with `db.ikasan.getIndexes()` and drop any index not in `ikasan.indexes.json`;
unused indexes only slow down writes.

### Standard indexes on `ikasan`

Every DAO query filters on `type` first (all entities share the collection), so the compound indexes lead with
`type`, followed by equality fields, then the sort field, then range fields (the MongoDB ESR rule). Field names are
the *stored* names (see `org.ikasan.spec.entity.EntityFields`): `timestamp` is the created time, `moduleName` /
`flowName` / `componentName` are reused by the scheduler entities for context, job and instance names.

| Index | Key | Queries served |
|-------|-----|----------------|
| `type_1_timestamp_-1` | `{type: 1, timestamp: -1}` | Lists by type within a time window, newest first: wiretaps, errors, exclusions, replays, system events, metrics, scheduled contexts |
| `type_1_moduleName_1_timestamp_-1` | `{type: 1, moduleName: 1, timestamp: -1}` | One module or context, newest first: metrics by module, context instances / scheduled contexts / profiles by context name, scheduler jobs by agent |
| `type_1_moduleName_1_flowName_1_timestamp_-1` | `{type: 1, moduleName: 1, flowName: 1, timestamp: -1}` | Module and flow filters with a time window: errors, exclusions, hospital, replays, wiretaps, metrics by module and flow; context views (parent + context) |
| `type_1_componentName_1_flowName_1` | `{type: 1, componentName: 1, flowName: 1}` | Scheduler jobs, notifications and profiles by context; job instances by context-instance id (including the status aggregations); job by context and job name; job lists sorted by job name |
| `type_1_flowName_1` | `{type: 1, flowName: 1}` | Context-instance audit aggregates by context-instance id; job-instance lookups by context name |
| `type_1_status_1` | `{type: 1, status: 1}` | Context instances by status |
| `type_1_name_1` | `{type: 1, name: 1}` | Users, principals, roles, policies and authentication methods by name, and their name-sorted lists |
| `type_1_expiry_1` | `{type: 1, expiry: 1}` | Per-type housekeeping (`type = X and expiry < now`) |
| `expiry_1` | `{expiry: 1}` | General housekeeping across all types (`MongoGeneralDaoImpl.removeExpired`) |
| `type_1_harvestReceivedTimestamp_1` | `{type: 1, harvestReceivedTimestamp: 1}` | Searches by harvest-received time (`searchByHarvestReceivedTime`) |
| `errorUri_1` (sparse) | `{errorUri: 1}` | Error / exclusion / hospital lookups and deletes by error URI; the event-id search (`$or` over `event` / `errorUri`) |
| `event_1` | `{event: 1}` | Wiretap / exclusion lookups by event id; the event-id search |

Notes:
- Case-insensitive substring filters (`contains`, e.g. actor or job-name filters) cannot use a B-tree index.
  On Atlas they are answered by the search index; on self-managed MongoDB they are evaluated on the documents
  already narrowed by the indexes above.
- **No TTL index.** `expiry` is stored as epoch milliseconds (a number), and MongoDB TTL indexes only act on BSON
  dates, so a TTL index on `expiry` would never delete anything. Expired documents are removed by the dashboard
  housekeeping jobs, using `type_1_expiry_1` / `expiry_1`.
- Security collections (users, roles, ...) are small; their array membership queries (`rolesRelatedEntity`,
  `principalRelatedEntity`, ...) are served by the `type` prefix of the indexes above and need no dedicated index.

### Standard indexes on `contextualisedScheduledProcessEventImpl`

Scheduled process events are stored in their own collection (Spring's default name for
`ContextualisedScheduledProcessEventImpl`).

| Index | Key | Queries served |
|-------|-----|----------------|
| `fireTime_-1` | `{fireTime: -1}` | Time-window lists sorted by fire time; housekeeping (`fireTime < threshold`) |
| `agentName_1_fireTime_-1` | `{agentName: 1, fireTime: -1}` | Events for the accessible agents in a time window, sorted by fire time |
| `agentName_1_jobGroup_1_jobName_1_fireTime_-1` | `{agentName: 1, jobGroup: 1, jobName: 1, fireTime: -1}` | Events for one agent / job group / job in a time window |

### Verifying the indexes

`MongoIndexDefinitionsTest` applies the files with `apply-indexes.js` (twice, to check it is idempotent) to a MongoDB
7.0 container, then `explain`s the query shape of every DAO query above. It asserts that each one uses the intended
index, never scans the collection and, where the DAO sorts, never sorts in memory. When you add a query or change
a filter, add its shape to that test.

`AtlasSearchDaoAtlasTest` builds its indexes (including both Atlas Search indexes) with the same script on Atlas Local.

To check a live deployment, run `db.ikasan.aggregate([{ $indexStats: {} }])` to see how often each index is used, and
`db.ikasan.find(<filter>).sort(<sort>).explain()` for individual queries (look for `IXSCAN` and no `COLLSCAN` /
`SORT` stage).

## Atlas Search Index

If using MongoDB Atlas with Atlas Search enabled (`mongodb.atlas.search.enabled=true`), each searched collection needs a
search index named `default` (or the name in `mongodb.atlas.search.index-name`). `apply-indexes.js` creates them
(see [Applying the indexes](#applying-the-indexes)) from:

- [`ikasan.search.json`](src/main/resources/mongo/indexes/ikasan.search.json): the `ikasan` collection
- [`contextualisedScheduledProcessEventImpl.search.json`](src/main/resources/mongo/indexes/contextualisedScheduledProcessEventImpl.search.json):
  scheduled process events (`agentName`, `jobName`, `jobGroup`, `commandLine`, `outcome`, `resultOutput`,
  `resultError`, `fireTime`, `successful`)

The DAOs push **all** filtering, sorting and counting into the `$search` stage (see
[Atlas Search Query Helper](#atlas-search-query-helper)), so every field a DAO filters or sorts on must be in the index
with the right type. The definition provides:

| Field kind | Index type | Used for |
|------------|------------|----------|
| Discriminators and names (`type`, `moduleName`, `flowName`, `componentName`, `status`, `actor`, `errorUri`, ...) | `token` | `equals` / `in` filters and in-index `sort` |
| The same name fields | `string` with the `lowercaseKeyword` analyzer | case-insensitive substring (`*term*`) matching |
| Free text (`payload`, `errorDetail`, `errorMessage`, `event`, ...) | `string` with `lucene.standard` | full-text / per-token wildcard search |
| Timestamps (`timestamp`, `startTime`, `endTime`, `expiry`, ...) | `number` (and `date` for `timestamp`, which system events store as a BSON date) | `range` filters and `sort` |
| Flags (`disabled`, `participatesInLock`, ...) | `boolean` | `equals` filters |

**Upgrading from the previous index definition:** the earlier README definition used field names that do not exist in
the stored documents (`createdDateTime`, `payloadContent`, `subject`, `action`) and indexed name fields only as
`string`. `equals`/`in` filters and sorting require `token` fields, so the index **must be replaced** with the shipped
definition; with the old one, searches that use them fail.

If you add a new filter or sort field to a DAO, add it to the index definition and, for sort fields, to
`AtlasSearchQueryHelper.DEFAULT_SORTABLE_FIELDS`. Sorting on a field that is not listed there is executed with a
standard query instead of `$search`.

### Creating the Search Indexes manually

`apply-indexes.js` is the recommended way. Alternatively, for each `<collection>.search.json` file:

- **Atlas UI:** *Search* tab → *Create Search Index* → *JSON Editor* → select the collection → paste the file
  contents → *Create Search Index*.
- **mongosh:** `db.<collection>.createSearchIndex("default", <file contents>)`, or `updateSearchIndex(...)` to replace
  an existing one.
- **Atlas CLI:** wrap the definition with `"name": "default"`, `"database": "<database>"` and
  `"collectionName": "<collection>"`, then run
  `atlas clusters search indexes create --clusterName <cluster> --file <wrapped-file>.json`.

Search indexes build asynchronously. Until a new index is queryable, searches against it fail, so create the index
before deploying a dashboard version that needs it.

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
3. Create the standard indexes with `ATLAS_SEARCH=false mongosh ... --file apply-indexes.js` (see [Applying the indexes](#applying-the-indexes))

```properties
# Disable Atlas Search for on-premises MongoDB
mongodb.atlas.search.enabled=false
```

Even with Atlas Search enabled, the module will automatically fall back to regex-based search if it detects that Atlas Search is not available (e.g., when connecting to a non-Atlas MongoDB instance).

## Atlas Search Query Helper

`AtlasSearchQueryHelper` executes searches described by an `AtlasSearchFilter`. A single filter definition renders
either as Atlas Search operators or as standard `Criteria` (for the regex fallback), so both paths share the same
semantics.

### Building a filter

```java
AtlasSearchFilter filter = AtlasSearchFilter.builder()
    .equals(EntityFields.TYPE, "EXCLUSION")                          // exact match       -> equals
    .in(EntityFields.MODULE_NAME, moduleNames)                       // any of            -> in
    .rangeMillis(EntityFields.CREATED_DATE_TIME, from, until)        // inclusive range   -> range
    .anyOf(AtlasSearchFilter.eq(EntityFields.EVENT, id),             // OR of clauses     -> compound.should
           AtlasSearchFilter.eq(EntityFields.ERROR_URI, id))
    .contains(EntityFields.STATUS, statusTerm)                       // LIKE '%term%' on one field
    .containsAny(List.of("payload", "errorUri"), searchString)       // LIKE '%term%' on any field
    .text(List.of("payload", "errorMessage"), searchString)          // analysed full-text match
    .negateText(false)                                               // true -> exclude text matches
    .build();
```

`null`/empty values are ignored, so optional filter fields need no guards. User input is escaped (wildcard
metacharacters for Atlas, `Pattern.quote` for regex) and substring matches are case-insensitive.

### Executing a search

```java
AtlasSearchQueryHelper.SearchQueryResult<MyEntity> result = atlasSearchQueryHelper.search(
    filter,
    Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME),
    offset, limit,
    true,              // include total count (-1 is returned when false)
    MyEntity.class);
```

When the filter contains search terms and Atlas Search is available, this runs **one** aggregation:

```
{ $search: { index: "default",
             compound: { must:   [ <search terms> ],
                         filter: [ <type/module/flow/time/... restrictions> ] },
             sort:  { timestamp: -1 },
             count: { type: "total" } } }
{ $skip: offset } { $limit: limit }
{ $set: { _searchMeta: "$$SEARCH_META" } }
```

There is no `$match`, `$sort` or separate `$count` after `$search`. Filters are resolved in the Lucene index, so mongod
only fetches the documents of the requested page, and the total comes from the index. If the page is empty because the
offset is past the end, the total is read with `$searchMeta`, which only touches the index.

Filters without search terms run as a standard indexed query, because a plain B-tree lookup is cheaper than
`$search`.

### Automatic Fallback

The helper uses a standard query (regexes for the text clauses) when:
- Atlas Search is disabled via configuration (`mongodb.atlas.search.enabled=false`)
- Atlas Search is unavailable (e.g. connecting to non-Atlas MongoDB); this is detected once and remembered
- the requested sort field is not sortable in the search index (see `DEFAULT_SORTABLE_FIELDS`)
