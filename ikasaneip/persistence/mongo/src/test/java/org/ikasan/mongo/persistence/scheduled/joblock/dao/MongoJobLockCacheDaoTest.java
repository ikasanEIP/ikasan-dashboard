package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheRepository;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class MongoJobLockCacheDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoJobLockCacheRepository repository;
    private MongoJobLockCacheDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoJobLockCacheRepository.class);

        dao = new MongoJobLockCacheDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoJobLockCacheRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_get_with_default_environment() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord(null);

        dao.save(record);

        JobLockCacheRecord found = dao.get(null);
        Assert.assertNotNull(found);
        Assert.assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, found.getEnvironment());
        Assert.assertNotNull(found.getJobLockCache());
        Assert.assertTrue(found.getTimestamp() > 0);
        Assert.assertTrue(found.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_and_get_with_custom_environment() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("production");

        dao.save(record);

        JobLockCacheRecord found = dao.get("production");
        Assert.assertNotNull(found);
        Assert.assertEquals("production", found.getEnvironment());
        Assert.assertNotNull(found.getJobLockCache());
    }

    @Test
    public void test_get_nonexistent_returns_null() {
        JobLockCacheRecord found = dao.get("nonexistent");
        Assert.assertNull(found);
    }

    @Test
    public void test_save_generates_id() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");

        dao.save(record);

        Assert.assertNotNull(record.getId());
        Assert.assertEquals("jobLockCacheIdentifier__test-env", record.getId());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        record.setId("custom_id");

        dao.save(record);

        Assert.assertEquals("custom_id", record.getId());
    }

    @Test
    public void test_save_sets_timestamp_if_not_set() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        Assert.assertEquals(0, record.getTimestamp());

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        Assert.assertTrue(record.getTimestamp() >= beforeSave);
        Assert.assertTrue(record.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_preserves_existing_timestamp() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        long originalTimestamp = System.currentTimeMillis() - 10000; // 10 seconds ago
        record.setTimestamp(originalTimestamp);

        dao.save(record);

        Assert.assertEquals(originalTimestamp, record.getTimestamp());
        Assert.assertTrue(record.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        dao.save(record);

        long firstModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10);

        // Save again
        dao.save(record);

        Assert.assertTrue(record.getModifiedTimestamp() > firstModifiedTimestamp);
    }

    @Test
    public void test_save_overwrites_existing_record() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        dao.save(record);

        JobLockCacheRecord found = dao.get("test-env");
        Assert.assertNotNull(found);

        // Save again with updated data
        JobLockCacheDataImpl newData = new JobLockCacheDataImpl();
        record.setJobLockCache(newData);
        dao.save(record);

        JobLockCacheRecord updated = dao.get("test-env");
        Assert.assertNotNull(updated);
        Assert.assertNotNull(updated.getJobLockCache());
    }

    @Test
    public void test_environment_extracted_from_id() {
        MongoJobLockCacheRecordImpl record = new MongoJobLockCacheRecordImpl();
        record.setId("jobLockCacheIdentifier__staging");

        Assert.assertEquals("staging", record.getEnvironment());
    }

    @Test
    public void test_default_environment_when_id_has_no_separator() {
        MongoJobLockCacheRecordImpl record = new MongoJobLockCacheRecordImpl();
        record.setId("someIdWithoutSeparator");

        Assert.assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, record.getEnvironment());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_wrong_type_throws_exception() {
        JobLockCacheRecord invalidRecord = new JobLockCacheRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public String getEnvironment() { return "test"; }
            @Override
            public void setEnvironment(String environment) {}
            @Override
            public void setJobLockCache(org.ikasan.spec.scheduled.joblock.model.JobLockCacheData jobLockCache) {}
            @Override
            public org.ikasan.spec.scheduled.joblock.model.JobLockCacheData getJobLockCache() { return null; }
            @Override
            public long getTimestamp() { return 0; }
            @Override
            public long getModifiedTimestamp() { return 0; }
        };

        dao.save(invalidRecord);
    }

    // Helper methods

    private MongoJobLockCacheRecordImpl createJobLockCacheRecord(String environment) {
        MongoJobLockCacheRecordImpl record = new MongoJobLockCacheRecordImpl();
        if (environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }
        record.setEnvironment(environment);

        JobLockCacheDataImpl data = new JobLockCacheDataImpl();
        record.setJobLockCache(data);

        return record;
    }
}
