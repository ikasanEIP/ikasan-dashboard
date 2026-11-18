package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.metrics.MetricsDao;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReconcilerTest extends SyncTestSupport {

    @Test
    public void loads_everything_into_an_empty_solr_in_batches() throws Exception {
        for (int i = 2; i <= 8; i++) { // batch size is 3; oneOfEach adds wiretap 1
            samples.wiretap(String.valueOf(i));
        }
        samples.oneOfEach();

        Reconciler.Result result = reconciler.reconcile();

        assertEquals(8, solrIds(EntityDao.WIRETAP_TYPE).size());
        assertEquals(mongoIds("securityUser"), solrIds("securityUser"));
        assertEquals(1, env.solrIds("type:\"" + SyncCollections.SCHEDULED_PROCESS_EVENT_TYPE + "\"").size());
        assertEquals(0, result.orphansRemoved());
        assertTrue(stateStore.lastFullLoad().isPresent());
    }

    @Test
    public void removes_solr_documents_that_are_no_longer_in_mongo() throws Exception {
        samples.wiretap("1");
        samples.wiretap("2");
        reconciler.reconcile();
        mongoTemplate.getCollection(SyncCollections.IKASAN).deleteOne(new Document("_id", "module-wiretap-1"));

        Reconciler.Result result = reconciler.reconcile();

        assertEquals(1, result.orphansRemoved());
        assertEquals(List.of("module-wiretap-2"), solrIds(EntityDao.WIRETAP_TYPE));
    }

    @Test
    public void insert_only_types_are_not_duplicated_by_a_second_reconcile() throws Exception {
        samples.metric("flow-1");
        samples.metric("flow-2");

        reconciler.reconcile();
        reconciler.reconcile();

        assertEquals(2, solrIds(MetricsDao.METRIC_ENTITY_TYPE).size());
    }

    @Test
    public void excluded_types_are_not_loaded() throws Exception {
        pipeline = newPipeline(List.of(EntityDao.WIRETAP_TYPE));
        reconciler = new Reconciler(mongoTemplate, solrDaos, pipeline, stateStore, 3);
        samples.wiretap("1");
        samples.policy("policy-1");

        reconciler.reconcile();

        assertTrue(solrIds(EntityDao.WIRETAP_TYPE).isEmpty());
        assertEquals(1, solrIds("securityPolicy").size());
    }
}
