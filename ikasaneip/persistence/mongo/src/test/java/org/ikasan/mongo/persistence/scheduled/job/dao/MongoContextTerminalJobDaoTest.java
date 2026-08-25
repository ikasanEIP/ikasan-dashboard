package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.ContextTerminalJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextTerminalJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextTerminalJobRepository;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
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

public class MongoContextTerminalJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoContextTerminalJobRepository repository;
    private MongoContextTerminalJobDao dao;

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
        repository = factory.getRepository(MongoContextTerminalJobRepository.class);

        dao = new MongoContextTerminalJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoContextTerminalJobRecordImpl.class);
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
        MongoContextTerminalJobRecordImpl record = createContextTerminalJobRecord("agent1", "job1", "context1");

        dao.save(record);

        ContextTerminalJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoContextTerminalJobRecordImpl record = createContextTerminalJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        ContextTerminalJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        ContextTerminalJobRecord record = createContextTerminalJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        record = dao.findById(record.getId());

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<ContextTerminalJobRecord> records = new ArrayList<>();
        records.add(createContextTerminalJobRecord("agent1", "job1", "context1"));
        records.add(createContextTerminalJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<ContextTerminalJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all() {
        dao.save(createContextTerminalJobRecord("agent1", "job1", "context1"));
        dao.save(createContextTerminalJobRecord("agent2", "job2", "context2"));

        SearchResults<ContextTerminalJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createContextTerminalJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<ContextTerminalJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createContextTerminalJobRecord("agent1", "job1", "context1"));
        dao.save(createContextTerminalJobRecord("agent2", "job2", "context1"));
        dao.save(createContextTerminalJobRecord("agent3", "job3", "context2"));

        SearchResults<ContextTerminalJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createContextTerminalJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<ContextTerminalJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createContextTerminalJobRecord("agent1", "job1", "context1"));

        SearchResults<ContextTerminalJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        ContextTerminalJobRecord record = createContextTerminalJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        ContextTerminalJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    // Helper methods

    private MongoContextTerminalJobRecordImpl createContextTerminalJobRecord(String agentName, String jobName, String contextName) {
        ContextTerminalJobImpl contextTerminalJob = new ContextTerminalJobImpl();
        contextTerminalJob.setAgentName(agentName);
        contextTerminalJob.setJobName(jobName);
        contextTerminalJob.setContextName(contextName);
        contextTerminalJob.setDisplayName("Display " + jobName);

        MongoContextTerminalJobRecordImpl record = new MongoContextTerminalJobRecordImpl();
        record.setId(contextTerminalJob.getAgentName() + "_"
            + contextTerminalJob.getJobName() + "_" + contextTerminalJob.getContextName());
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setContextTerminalJob(contextTerminalJob);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }
}
