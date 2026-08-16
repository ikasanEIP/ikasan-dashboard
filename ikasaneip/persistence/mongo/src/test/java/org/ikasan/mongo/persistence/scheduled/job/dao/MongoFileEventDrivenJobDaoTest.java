package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoFileEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoFileEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
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

public class MongoFileEventDrivenJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoFileEventDrivenJobRepository repository;
    private MongoFileEventDrivenJobDao dao;

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
        repository = factory.getRepository(MongoFileEventDrivenJobRepository.class);

        dao = new MongoFileEventDrivenJobDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoFileEventDrivenJobRecordImpl.class);
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
        MongoFileEventDrivenJobRecordImpl record = createFileEventDrivenJobRecord("agent1", "job1", "context1");

        dao.save(record);

        FileEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(record.getId(), found.getId());
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertEquals("context1", found.getContextName());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoFileEventDrivenJobRecordImpl record = createFileEventDrivenJobRecord("agent1", "job1", "context1");
        record.setId("custom_id");

        dao.save(record);

        FileEventDrivenJobRecord found = dao.findById("custom_id");
        Assert.assertNotNull(found);
        Assert.assertEquals("custom_id", found.getId());
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        MongoFileEventDrivenJobRecordImpl record = createFileEventDrivenJobRecord("agent1", "job1", "context1");

        long beforeSave = System.currentTimeMillis();
        Thread.sleep(10);
        dao.save(record);

        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_list() {
        List<FileEventDrivenJobRecord> records = new ArrayList<>();
        records.add(createFileEventDrivenJobRecord("agent1", "job1", "context1"));
        records.add(createFileEventDrivenJobRecord("agent2", "job2", "context2"));

        dao.save(records);

        SearchResults<FileEventDrivenJobRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all() {
        dao.save(createFileEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createFileEventDrivenJobRecord("agent2", "job2", "context2"));

        SearchResults<FileEventDrivenJobRecord> results = dao.findAll(10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createFileEventDrivenJobRecord("agent" + i, "job" + i, "context" + i));
        }

        SearchResults<FileEventDrivenJobRecord> results = dao.findAll(2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        results = dao.findAll(2, 2);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context() {
        dao.save(createFileEventDrivenJobRecord("agent1", "job1", "context1"));
        dao.save(createFileEventDrivenJobRecord("agent2", "job2", "context1"));
        dao.save(createFileEventDrivenJobRecord("agent3", "job3", "context2"));

        SearchResults<FileEventDrivenJobRecord> results = dao.findByContext("context1", 10, 0);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_with_pagination() {
        for (int i = 0; i < 5; i++) {
            dao.save(createFileEventDrivenJobRecord("agent" + i, "job" + i, "context1"));
        }

        SearchResults<FileEventDrivenJobRecord> results = dao.findByContext("context1", 2, 0);
        Assert.assertEquals(5, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_find_by_context_no_results() {
        dao.save(createFileEventDrivenJobRecord("agent1", "job1", "context1"));

        SearchResults<FileEventDrivenJobRecord> results = dao.findByContext("nonexistent", 10, 0);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_modified_by_field() {
        MongoFileEventDrivenJobRecordImpl record = createFileEventDrivenJobRecord("agent1", "job1", "context1");
        record.setModifiedBy("testUser");

        dao.save(record);

        FileEventDrivenJobRecord found = dao.findById(record.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("testUser", found.getModifiedBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_wrong_type_throws_exception() {
        FileEventDrivenJobRecord invalidRecord = new FileEventDrivenJobRecord() {
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
            public FileEventDrivenJob getFileEventDrivenJob() { return null; }
            @Override
            public void setFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {}
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

    private MongoFileEventDrivenJobRecordImpl createFileEventDrivenJobRecord(String agentName, String jobName, String contextName) {
        FileEventDrivenJobImpl fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setAgentName(agentName);
        fileEventDrivenJob.setJobName(jobName);
        fileEventDrivenJob.setContextName(contextName);
        fileEventDrivenJob.setDisplayName("Display " + jobName);

        MongoFileEventDrivenJobRecordImpl record = new MongoFileEventDrivenJobRecordImpl();
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName("Display " + jobName);
        record.setFileEventDrivenJob(fileEventDrivenJob);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }
}
