package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.junit.Before;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Wires the synchronisation components against the shared {@link TestEnvironment}, emptied before each test.
 */
public abstract class SyncTestSupport {

    protected TestEnvironment env;
    protected MongoTemplate mongoTemplate;
    protected SampleData samples;
    protected SolrDaos solrDaos;
    protected DataSynchronisationProperties properties;
    protected SyncPipeline pipeline;
    protected SyncStateStore stateStore;
    protected Reconciler reconciler;

    @Before
    public void setUpSync() throws Exception {
        env = TestEnvironment.get();
        env.reset();
        mongoTemplate = env.mongoTemplate();
        samples = new SampleData(env);
        solrDaos = env.solrDaos();
        properties = new DataSynchronisationProperties();
        properties.setBatchMaxWaitMs(100);
        properties.setRetryMaxBackoffMs(500);
        pipeline = newPipeline(List.of());
        stateStore = new SyncStateStore(mongoTemplate);
        reconciler = new Reconciler(mongoTemplate, solrDaos, pipeline, stateStore, 3);
    }

    protected SyncPipeline newPipeline(List<String> excludedTypes) {
        return new SyncPipeline(SyncHandlers.create(mongoTemplate.getConverter(), solrDaos, env.lookups()),
            excludedTypes, solrDaos, properties.getRetryMaxBackoffMs());
    }

    /** @return every document of the ikasan collection */
    protected List<Document> mongoDocuments() {
        return mongoTemplate.getCollection(SyncCollections.IKASAN).find().into(new ArrayList<>());
    }

    protected List<String> mongoIds(String type) {
        return mongoTemplate.getCollection(SyncCollections.IKASAN).find(new Document("type", type))
            .map(d -> String.valueOf(d.get("_id"))).into(new ArrayList<>()).stream().sorted().toList();
    }

    protected List<String> solrIds(String type) throws Exception {
        return env.solrIds("type:\"" + type + "\"");
    }

    /** Wait up to 30 seconds for the condition. */
    static void await(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for: " + description);
            }
            Thread.sleep(100);
        }
    }

    /** {@link #solrIds} without the checked exception, for {@link #await}. */
    protected List<String> solrIdsUnchecked(String type) {
        try {
            return solrIds(type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
