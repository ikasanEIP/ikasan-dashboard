package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoQuartzScheduleDrivenJobRepository;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
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

public class MongoQuartzScheduleDrivenJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoQuartzScheduleDrivenJobRepository repository;
    private MongoQuartzScheduleDrivenJobDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        // Use Spring Data MongoDB repository factory
        var mongoMappingContext = mongoTemplate.getConverter().getMappingContext();
        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoQuartzScheduleDrivenJobRepository.class);

        dao = new MongoQuartzScheduleDrivenJobDao(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoQuartzScheduleDrivenJobRecordImpl.class);
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
        QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setContextName("contextId");
        quartzScheduleDrivenJob.setAgentName("agentName");
        quartzScheduleDrivenJob.setJobName("jobName");
        quartzScheduleDrivenJob.setCronExpression("0 0 * * * ?");

        MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
        record.setAgentName("agentName");
        record.setJobName("jobName");
        record.setContextName("contextId");
        record.setTimestamp(1000000L);
        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        dao.save(record);

        QuartzScheduleDrivenJobRecord found = dao.findById("quartzScheduleDrivenJob_agentName_jobName_contextId");

        Assert.assertNotNull(found);
        Assert.assertEquals("quartzScheduleDrivenJob_agentName_jobName_contextId", found.getId());
        Assert.assertEquals("agentName", found.getAgentName());
        Assert.assertEquals("jobName", found.getJobName());
        Assert.assertEquals("contextId", found.getContextName());
        Assert.assertEquals(1000000L, found.getTimestamp());
        Assert.assertNotNull(found.getQuartzScheduleDrivenJob());
        Assert.assertEquals("contextId", found.getQuartzScheduleDrivenJob().getContextName());

        Assert.assertNull(dao.findById("bad_id"));
    }

    @Test
    public void test_save_with_explicit_id() {
        QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setContextName("testContext");
        quartzScheduleDrivenJob.setAgentName("testAgent");
        quartzScheduleDrivenJob.setJobName("testJob");
        quartzScheduleDrivenJob.setCronExpression("0 0 12 * * ?");

        MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
        record.setId("custom-id-123");
        record.setAgentName("testAgent");
        record.setJobName("testJob");
        record.setContextName("testContext");
        record.setTimestamp(2000000L);
        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        dao.save(record);

        QuartzScheduleDrivenJobRecord found = dao.findById("custom-id-123");

        Assert.assertNotNull(found);
        Assert.assertEquals("custom-id-123", found.getId());
        Assert.assertEquals("testAgent", found.getAgentName());
        Assert.assertEquals("testJob", found.getJobName());
        Assert.assertEquals(2000000L, found.getTimestamp());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setContextName("context");
        quartzScheduleDrivenJob.setAgentName("agent");
        quartzScheduleDrivenJob.setJobName("job");

        MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName("job");
        record.setContextName("context");
        record.setTimestamp(1000000L);
        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        QuartzScheduleDrivenJobRecord found = dao.findById("quartzScheduleDrivenJob_agent_job_context");
        Assert.assertNotNull(found);
        Assert.assertTrue(found.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<QuartzScheduleDrivenJobRecord> records = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextName("context" + i);
            quartzScheduleDrivenJob.setAgentName("agent" + i);
            quartzScheduleDrivenJob.setJobName("job" + i);
            quartzScheduleDrivenJob.setCronExpression("0 0 * * * ?");

            MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
            record.setAgentName("agent" + i);
            record.setJobName("job" + i);
            record.setContextName("context" + i);
            record.setTimestamp(1000000L + i);
            record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

            records.add(record);
        }

        dao.save(records);

        QuartzScheduleDrivenJobRecord found = dao.findById("quartzScheduleDrivenJob_agent2_job2_context2");
        Assert.assertNotNull(found);
        Assert.assertEquals("agent2", found.getAgentName());
        Assert.assertEquals("job2", found.getJobName());
    }

    @Test
    public void test_find_all() {
        insertQuartzScheduleDrivenJobs("id", 50, "context1");

        SearchResults<QuartzScheduleDrivenJobRecord> results = dao.findAll(10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(50, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_all_with_pagination() {
        insertQuartzScheduleDrivenJobs("id", 100, "context1");

        SearchResults<QuartzScheduleDrivenJobRecord> page1 = dao.findAll(20, 0);
        SearchResults<QuartzScheduleDrivenJobRecord> page2 = dao.findAll(20, 20);
        SearchResults<QuartzScheduleDrivenJobRecord> page3 = dao.findAll(20, 40);

        Assert.assertEquals(20, page1.getResultList().size());
        Assert.assertEquals(100, page1.getTotalNumberOfResults());
        Assert.assertEquals(20, page2.getResultList().size());
        Assert.assertEquals(100, page2.getTotalNumberOfResults());
        Assert.assertEquals(20, page3.getResultList().size());
        Assert.assertEquals(100, page3.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context() {
        insertQuartzScheduleDrivenJobs("id1", 30, "context1");
        insertQuartzScheduleDrivenJobs("id2", 50, "context2");
        insertQuartzScheduleDrivenJobs("id3", 20, "context3");

        SearchResults<QuartzScheduleDrivenJobRecord> results = dao.findByContext("context2", 100, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(50, results.getResultList().size());
        Assert.assertEquals(50, results.getTotalNumberOfResults());

        results.getResultList().forEach(record ->
            Assert.assertEquals("context2", record.getContextName())
        );
    }

    @Test
    public void test_find_by_context_with_pagination() {
        insertQuartzScheduleDrivenJobs("id", 100, "context1");

        SearchResults<QuartzScheduleDrivenJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(100, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context_no_results() {
        insertQuartzScheduleDrivenJobs("id", 10, "context1");

        SearchResults<QuartzScheduleDrivenJobRecord> results = dao.findByContext("nonexistent", 100, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getResultList().size());
        Assert.assertEquals(0, results.getTotalNumberOfResults());
    }

    @Test
    public void test_modified_by_field() {
        QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setContextName("context");
        quartzScheduleDrivenJob.setAgentName("agent");
        quartzScheduleDrivenJob.setJobName("job");

        MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName("job");
        record.setContextName("context");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");
        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        dao.save(record);

        QuartzScheduleDrivenJobRecord found = dao.findById("quartzScheduleDrivenJob_agent_job_context");
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_wrong_type_throws_exception() {
        QuartzScheduleDrivenJobRecord invalidRecord = new QuartzScheduleDrivenJobRecord() {
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
            public QuartzScheduleDrivenJobImpl getQuartzScheduleDrivenJob() { return null; }
            @Override
            public void setQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {}
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

    private void insertQuartzScheduleDrivenJobs(String idPrefix, int num, String contextId) {
        for (int i = 0; i < num; i++) {
            String agent = idPrefix + "agent" + i;
            String jobName = "jobName" + i;

            QuartzScheduleDrivenJobImpl quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextName(contextId);
            quartzScheduleDrivenJob.setAgentName(agent);
            quartzScheduleDrivenJob.setJobName(jobName);
            quartzScheduleDrivenJob.setCronExpression("0 0 * * * ?");
            quartzScheduleDrivenJob.setDisplayName("displayName" + i);

            MongoQuartzScheduleDrivenJobRecordImpl record = new MongoQuartzScheduleDrivenJobRecordImpl();
            record.setAgentName(agent);
            record.setJobName(jobName);
            record.setContextName(contextId);
            record.setDisplayName("displayName" + i);
            record.setTimestamp(System.currentTimeMillis());
            record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

            dao.save(record);
        }
    }
}
