package org.ikasan.data.synchronisation;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.bson.Document;
import org.ikasan.spec.entity.EntityDao;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SyncPipelineTest extends SyncTestSupport {

    private Document wiretapDocument(String id) {
        samples.wiretap(id);
        return mongoTemplate.getCollection(SyncCollections.IKASAN)
            .find(new Document("_id", "module-wiretap-" + id)).first();
    }

    @Test
    public void the_last_change_to_a_document_in_a_batch_wins() throws Exception {
        Document one = wiretapDocument("1");
        Document two = wiretapDocument("2");

        pipeline.apply(List.of(
            ChangeEvent.upsert(SyncCollections.IKASAN, one),
            ChangeEvent.delete(SyncCollections.IKASAN, "module-wiretap-1"),
            ChangeEvent.delete(SyncCollections.IKASAN, "module-wiretap-2"),
            ChangeEvent.upsert(SyncCollections.IKASAN, two)));

        assertEquals(List.of("module-wiretap-2"), solrIds(EntityDao.WIRETAP_TYPE));
    }

    @Test
    public void unknown_and_excluded_types_are_skipped() throws Exception {
        SyncPipeline excluding = newPipeline(List.of(EntityDao.WIRETAP_TYPE));
        Document unknown = new Document("_id", "x").append("type", "noSuchType");

        excluding.apply(List.of(ChangeEvent.upsert(SyncCollections.IKASAN, wiretapDocument("1")),
            ChangeEvent.upsert(SyncCollections.IKASAN, unknown)));

        assertTrue(env.solrIds("*:*").isEmpty());
    }

    @Test
    public void a_document_that_cannot_be_converted_is_skipped_and_the_rest_are_saved() throws Exception {
        Document good = wiretapDocument("1");
        Document bad = new Document(wiretapDocument("2"));
        bad.put("timestamp", new Document("not", "a number")); // the converter cannot read this as a long

        pipeline.apply(List.of(ChangeEvent.upsert(SyncCollections.IKASAN, good),
            ChangeEvent.upsert(SyncCollections.IKASAN, bad)));

        assertEquals(List.of("module-wiretap-1"), solrIds(EntityDao.WIRETAP_TYPE));
    }

    @Test
    public void a_transient_solr_failure_is_retried_until_it_succeeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        List<Document> saved = new ArrayList<>();
        SyncHandler flaky = handler("flaky", documents -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException(new SolrServerException("Solr is down"));
            }
            saved.addAll(documents);
        });
        SyncPipeline withFlaky = new SyncPipeline(Map.of("flaky", flaky), List.of(), solrDaos, 100);

        withFlaky.apply(List.of(ChangeEvent.upsert(SyncCollections.IKASAN, new Document("_id", "1").append("type", "flaky"))));

        assertEquals(3, attempts.get());
        assertEquals(1, saved.size());
    }

    @Test
    public void a_batch_that_cannot_be_written_is_split_and_only_the_failing_document_skipped() throws Exception {
        List<Object> saved = new ArrayList<>();
        SyncHandler rejecting = handler("rejecting", documents -> {
            if (documents.stream().anyMatch(d -> d.get("_id").equals("bad"))) {
                throw new IllegalStateException(new NumberFormatException("bad id"));
            }
            documents.forEach(d -> saved.add(d.get("_id")));
        });
        SyncPipeline withRejecting = new SyncPipeline(Map.of("rejecting", rejecting), List.of(), solrDaos, 100);

        withRejecting.apply(List.of(
            ChangeEvent.upsert(SyncCollections.IKASAN, new Document("_id", "a").append("type", "rejecting")),
            ChangeEvent.upsert(SyncCollections.IKASAN, new Document("_id", "bad").append("type", "rejecting")),
            ChangeEvent.upsert(SyncCollections.IKASAN, new Document("_id", "b").append("type", "rejecting"))));

        assertEquals(List.of("a", "b"), saved);
    }

    @Test
    public void only_failures_to_reach_solr_are_retried() {
        assertFalse(SyncPipeline.isRejectedBySolr(new RuntimeException(new SolrServerException("down"))));
        assertFalse(SyncPipeline.isRejectedBySolr(new RuntimeException(new IOException("connection reset"))));
        assertFalse(SyncPipeline.isRejectedBySolr(new SolrException(SolrException.ErrorCode.SERVER_ERROR, "500")));
        assertFalse(SyncPipeline.isRejectedBySolr(new SolrException(SolrException.ErrorCode.UNAUTHORIZED, "401")));
        assertTrue(SyncPipeline.isRejectedBySolr(new SolrException(SolrException.ErrorCode.BAD_REQUEST, "400")));
        assertTrue(SyncPipeline.isRejectedBySolr(new RuntimeException(new NumberFormatException("x"))));
    }

    @Test(timeout = 10_000)
    public void retrying_stops_when_the_thread_is_interrupted() throws Exception {
        SyncHandler alwaysDown = handler("down", documents -> {
            throw new IllegalStateException(new SolrServerException("Solr is down"));
        });
        SyncPipeline withDown = new SyncPipeline(Map.of("down", alwaysDown), List.of(), solrDaos, 100);
        Thread.currentThread().interrupt();
        try {
            withDown.apply(List.of(ChangeEvent.upsert(SyncCollections.IKASAN, new Document("_id", "1").append("type", "down"))));
            throw new AssertionError("expected InterruptedException");
        } catch (InterruptedException expected) {
            // stopped
        }
    }

    private static SyncHandler handler(String type, java.util.function.Consumer<List<Document>> upsert) {
        return new SyncHandler() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public boolean deletable() {
                return true;
            }

            @Override
            public void upsert(List<Document> documents) {
                upsert.accept(documents);
            }
        };
    }
}
