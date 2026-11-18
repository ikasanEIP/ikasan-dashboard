# Ikasan Dashboard Data Synchronisation

Copies every write made to the dashboard's Mongo database into Solr, through the Solr DAOs in `ikasan-solr-client`.
It is off by default.

```properties
ikasan.data-synchronisation.enabled=true
```

## How changes are detected

| Mode | When | What it sees |
|---|---|---|
| `change-stream` | Mongo is a replica set or sharded cluster | Every committed insert, replace, update and delete from any client, including deletes by query (housekeeping, deletes by context). Resumable: the position is stored after each batch reaches Solr, so a restart or a Solr outage loses nothing. |
| `command-listener` | Mongo is a standalone server | Only this application's writes: inserts, whole-document replacements (how the DAOs save) and deletes by `_id`. Deletes by query, writes by other clients and changes still queued when the application stops are repaired by the scheduled reconcile. |
| `auto` (default) | | Asks Mongo at startup (`hello`) and picks `change-stream` if it can, otherwise `command-listener`, logging which and why. |

`mode=change-stream` against a standalone server fails at startup with a message saying how to fix it.

### Turning a local mongod into a replica set

Change streams need a replica set; a single-node one is enough and keeps your data:

```
mongod --dbpath <your data directory> --replSet rs0          # always start it with --replSet from now on
mongosh --eval 'rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]})'   # once
```

## What is synchronised

All dashboard entities in the `ikasan` collection, routed by their `type` field, plus scheduled process events from
the `contextualisedScheduledProcessEventImpl` collection. Each type has a handler (`SyncHandlers`) that turns the
Mongo document into what the type's Solr DAO saves:

* most Mongo models implement the interface the Solr DAO takes, so the document is read with the Mongo converter and
  passed straight to the Solr DAO;
* security records and metadata are stored in Mongo in a different shape, so they are read back through their Mongo
  DAO;
* replay events and replay audits are copied field for field, keeping the Mongo id (their Solr DAOs can't build the
  id from the Mongo models).

Deletes are applied to Solr by id: for every type that can be deleted, the Solr id is the Mongo `_id`
(`SyncHandlerParityTest` checks this).

Types whose Solr DAO generates a new id on every save — metrics, the job lock cache audit, the context instance audit
aggregate and scheduled process events — are **insert-only**: they are written once, Mongo deletes are not applied to
them, and they expire through Solr's own housekeeping.

Types without a handler are logged once at WARN and skipped. Types can be skipped deliberately with
`excluded-types`.

## Reconcile

The reconciler loads every Mongo document into Solr and deletes Solr documents that no longer exist in Mongo. It runs:

* the first time the synchronisation starts (`reconcile.on-first-start`);
* on a schedule (`reconcile.cron`): by default nightly at 02:00 in `command-listener` mode, never in `change-stream`
  mode;
* when a change stream can no longer resume (Mongo no longer has the history, e.g. after a long outage);
* in `command-listener` mode, when the queue was full and changes were dropped;
* on request: `DataSynchronisationEngine.reconcile()`.

It runs on the synchronisation thread, between batches, so it never races with change processing.

## Failures

* Solr unavailable: the batch is retried with exponential back-off (up to `retry.max-backoff-ms`); in change-stream
  mode the stored position doesn't move, so nothing is lost.
* A document that can't be converted, or that Solr rejects (HTTP 4xx), is logged at ERROR with its id and the stack
  trace, and skipped; the rest of the batch is still written.
* The command listener never blocks a Mongo command: if its queue is full, changes are dropped with a WARN and a
  reconcile is requested.

## Several dashboard instances

Only the instance holding the lease synchronises (`lease.enabled`, on by default). The lease is a document in the
`ikasanDataSynchronisation` collection, renewed every third of `lease.duration-seconds`; if the holder dies, another
instance takes over once it expires. The same collection holds the change stream position and the last reconcile.

## Configuration

```properties
ikasan.data-synchronisation.enabled=false
ikasan.data-synchronisation.mode=auto                # auto | change-stream | command-listener
ikasan.data-synchronisation.batch-size=200
ikasan.data-synchronisation.batch-max-wait-ms=500
ikasan.data-synchronisation.queue-capacity=10000     # command-listener mode
ikasan.data-synchronisation.reconcile.on-first-start=true
ikasan.data-synchronisation.reconcile.cron=          # empty: the mode's default; "-": never
ikasan.data-synchronisation.excluded-types=
ikasan.data-synchronisation.lease.enabled=true
ikasan.data-synchronisation.lease.duration-seconds=30
ikasan.data-synchronisation.retry.max-backoff-ms=60000
```

The Solr connection and retention use the same properties as `SolrClientAutoConfiguration`: `solr.mode`, `solr.url`,
`solr.cloud.zk.hosts`, `solr.username`, `solr.password`, `solr.retention.days`,
`solr.scheduler.instance.retention.days`, `solr.joblockcacheaudit.retention.days`, `solr.connection.timeout.milli`.

The module creates its own Solr DAOs rather than loading `SolrClientAutoConfiguration`, whose bean names clash with
`MongoPersistenceAutoConfiguration`'s. It needs `MongoPersistenceAutoConfiguration`, so it only makes sense with Mongo
persistence.

## Tests

The tests use a Testcontainers Mongo replica set, the real Mongo DAOs, and an embedded Solr core built from the
`minimal` configset (the same one `ikasan-solr-client`'s tests use). Docker is required.
