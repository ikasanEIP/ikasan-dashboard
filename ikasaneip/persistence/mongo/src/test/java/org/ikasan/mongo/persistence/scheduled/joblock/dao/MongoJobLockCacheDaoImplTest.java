package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheRepository;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ConcurrentHashMap;

public class MongoJobLockCacheDaoImplTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoJobLockCacheRepository repository;
    private MongoJobLockCacheDaoImpl dao;

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

        dao = new MongoJobLockCacheDaoImpl(repository, mongoTemplate);
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

        MongoJobLockCacheRecordImpl found = (MongoJobLockCacheRecordImpl) dao.get("test-env");
        Assert.assertEquals("jobLockCacheIdentifier__test-env", found.getId());
        Assert.assertEquals(JobLockCacheDao.JOB_LOCK_CACHE_TYPE, found.getType());
        Assert.assertEquals(-1, found.getExpiry());
    }

    @Test
    public void test_save_sets_timestamp_if_not_set() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        Assert.assertEquals(0, record.getTimestamp());

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        JobLockCacheRecord found = dao.get("test-env");
        Assert.assertTrue(found.getTimestamp() >= beforeSave);
        Assert.assertTrue(found.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_preserves_existing_timestamp() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        long originalTimestamp = System.currentTimeMillis() - 10000; // 10 seconds ago
        record.setTimestamp(originalTimestamp);

        dao.save(record);

        JobLockCacheRecord found = dao.get("test-env");
        Assert.assertEquals(originalTimestamp, found.getTimestamp());
        Assert.assertTrue(found.getModifiedTimestamp() > originalTimestamp);
    }

    @Test
    public void test_save_updates_modified_timestamp() throws InterruptedException {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");
        dao.save(record);

        JobLockCacheRecord first = dao.get("test-env");

        Thread.sleep(10);

        // Save the record read back, as callers do
        dao.save(first);

        JobLockCacheRecord second = dao.get("test-env");
        Assert.assertTrue(second.getModifiedTimestamp() > first.getModifiedTimestamp());
        Assert.assertEquals(first.getTimestamp(), second.getTimestamp());
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

    @Test
    public void test_save_non_mongo_record_implementation() {
        // JobLockCacheImpl saves a JobLockCacheRecordImpl, which is not a MongoJobLockCacheRecordImpl
        JobLockCacheRecordImpl record = new JobLockCacheRecordImpl();
        record.setEnvironment("generic-env");
        record.setJobLockCache(dataWithLock("lockA", "job1"));

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        MongoJobLockCacheRecordImpl found = (MongoJobLockCacheRecordImpl) dao.get("generic-env");
        Assert.assertNotNull(found);
        Assert.assertEquals("jobLockCacheIdentifier__generic-env", found.getId());
        Assert.assertEquals("generic-env", found.getEnvironment());
        Assert.assertEquals(JobLockCacheDao.JOB_LOCK_CACHE_TYPE, found.getType());
        Assert.assertEquals(-1, found.getExpiry());
        Assert.assertEquals("job1", found.getJobLockCache().getJobLocksByIdentifier().get("lockA"));
        Assert.assertTrue(found.getTimestamp() >= beforeSave);
        Assert.assertTrue(found.getModifiedTimestamp() >= beforeSave);
    }

    @Test
    public void test_save_copies_all_fields_from_any_record_implementation() {
        long createdTimestamp = 12345L;
        StubJobLockCacheRecord record = new StubJobLockCacheRecord("stub-env", dataWithLock("lockB", "job2"),
            createdTimestamp);

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        JobLockCacheRecord found = dao.get("stub-env");
        Assert.assertNotNull(found);
        Assert.assertEquals("stub-env", found.getEnvironment());
        Assert.assertEquals(createdTimestamp, found.getTimestamp());
        Assert.assertTrue(found.getModifiedTimestamp() >= beforeSave);
        Assert.assertEquals("job2", found.getJobLockCache().getJobLocksByIdentifier().get("lockB"));
    }

    @Test
    public void test_save_record_with_null_environment_uses_default_environment() {
        StubJobLockCacheRecord record = new StubJobLockCacheRecord(null, new JobLockCacheDataImpl(), 0L);

        dao.save(record);

        JobLockCacheRecord found = dao.get(null);
        Assert.assertNotNull(found);
        Assert.assertEquals("jobLockCacheIdentifier__" + JobLockCacheRecord.DEFAULT_ENVIRONMENT, found.getId());
        Assert.assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, found.getEnvironment());
    }

    @Test
    public void test_save_does_not_modify_the_record_passed_in() {
        MongoJobLockCacheRecordImpl record = createJobLockCacheRecord("test-env");

        dao.save(record);

        Assert.assertNull(record.getId());
        Assert.assertNull(record.getType());
        Assert.assertEquals(0, record.getTimestamp());
        Assert.assertEquals(0, record.getModifiedTimestamp());
        Assert.assertEquals(0, record.getExpiry());
    }

    @Test
    public void test_save_same_environment_from_different_implementations_overwrites() {
        dao.save(new StubJobLockCacheRecord("shared-env", dataWithLock("lockA", "job1"), 0L));

        JobLockCacheRecordImpl record = new JobLockCacheRecordImpl();
        record.setEnvironment("shared-env");
        record.setJobLockCache(dataWithLock("lockA", "job2"));
        dao.save(record);

        Assert.assertEquals(1, mongoTemplate.findAll(MongoJobLockCacheRecordImpl.class).size());
        Assert.assertEquals("job2",
            dao.get("shared-env").getJobLockCache().getJobLocksByIdentifier().get("lockA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_null_record_throws() {
        dao.save(null);
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

    private static JobLockCacheDataImpl dataWithLock(String lockName, String jobIdentifier) {
        JobLockCacheDataImpl data = new JobLockCacheDataImpl();
        ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();
        locks.put(lockName, jobIdentifier);
        data.setJobLocksByIdentifier(locks);
        return data;
    }

    /**
     * A JobLockCacheRecord that is neither the Mongo nor the generic implementation.
     */
    private static class StubJobLockCacheRecord implements JobLockCacheRecord {
        private String environment;
        private JobLockCacheData jobLockCache;
        private final long timestamp;

        StubJobLockCacheRecord(String environment, JobLockCacheData jobLockCache, long timestamp) {
            this.environment = environment;
            this.jobLockCache = jobLockCache;
            this.timestamp = timestamp;
        }

        @Override
        public String getId() {
            return "ignored-id";
        }

        @Override
        public String getEnvironment() {
            return environment;
        }

        @Override
        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        @Override
        public void setJobLockCache(JobLockCacheData jobLockCache) {
            this.jobLockCache = jobLockCache;
        }

        @Override
        public JobLockCacheData getJobLockCache() {
            return jobLockCache;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public long getModifiedTimestamp() {
            return 99L;
        }
    }
}
