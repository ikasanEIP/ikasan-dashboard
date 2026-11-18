package org.ikasan.data.synchronisation;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.solr.SolrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Brings Solr in line with Mongo: loads every Mongo document into Solr and removes Solr documents that no longer
 * exist in Mongo.
 *
 * <p>It does the first load when the synchronisation starts for the first time, and repairs what the command
 * listener cannot see (deletes by query, writes by other applications, changes lost when the application stopped).
 *
 * <p>Types that are not {@link SyncHandler#deletable() deletable} get a new Solr id on every save, so re-sending them
 * would duplicate them: they are only loaded when Solr has none of that type, and are never treated as orphans.
 */
public class Reconciler {

    private static final Logger logger = LoggerFactory.getLogger(Reconciler.class);

    private final MongoTemplate mongoTemplate;
    private final SolrDaos solr;
    private final SyncPipeline pipeline;
    private final SyncStateStore stateStore;
    private final int batchSize;

    public Reconciler(MongoTemplate mongoTemplate, SolrDaos solr, SyncPipeline pipeline, SyncStateStore stateStore,
                      int batchSize) {
        this.mongoTemplate = mongoTemplate;
        this.solr = solr;
        this.pipeline = pipeline;
        this.stateStore = stateStore;
        this.batchSize = batchSize;
    }

    public record Result(long documentsLoaded, long orphansRemoved) {
    }

    /**
     * Load everything and remove orphans.
     *
     * @throws InterruptedException if interrupted, e.g. because the application is stopping
     */
    public synchronized Result reconcile() throws InterruptedException {
        long start = System.currentTimeMillis();
        logger.info("Reconciling Solr with Mongo");
        long loaded = fullLoad();
        long removed = removeOrphans();
        stateStore.saveFullLoad(new Date(), loaded, removed);
        logger.info("Reconciled Solr with Mongo in {} ms: {} documents written, {} orphans removed",
            System.currentTimeMillis() - start, loaded, removed);
        return new Result(loaded, removed);
    }

    /**
     * Write every Mongo document of every synchronised type to Solr.
     *
     * @return the number of documents written
     */
    public long fullLoad() throws InterruptedException {
        long total = 0;
        for (SyncHandler handler : pipeline.handlers()) {
            if (!handler.deletable() && solrCount(handler.type()) > 0) {
                logger.info("Not loading [{}]: Solr already has documents of this type and their ids differ from"
                    + " Mongo's, so loading them again would duplicate them", handler.type());
                continue;
            }
            long count = 0;
            FindIterable<Document> documents = mongoTemplate.getCollection(handler.collection())
                .find(filterFor(handler))
                .sort(Sorts.ascending("_id"))
                .batchSize(batchSize);
            List<Document> batch = new ArrayList<>(batchSize);
            try (MongoCursor<Document> cursor = documents.iterator()) {
                while (cursor.hasNext()) {
                    checkInterrupted();
                    batch.add(cursor.next());
                    if (batch.size() == batchSize) {
                        pipeline.upsert(handler, batch);
                        count += batch.size();
                        batch = new ArrayList<>(batchSize);
                    }
                }
            }
            pipeline.upsert(handler, batch);
            count += batch.size();
            if (count > 0) {
                logger.info("Loaded {} [{}] documents into Solr", count, handler.type());
            }
            total += count;
        }
        return total;
    }

    /**
     * Delete Solr documents of synchronised, deletable types that no longer exist in Mongo.
     *
     * @return the number of documents removed
     */
    public long removeOrphans() throws InterruptedException {
        long total = 0;
        for (SyncHandler handler : pipeline.handlers()) {
            if (!handler.deletable()) {
                continue;
            }
            long removed = 0;
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            while (true) {
                checkInterrupted();
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("q", EntityFields.TYPE + ":\"" + handler.type() + "\"");
                params.set("fl", EntityFields.ID);
                params.set("sort", EntityFields.ID + " asc");
                params.set("rows", batchSize);
                params.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse response = query(params);

                List<String> solrIds = new ArrayList<>();
                for (SolrDocument document : response.getResults()) {
                    solrIds.add(String.valueOf(document.getFieldValue(EntityFields.ID)));
                }
                List<String> orphans = missingFromMongo(handler.collection(), solrIds);
                pipeline.delete(orphans);
                removed += orphans.size();

                String next = response.getNextCursorMark();
                if (next == null || next.equals(cursorMark)) {
                    break;
                }
                cursorMark = next;
            }
            if (removed > 0) {
                logger.info("Removed {} [{}] documents from Solr that are no longer in Mongo", removed,
                    handler.type());
            }
            total += removed;
        }
        return total;
    }

    private List<String> missingFromMongo(String collection, List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Set<String> present = new HashSet<>();
        mongoTemplate.getCollection(collection)
            .find(Filters.in("_id", ids))
            .projection(Projections.include("_id"))
            .forEach(document -> present.add(String.valueOf(document.get("_id"))));
        return ids.stream().filter(id -> !present.contains(id)).toList();
    }

    private long solrCount(String type) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", EntityFields.TYPE + ":\"" + type + "\"");
        params.set("rows", 0);
        return query(params).getResults().getNumFound();
    }

    private QueryResponse query(ModifiableSolrParams params) {
        try {
            QueryRequest request = new QueryRequest(params);
            request.setBasicAuthCredentials(solr.settings().username(), solr.settings().password());
            return request.process(solr.solrClient(), SolrConstants.CORE);
        } catch (Exception e) {
            throw new IllegalStateException("Could not query Solr with " + params, e);
        }
    }

    private static Bson filterFor(SyncHandler handler) {
        return SyncCollections.IKASAN.equals(handler.collection())
            ? Filters.eq(EntityFields.TYPE, handler.type())
            : Filters.empty();
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Reconcile interrupted");
        }
    }
}
