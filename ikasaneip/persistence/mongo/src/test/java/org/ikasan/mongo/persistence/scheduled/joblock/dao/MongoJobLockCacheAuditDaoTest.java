package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheAuditRepository;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class MongoJobLockCacheAuditDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoJobLockCacheAuditRepository repository;
    private MongoJobLockCacheAuditDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoJobLockCacheAuditRepository.class);

        dao = new MongoJobLockCacheAuditDao(repository, mongoTemplate, 7);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoJobLockCacheAuditRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_find_all() {
        MongoJobLockCacheAuditRecordImpl record1 = createJobLockCacheAuditRecord("env1");
        MongoJobLockCacheAuditRecordImpl record2 = createJobLockCacheAuditRecord("env2");

        dao.save(record1);
        dao.save(record2);

        SearchResults<JobLockCacheAuditRecord> results = dao.findAll(10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_save_generates_unique_id() {
        JobLockCacheAuditRecord record1 = createJobLockCacheAuditRecord("env1");
        JobLockCacheAuditRecord record2 = createJobLockCacheAuditRecord("env1");

        dao.save(record1);
        dao.save(record2);

        record1 = dao.findAll(100, 0).getResultList().get(0);
        record2 = dao.findAll(100, 0).getResultList().get(1);

        Assert.assertNotNull(record1.getId());
        Assert.assertNotNull(record2.getId());
        Assert.assertNotEquals(record1.getId(), record2.getId());
        Assert.assertTrue(record1.getId().startsWith("jockLockCacheRecordAuditID_"));
        Assert.assertTrue(record2.getId().startsWith("jockLockCacheRecordAuditID_"));
    }

    @Test
    public void test_save_sets_timestamp() {
        JobLockCacheAuditRecord record = createJobLockCacheAuditRecord("test-env");

        long beforeSave = System.currentTimeMillis();
        dao.save(record);

        record = dao.findAll(100, 0).getResultList().get(0);
        Assert.assertTrue(record.getTimestamp() >= beforeSave);
        Assert.assertTrue(record.getTimestamp() <= System.currentTimeMillis());
    }

    @Test
    public void test_find_all_with_pagination() {
        for (int i = 0; i < 15; i++) {
            dao.save(createJobLockCacheAuditRecord("env" + i));
        }

        SearchResults<JobLockCacheAuditRecord> page1 = dao.findAll(10, 0);
        Assert.assertEquals(15, page1.getTotalNumberOfResults());
        Assert.assertEquals(10, page1.getResultList().size());

        SearchResults<JobLockCacheAuditRecord> page2 = dao.findAll(10, 10);
        Assert.assertEquals(15, page2.getTotalNumberOfResults());
        Assert.assertEquals(5, page2.getResultList().size());
    }

    @Test
    public void test_find_all_empty_collection() {
        SearchResults<JobLockCacheAuditRecord> results = dao.findAll(10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_save_with_explicit_id() {
        MongoJobLockCacheAuditRecordImpl record = createJobLockCacheAuditRecord("test-env");
        record.setId("custom_audit_id_12345");

        dao.save(record);

        Assert.assertEquals("custom_audit_id_12345", record.getId());
    }

    @Test
    public void test_save_preserves_job_lock_cache_data() {
        MongoJobLockCacheAuditRecordImpl record = createJobLockCacheAuditRecord("test-env");

        dao.save(record);

        SearchResults<JobLockCacheAuditRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(1, results.getResultList().size());

        JobLockCacheAuditRecord found = results.getResultList().get(0);
        Assert.assertNotNull(found.getJobLockCache());
        Assert.assertNotNull(found.getJobLockCache().getJobLocksByIdentifier());
        Assert.assertNotNull(found.getJobLockCache().getJobLocksByLockName());
    }

    @Test
    public void test_save_multiple_records_maintains_order() throws InterruptedException {
        JobLockCacheAuditRecord record1 = createJobLockCacheAuditRecord("env1");
        Thread.sleep(10);
        JobLockCacheAuditRecord record2 = createJobLockCacheAuditRecord("env2");
        Thread.sleep(10);
        JobLockCacheAuditRecord record3 = createJobLockCacheAuditRecord("env3");

        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        SearchResults<JobLockCacheAuditRecord> results = dao.findAll(10, 0);
        Assert.assertEquals(3, results.getTotalNumberOfResults());

        record1 = dao.findAll(100, 0).getResultList().get(0);
        record2 = dao.findAll(100, 0).getResultList().get(1);
        record3 = dao.findAll(100, 0).getResultList().get(2);

        // Verify timestamps are set
        Assert.assertTrue(record1.getTimestamp() > 0);
        Assert.assertTrue(record2.getTimestamp() > record1.getTimestamp());
        Assert.assertTrue(record3.getTimestamp() > record2.getTimestamp());
    }

    // Helper methods

    private MongoJobLockCacheAuditRecordImpl createJobLockCacheAuditRecord(String environment) {
        MongoJobLockCacheAuditRecordImpl record = new MongoJobLockCacheAuditRecordImpl();
        record.setEnvironment(environment);

        JobLockCacheDataImpl data = new JobLockCacheDataImpl();
        record.setJobLockCache(data);

        return record;
    }
}
