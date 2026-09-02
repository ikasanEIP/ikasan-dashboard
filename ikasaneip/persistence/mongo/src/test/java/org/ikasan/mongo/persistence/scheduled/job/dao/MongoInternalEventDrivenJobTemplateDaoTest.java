package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoInternalEventDrivenJobTemplateDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoInternalEventDrivenJobRepository repository;
    private MongoInternalEventDrivenJobTemplateDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoInternalEventDrivenJobRepository.class);

        dao = new MongoInternalEventDrivenJobTemplateDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoInternalEventDrivenJobRecordImpl.class);
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
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
        Assert.assertEquals(record.getId(), found.getId());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setId("custom_template_id");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById("custom_template_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_template_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        InternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        record = this.dao.findById(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_agent1_job1_context1");

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<InternalEventDrivenJobRecord> records = new ArrayList<>();
        records.add(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        records.add(createInternalEventDrivenJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_all() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent2", "job2", "context2"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent2", "job2", "context1"));
        dao.save(createInternalEventDrivenJobRecord("agent3", "job3", "context2"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createInternalEventDrivenJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createInternalEventDrivenJobRecord("agent1", "job1", "context1"));

        SearchResults<InternalEventDrivenJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        InternalEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_skip_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        dao.save(record);

        List<String> childContexts = Arrays.asList("childContext1", "childContext2");
        dao.skip(record, childContexts, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_hold_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        dao.save(record);

        List<String> childContexts = Arrays.asList("childContext1", "childContext2");
        dao.hold(record, childContexts, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_enable_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        dao.save(record);

        dao.enable(record, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_release_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        dao.save(record);

        dao.release(record, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_release_all_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.releaseAll(records, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_hold_all_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.holdAll(records, "testUser");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_enable_all_throws_exception() {
        MongoInternalEventDrivenJobRecordImpl record1 = createInternalEventDrivenJobRecord("agent1", "job1", "context1");
        MongoInternalEventDrivenJobRecordImpl record2 = createInternalEventDrivenJobRecord("agent2", "job2", "context2");
        dao.save(record1);
        dao.save(record2);

        List<InternalEventDrivenJobRecord> records = Arrays.asList(record1, record2);
        dao.enableAll(records, "testUser");
    }

    @Test
    public void test_findAll_only_returns_templates() {
        // Create a regular job using the parent DAO
        MongoInternalEventDrivenJobDao regularDao = new MongoInternalEventDrivenJobDao(repository, mongoTemplate);
        MongoInternalEventDrivenJobRecordImpl regularJob = createInternalEventDrivenJobRecord("regular", "job", "context");
        regularDao.save(regularJob);

        // Create template jobs
        dao.save(createInternalEventDrivenJobRecord("template1", "job1", "context1"));
        dao.save(createInternalEventDrivenJobRecord("template2", "job2", "context2"));

        // Template DAO should only find templates
        SearchResults<InternalEventDrivenJobRecord> templateResults = dao.findAll(10, 0);
        Assert.assertEquals(2, templateResults.getTotalNumberOfResults());

        // Regular DAO should find non template only
        SearchResults<InternalEventDrivenJobRecord> allResults = regularDao.findAll(10, 0);
        Assert.assertEquals(1, allResults.getTotalNumberOfResults());
    }

    // Helper methods

    private MongoInternalEventDrivenJobRecordImpl createInternalEventDrivenJobRecord(String agentName, String jobName, String contextName) {
        InternalEventDrivenJobImpl internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName(agentName);
        internalEventDrivenJob.setJobName(jobName);
        internalEventDrivenJob.setContextName(contextName);
        internalEventDrivenJob.setDisplayName("Display " + jobName);
        internalEventDrivenJob.setTargetResidingContextOnly(false);
        internalEventDrivenJob.setParticipatesInLock(false);

        MongoInternalEventDrivenJobRecordImpl record = new MongoInternalEventDrivenJobRecordImpl();
        record.setId(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + internalEventDrivenJob.getAgentName() + "_"
            + internalEventDrivenJob.getJobName() + "_" + internalEventDrivenJob.getContextName());
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setInternalEventDrivenJob(internalEventDrivenJob);
        record.setTimestamp(System.currentTimeMillis());
        record.setTargetResidingContextOnly(false);
        record.setParticipatesInLock(false);

        return record;
    }
}
