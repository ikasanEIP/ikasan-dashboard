package org.ikasan.data.synchronisation;

import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.bson.Document;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.solr.SolrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Applies Mongo changes to Solr; shared by every synchronisation mode and the reconciler.
 *
 * <p>Within a batch only the last change to each document counts. Upserts are grouped by {@code type} and passed to
 * that type's {@link SyncHandler}; deletes are applied to Solr by id, which works for every type because Solr's
 * unique key is the id within the single {@code ikasan} core.
 *
 * <p>If Solr cannot be written to, the whole batch is retried with exponential back-off until it succeeds or the
 * calling thread is interrupted, so the caller only moves on (e.g. stores its change stream resume token) once
 * the batch is in Solr.
 */
public class SyncPipeline {

    private static final Logger logger = LoggerFactory.getLogger(SyncPipeline.class);

    static final long INITIAL_BACKOFF_MS = 250;

    private final Map<String, SyncHandler> handlers;
    private final Set<String> excludedTypes;
    private final SolrDaos solr;
    private final long maxBackoffMs;
    private final Set<String> unsupportedTypesLogged = ConcurrentHashMap.newKeySet();

    private final AtomicLong upserted = new AtomicLong();
    private final AtomicLong deleted = new AtomicLong();

    public SyncPipeline(Map<String, SyncHandler> handlers, Collection<String> excludedTypes, SolrDaos solr,
                        long maxBackoffMs) {
        this.handlers = Map.copyOf(handlers);
        this.excludedTypes = Set.copyOf(excludedTypes);
        this.solr = solr;
        this.maxBackoffMs = maxBackoffMs;
    }

    /**
     * Apply the changes to Solr, retrying until they have all been written.
     *
     * @throws InterruptedException if the thread is interrupted while waiting to retry
     */
    public void apply(List<ChangeEvent> events) throws InterruptedException {
        if (events.isEmpty()) {
            return;
        }
        // last change per document wins, keeping the order documents were first seen in
        Map<String, ChangeEvent> latest = new LinkedHashMap<>();
        for (ChangeEvent event : events) {
            String key = event.collection() + '\u0000' + event.id();
            latest.remove(key);
            latest.put(key, event);
        }

        Map<SyncHandler, List<Document>> upserts = new LinkedHashMap<>();
        List<String> deletes = new ArrayList<>();
        for (ChangeEvent event : latest.values()) {
            if (event.operation() == ChangeEvent.Operation.DELETE) {
                deletes.add(event.id());
            } else {
                SyncHandler handler = handlerFor(event.collection(), event.document());
                if (handler != null) {
                    upserts.computeIfAbsent(handler, h -> new ArrayList<>()).add(event.document());
                }
            }
        }

        for (Map.Entry<SyncHandler, List<Document>> entry : upserts.entrySet()) {
            upsert(entry.getKey(), entry.getValue());
        }
        if (!deletes.isEmpty()) {
            withRetry("delete " + deletes.size() + " documents", () -> deleteFromSolr(deletes));
            deleted.addAndGet(deletes.size());
        }
    }

    /**
     * Save documents of one type, retrying until they are written; used by the reconciler.
     */
    public void upsert(SyncHandler handler, List<Document> documents) throws InterruptedException {
        if (documents.isEmpty()) {
            return;
        }
        try {
            withRetry("save " + documents.size() + " [" + handler.type() + "] documents",
                () -> handler.upsert(documents));
            upserted.addAndGet(documents.size());
        } catch (RejectedBySolrException e) {
            if (documents.size() > 1) {
                // find the document(s) Solr rejects, and save the rest
                for (Document document : documents) {
                    upsert(handler, List.of(document));
                }
            } else {
                logger.error(String.format("Skipping [%s] document [%s]: it cannot be written to Solr", handler.type(),
                    documents.get(0).get("_id")), e.getCause());
            }
        }
    }

    /**
     * Delete documents from Solr by id, retrying until done; used by the reconciler.
     */
    public void delete(List<String> ids) throws InterruptedException {
        if (!ids.isEmpty()) {
            withRetry("delete " + ids.size() + " documents", () -> deleteFromSolr(ids));
            deleted.addAndGet(ids.size());
        }
    }

    /**
     * @return the handler for a document, or {@code null} if its type is excluded or has no Solr counterpart
     */
    public SyncHandler handlerFor(String collection, Document document) {
        String type = SyncCollections.SCHEDULED_PROCESS_EVENT.equals(collection)
            ? SyncCollections.SCHEDULED_PROCESS_EVENT_TYPE
            : document == null ? null : document.getString(EntityFields.TYPE);
        if (type == null || excludedTypes.contains(type)) {
            return null;
        }
        SyncHandler handler = handlers.get(type);
        if (handler == null && unsupportedTypesLogged.add(type)) {
            logger.warn("Documents of type [{}] are not synchronised to Solr: there is no handler for that type", type);
        }
        return handler;
    }

    /** @return the handlers that are not excluded */
    public Collection<SyncHandler> handlers() {
        return handlers.values().stream().filter(h -> !excludedTypes.contains(h.type())).toList();
    }

    public long upsertedCount() {
        return upserted.get();
    }

    public long deletedCount() {
        return deleted.get();
    }

    private void deleteFromSolr(List<String> ids) {
        try {
            UpdateRequest request = new UpdateRequest();
            request.setBasicAuthCredentials(solr.settings().username(), solr.settings().password());
            request.deleteById(ids);
            request.process(solr.solrClient(), SolrConstants.CORE);
            request.commit(solr.solrClient(), SolrConstants.CORE);
        } catch (Exception e) {
            throw new IllegalStateException("Could not delete documents from Solr", e);
        }
    }

    /**
     * Whether retrying the same request can never succeed. Only failures to reach or be served by Solr are worth
     * retrying: an I/O or {@link SolrServerException}, or a {@link SolrException} other than a 4xx rejection of the
     * request (authentication, timeout and throttling excepted). Anything else (e.g. a Solr DAO failing to convert
     * an entity) fails the same way every time.
     */
    static boolean isRejectedBySolr(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SolrException solrException) {
                int code = solrException.code();
                return code >= 400 && code < 500 && code != 401 && code != 403 && code != 408 && code != 429;
            }
            if (cause instanceof SolrServerException || cause instanceof IOException) {
                return false;
            }
        }
        return true;
    }

    /** Solr rejected a request, or it failed for a reason that is not transient; retrying it cannot succeed. */
    static final class RejectedBySolrException extends RuntimeException {
        RejectedBySolrException(Throwable cause) {
            super(cause);
        }
    }

    private void withRetry(String description, Runnable action) throws InterruptedException {
        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; ; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted before attempting to " + description);
            }
            try {
                action.run();
                if (attempt > 1) {
                    logger.info("Succeeded to {} on attempt {}", description, attempt);
                }
                return;
            } catch (RuntimeException e) {
                if (isRejectedBySolr(e)) {
                    throw new RejectedBySolrException(e);
                }
                logger.error(String.format("Failed to %s in Solr (attempt %d), retrying in %d ms", description,
                    attempt, backoff), e);
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, maxBackoffMs);
            }
        }
    }
}
