package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.ContextStartJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextStartJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextStartJobRepository;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
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
import java.util.List;

public class MongoContextStartJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoContextStartJobRepository repository;
    private MongoContextStartJobDao dao;

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
        repository = factory.getRepository(MongoContextStartJobRepository.class);

        dao = new MongoContextStartJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoContextStartJobRecordImpl.class);
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
        MongoContextStartJobRecordImpl record = createContextStartJobRecord("agent1", "job1", "context1");

        dao.save(record);

        ContextStartJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("CONTEXT_START_JOB", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoContextStartJobRecordImpl record = createContextStartJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        ContextStartJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        ContextStartJobRecord record = createContextStartJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        record = dao.findById(record.getId());

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<ContextStartJobRecord> records = new ArrayList<>();
        records.add(createContextStartJobRecord("agent1", "job1", "context1"));
        records.add(createContextStartJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<ContextStartJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all() {
        dao.save(createContextStartJobRecord("agent1", "job1", "context1"));
        dao.save(createContextStartJobRecord("agent2", "job2", "context2"));

        SearchResults<ContextStartJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createContextStartJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<ContextStartJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createContextStartJobRecord("agent1", "job1", "context1"));
        dao.save(createContextStartJobRecord("agent2", "job2", "context1"));
        dao.save(createContextStartJobRecord("agent3", "job3", "context2"));

        SearchResults<ContextStartJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createContextStartJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<ContextStartJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createContextStartJobRecord("agent1", "job1", "context1"));

        SearchResults<ContextStartJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoContextStartJobRecordImpl record = createContextStartJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        ContextStartJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    // Helper methods

    private MongoContextStartJobRecordImpl createContextStartJobRecord(String agentName, String jobName, String contextName) {
        ContextStartJobImpl contextStartJob = new ContextStartJobImpl();
        contextStartJob.setAgentName(agentName);
        contextStartJob.setJobName(jobName);
        contextStartJob.setContextName(contextName);
        contextStartJob.setDisplayName("Display " + jobName);

        MongoContextStartJobRecordImpl record = new MongoContextStartJobRecordImpl();
        record.setId(contextStartJob.getAgentName() + "_"
            + contextStartJob.getJobName() + "_" + contextStartJob.getContextName());
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setContextStartJob(contextStartJob);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }
}
