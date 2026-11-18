package org.ikasan.data.synchronisation;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.ChangeStreamIterable;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Synchronises through a Mongo change stream on the database, filtered to the synchronised collections.
 *
 * <p>The stream's resume token is stored only after a batch is in Solr, so after a restart or a Solr outage the
 * synchronisation carries on exactly where it stopped; changes may be applied twice, never missed. Deletes by query
 * produce one delete event per document, so they are synchronised too.
 *
 * <p>If Mongo no longer has the history to resume from (the application was stopped for longer than the oplog
 * covers), the stream is restarted from now and a full reconcile is run.
 */
public class ChangeStreamSynchroniser extends Synchroniser {

    private static final Logger logger = LoggerFactory.getLogger(ChangeStreamSynchroniser.class);

    /** ChangeStreamHistoryLost. */
    static final int HISTORY_LOST = 286;
    /** ChangeStreamFatalError, e.g. the resume token refers to an event that no longer exists. */
    static final int FATAL_ERROR = 280;

    /** How often the token is stored when nothing changes, so it doesn't age out of the oplog while idle. */
    static final long IDLE_TOKEN_SAVE_INTERVAL_MS = 60_000;

    private final MongoTemplate mongoTemplate;

    public ChangeStreamSynchroniser(MongoTemplate mongoTemplate, SyncPipeline pipeline, Reconciler reconciler,
                                    SyncStateStore stateStore, DataSynchronisationProperties properties) {
        super(pipeline, reconciler, stateStore, properties);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SyncMode mode() {
        return SyncMode.CHANGE_STREAM;
    }

    @Override
    protected void run() throws InterruptedException {
        long backoff = SyncPipeline.INITIAL_BACKOFF_MS;
        while (shouldRun()) {
            try {
                stream();
                backoff = SyncPipeline.INITIAL_BACKOFF_MS;
            } catch (MongoCommandException e) {
                if (cannotResume(e)) {
                    logger.error("The change stream cannot resume from its stored position (Mongo no longer has that"
                        + " history), so changes may have been missed: restarting the stream from now and"
                        + " reconciling Solr with Mongo", e);
                    stateStore.clearResumeToken();
                    requestReconcile();
                } else {
                    backoff = pause(e, backoff);
                }
            } catch (MongoInterruptedException e) {
                throw new InterruptedException("Change stream interrupted");
            } catch (MongoException e) {
                backoff = pause(e, backoff);
            }
        }
    }

    /** Whether the stream cannot carry on from its stored position, so it must start again from now. */
    static boolean cannotResume(MongoCommandException e) {
        return e.getErrorCode() == HISTORY_LOST || e.getErrorCode() == FATAL_ERROR;
    }

    private long pause(MongoException e, long backoff) throws InterruptedException {
        logger.error(String.format("The change stream failed, reopening it in %d ms", backoff), e);
        Thread.sleep(backoff);
        return Math.min(backoff * 2, properties.getRetryMaxBackoffMs());
    }

    private void stream() throws InterruptedException {
        Optional<BsonDocument> resumeToken = stateStore.resumeToken();
        ChangeStreamIterable<Document> iterable = mongoTemplate.getDb()
            .watch(pipeline())
            .fullDocument(FullDocument.UPDATE_LOOKUP)
            .batchSize(properties.getBatchSize())
            .maxAwaitTime(properties.getBatchMaxWaitMs(), TimeUnit.MILLISECONDS);
        resumeToken.ifPresent(iterable::resumeAfter);

        try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = iterable.cursor()) {
            if (resumeToken.isEmpty()) {
                // first start (or history lost): remember where the stream begins, then load what is already there;
                // everything after this point arrives through the stream
                BsonDocument start = cursor.getResumeToken();
                if (start != null) {
                    stateStore.saveResumeToken(start);
                }
                firstLoadIfNeeded();
            }
            logger.info("Following the Mongo change stream{}", resumeToken.isPresent() ? " from the stored position" : "");

            long lastTokenSave = System.currentTimeMillis();
            BsonDocument savedToken = resumeToken.orElse(null);
            while (shouldRun()) {
                reconcileIfRequested();

                List<ChangeEvent> batch = new ArrayList<>();
                long deadline = System.currentTimeMillis() + properties.getBatchMaxWaitMs();
                while (batch.size() < properties.getBatchSize() && System.currentTimeMillis() < deadline) {
                    ChangeStreamDocument<Document> change = cursor.tryNext();
                    if (change == null) {
                        break;
                    }
                    toChangeEvent(change).ifPresent(batch::add);
                }
                pipeline.apply(batch);

                BsonDocument token = cursor.getResumeToken();
                boolean idleSaveDue = System.currentTimeMillis() - lastTokenSave > IDLE_TOKEN_SAVE_INTERVAL_MS;
                if (token != null && !token.equals(savedToken) && (!batch.isEmpty() || idleSaveDue || savedToken == null)) {
                    stateStore.saveResumeToken(token);
                    savedToken = token;
                    lastTokenSave = System.currentTimeMillis();
                }
            }
        }
    }

    private static List<Bson> pipeline() {
        return List.of(Aggregates.match(Filters.and(
            Filters.in("ns.coll", SyncCollections.SYNCHRONISED),
            Filters.in("operationType", List.of(OperationType.INSERT.getValue(), OperationType.UPDATE.getValue(),
                OperationType.REPLACE.getValue(), OperationType.DELETE.getValue())))));
    }

    static Optional<ChangeEvent> toChangeEvent(ChangeStreamDocument<Document> change) {
        String collection = change.getNamespace() == null ? null : change.getNamespace().getCollectionName();
        if (collection == null || change.getDocumentKey() == null) {
            return Optional.empty();
        }
        return switch (change.getOperationType()) {
            case INSERT, UPDATE, REPLACE -> Optional.ofNullable(change.getFullDocument())
                // a null full document means it was deleted before the update was looked up; its delete follows
                .map(document -> ChangeEvent.upsert(collection, document));
            case DELETE -> Optional.of(ChangeEvent.delete(collection, idOf(change.getDocumentKey().get("_id"))));
            default -> Optional.empty();
        };
    }

    static String idOf(BsonValue id) {
        if (id == null) {
            return null;
        }
        return switch (id.getBsonType()) {
            case STRING -> id.asString().getValue();
            case OBJECT_ID -> id.asObjectId().getValue().toHexString();
            case INT32 -> String.valueOf(id.asInt32().getValue());
            case INT64 -> String.valueOf(id.asInt64().getValue());
            default -> id.toString();
        };
    }
}
