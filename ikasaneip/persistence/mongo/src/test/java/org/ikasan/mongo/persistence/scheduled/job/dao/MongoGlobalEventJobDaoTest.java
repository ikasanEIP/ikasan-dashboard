package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoGlobalEventJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoGlobalEventJobRepository;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoGlobalEventJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoGlobalEventJobRepository repository;
    private MongoGlobalEventJobDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        // Use Spring Data MongoDB repository factory
        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoGlobalEventJobRepository.class);

        dao = new MongoGlobalEventJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoGlobalEventJobRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_find_success() {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");

        dao.save(record);

        GlobalEventJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        GlobalEventJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<GlobalEventJobRecord> records = new ArrayList<>();
        records.add(createGlobalEventJobRecord("agent1", "job1", "context1"));
        records.add(createGlobalEventJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<GlobalEventJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all() {
        dao.save(createGlobalEventJobRecord("agent1", "job1", "context1"));
        dao.save(createGlobalEventJobRecord("agent2", "job2", "context2"));

        SearchResults<GlobalEventJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createGlobalEventJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<GlobalEventJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createGlobalEventJobRecord("agent1", "job1", "context1"));
        dao.save(createGlobalEventJobRecord("agent2", "job2", "context1"));
        dao.save(createGlobalEventJobRecord("agent3", "job3", "context2"));

        SearchResults<GlobalEventJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createGlobalEventJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<GlobalEventJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createGlobalEventJobRecord("agent1", "job1", "context1"));

        SearchResults<GlobalEventJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        GlobalEventJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test
    public void test_skip_contexts() {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");
        dao.save(record);

        List<String> childContexts = Arrays.asList("child1", "child2", "child3");
        dao.skip(record, childContexts, "skipUser");

        GlobalEventJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("skipUser", found.getModifiedBy());
        Assert.assertNotNull(found.getGlobalEventJob().getSkippedContexts());
        Assert.assertEquals(3, found.getGlobalEventJob().getSkippedContexts().size());
        Assert.assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("child1"));
        Assert.assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("child2"));
        Assert.assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("child3"));
    }

    @Test
    public void test_enable() {
        MongoGlobalEventJobRecordImpl record = createGlobalEventJobRecord("agent1", "job1", "context1");
        dao.save(record);

        dao.enable(record, "enableUser");

        GlobalEventJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("enableUser", found.getModifiedBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_wrong_type_throws_exception() {
        GlobalEventJobRecord invalidRecord = new GlobalEventJobRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public String getAgentName() { return "agent"; }
            @Override
            public void setAgentName(String agentName) {}
            @Override
            public String getJobName() { return "job"; }
            @Override
            public void setJobName(String jobName) {}
            @Override
            public String getDisplayName() { return "display"; }
            @Override
            public void setDisplayName(String displayName) {}
            @Override
            public String getContextName() { return "context"; }
            @Override
            public void setContextName(String contextName) {}
            @Override
            public GlobalEventJob getGlobalEventJob() { return null; }
            @Override
            public void setGlobalEventJob(GlobalEventJob globalEventJob) {}
            @Override
            public long getTimestamp() { return 0; }
            @Override
            public void setTimestamp(long timestamp) {}
            @Override
            public long getModifiedTimestamp() { return 0; }
            @Override
            public void setModifiedTimestamp(long modifiedTimestamp) {}
            @Override
            public String getModifiedBy() { return null; }
            @Override
            public void setModifiedBy(String modifiedBy) {}
        };

        dao.save(invalidRecord);
    }

    // Helper methods

    private MongoGlobalEventJobRecordImpl createGlobalEventJobRecord(String agentName, String jobName, String contextName) {
        GlobalEventJobImpl globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setAgentName(agentName);
        globalEventJob.setJobName(jobName);
        globalEventJob.setContextName(contextName);
        globalEventJob.setDisplayName("Display " + jobName);

        MongoGlobalEventJobRecordImpl record = new MongoGlobalEventJobRecordImpl();
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setGlobalEventJob(globalEventJob);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }
}
