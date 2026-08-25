package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.BridgingJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoBridgingJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoBridgingJobRepository;
import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.scheduled.job.model.BridgingJobRecord;
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

public class MongoBridgingJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoBridgingJobRepository repository;
    private MongoBridgingJobDao dao;

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
        repository = factory.getRepository(MongoBridgingJobRepository.class);

        dao = new MongoBridgingJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoBridgingJobRecordImpl.class);
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
        MongoBridgingJobRecordImpl record = createBridgingJobRecord("agent1", "job1", "context1");

        dao.save(record);

        BridgingJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("BRIDGING_JOB", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoBridgingJobRecordImpl record = createBridgingJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        BridgingJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        BridgingJobRecord record = createBridgingJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        record = dao.findById(record.getId());

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<BridgingJobRecord> records = new ArrayList<>();
        records.add(createBridgingJobRecord("agent1", "job1", "context1"));
        records.add(createBridgingJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<BridgingJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all() {
        dao.save(createBridgingJobRecord("agent1", "job1", "context1"));
        dao.save(createBridgingJobRecord("agent2", "job2", "context2"));

        SearchResults<BridgingJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createBridgingJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<BridgingJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createBridgingJobRecord("agent1", "job1", "context1"));
        dao.save(createBridgingJobRecord("agent2", "job2", "context1"));
        dao.save(createBridgingJobRecord("agent3", "job3", "context2"));

        SearchResults<BridgingJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createBridgingJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<BridgingJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createBridgingJobRecord("agent1", "job1", "context1"));

        SearchResults<BridgingJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoBridgingJobRecordImpl record = createBridgingJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        BridgingJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    // Helper methods
    private MongoBridgingJobRecordImpl createBridgingJobRecord(String agentName, String jobName, String contextName) {
        BridgingJobImpl bridgingJob = new BridgingJobImpl();
        bridgingJob.setAgentName(agentName);
        bridgingJob.setJobName(jobName);
        bridgingJob.setContextName(contextName);
        bridgingJob.setDisplayName("Display " + jobName);

        MongoBridgingJobRecordImpl record = new MongoBridgingJobRecordImpl();
        record.setId(bridgingJob.getAgentName() + "_"
            + bridgingJob.getJobName() + "_" + bridgingJob.getContextName());
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setBridgingJob(bridgingJob);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }
}
