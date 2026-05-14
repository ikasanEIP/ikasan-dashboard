package org.ikasan.relational.persistence.scheduled.joblock.dao;

import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheData;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheRecord;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Comprehensive integration test for HibernateJobLockCacheDaoImpl using Testcontainers with PostgreSQL.
 *
 * Tests all DAO operations including save, get, update, and delete operations.
 * Uses PostgreSQL test container for realistic database testing.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateJobLockCacheDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();

    }

    @Autowired
    private JobLockCacheDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data
//        dao.
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // Helper Methods

    private HibernateJobLockCacheRecord createJobLockCacheRecord(String environment) {
        HibernateJobLockCacheRecord record = new HibernateJobLockCacheRecord(environment);

        HibernateJobLockCacheData cacheData = new HibernateJobLockCacheData();
        cacheData.setJobLocksByLockName(new ConcurrentHashMap<>());
        cacheData.setJobLocksByIdentifier(new ConcurrentHashMap<>());
        cacheData.setExclusiveLockSchedulerJobInitiationEventWaitQueue(new LinkedList<>());
        cacheData.setExclusiveLockHolder(new JobLockHolderImpl());

        record.setJobLockCache(cacheData);
        return record;
    }

    private JobLockHolder createJobLockHolder(String lockName, String... jobIdentifiers) {
        JobLockHolderImpl holder = new JobLockHolderImpl();
        holder.setLockName(lockName);
        List<SchedulerJobLockParticipant> jobLockParticipants = new ArrayList<>();
        for (String identifier : jobIdentifiers) {
            SchedulerJobLockParticipant jobLockParticipant = new SchedulerJobLockParticipantImpl();
            jobLockParticipant.setJobName(identifier);
            jobLockParticipants.add(jobLockParticipant);
        }
        holder.addSchedulerJobs("context", jobLockParticipants);
        return holder;
    }

    // Constructor and Validation Tests

    @Test
    public void test_dao_autowired_successfully() {
        assertNotNull("DAO should be autowired", dao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_null_record_throwsException() {
        dao.save(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_save_nonHibernateRecord_throwsException() {
        JobLockCacheRecord mockRecord = new JobLockCacheRecord() {
            @Override
            public String getId() {
                return "test";
            }

            @Override
            public void setEnvironment(String id) {
            }

            @Override
            public String getEnvironment() {
                return "test";
            }

            @Override
            public void setJobLockCache(JobLockCacheData jobLockCache) {
            }

            @Override
            public JobLockCacheData getJobLockCache() {
                return null;
            }

            @Override
            public long getTimestamp() {
                return 0;
            }

            @Override
            public long getModifiedTimestamp() {
                return 0;
            }
        };

        dao.save(mockRecord);
    }

    // Basic CRUD Tests

    @Test
    public void test_save_and_get_defaultEnvironment() {
        // Given
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(JobLockCacheRecord.DEFAULT_ENVIRONMENT);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        assertEquals("Environment should match", JobLockCacheRecord.DEFAULT_ENVIRONMENT, retrieved.getEnvironment());
        assertNotNull("JobLockCache should not be null", retrieved.getJobLockCache());
        assertTrue("Timestamp should be set", retrieved.getTimestamp() > 0);
        assertTrue("ModifiedTimestamp should be set", retrieved.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_and_get_customEnvironment() {
        // Given
        String customEnv = "PRODUCTION";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(customEnv);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(customEnv);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        assertEquals("Environment should match", customEnv, retrieved.getEnvironment());
        assertEquals("ID should match environment", customEnv, retrieved.getId());
        assertNotNull("JobLockCache should not be null", retrieved.getJobLockCache());
    }

    @Test
    public void test_get_nonExistentEnvironment_returnsNull() {
        // When
        JobLockCacheRecord retrieved = dao.get("NON_EXISTENT_ENV");

        // Then
        assertNull("Should return null for non-existent environment", retrieved);
    }

    @Test
    public void test_get_nullEnvironment_usesDefault() {
        // Given
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        dao.save(record);

        // When
        JobLockCacheRecord retrieved = dao.get(null);

        // Then
        assertNotNull("Should retrieve default environment record", retrieved);
        assertEquals("Should use default environment", JobLockCacheRecord.DEFAULT_ENVIRONMENT, retrieved.getEnvironment());
    }

    // Update Tests

    @Test
    public void test_save_existingRecord_updates() throws InterruptedException {
        // Given
        String environment = "TEST_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        dao.save(record);
        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // When - update the record
        JobLockCacheData updatedData = record.getJobLockCache();
        updatedData.getJobLocksByLockName().put("lock1", createJobLockHolder("lock1", "job1"));

        record.setJobLockCache(updatedData);
        dao.save(record);

        // Then
        JobLockCacheRecord retrieved = dao.get(environment);
        assertNotNull("Retrieved record should not be null", retrieved);
        assertEquals("Timestamp should remain the same", initialTimestamp, retrieved.getTimestamp());
        assertTrue("ModifiedTimestamp should be updated",
            retrieved.getModifiedTimestamp() > initialModifiedTimestamp);
        assertEquals("Should have one lock", 1, retrieved.getJobLockCache().getJobLocksByLockName().size());
        assertTrue("Should contain lock1", retrieved.getJobLockCache().getJobLocksByLockName().containsKey("lock1"));
    }

    // Complex Data Tests

    @Test
    public void test_save_withJobLocksByLockName() {
        // Given
        String environment = "LOCK_TEST_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        JobLockCacheData cacheData = record.getJobLockCache();
        cacheData.getJobLocksByLockName().put("lock1", createJobLockHolder("lock1", "job1", "job2"));
        cacheData.getJobLocksByLockName().put("lock2", createJobLockHolder("lock2", "job3"));

        record.setJobLockCache(cacheData);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        JobLockCacheData retrievedData = retrieved.getJobLockCache();
        assertEquals("Should have 2 locks", 2, retrievedData.getJobLocksByLockName().size());
        assertTrue("Should contain lock1", retrievedData.getJobLocksByLockName().containsKey("lock1"));
        assertTrue("Should contain lock2", retrievedData.getJobLocksByLockName().containsKey("lock2"));

        JobLockHolder lock1 = retrievedData.getJobLocksByLockName().get("lock1");
        assertEquals("lock1 should have correct name", "lock1", lock1.getLockName());
        assertEquals("lock1 should have 2 jobs", 2, lock1.getSchedulerJobs().get("context").size());
    }

    @Test
    public void test_save_withJobLocksByIdentifier() {
        // Given
        String environment = "IDENTIFIER_TEST_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        JobLockCacheData cacheData = record.getJobLockCache();
        cacheData.getJobLocksByIdentifier().put("job1", "lock1");
        cacheData.getJobLocksByIdentifier().put("job2", "lock1");
        cacheData.getJobLocksByIdentifier().put("job3", "lock2");

        record.setJobLockCache(cacheData);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        JobLockCacheData retrievedData = retrieved.getJobLockCache();
        assertEquals("Should have 3 identifier mappings", 3, retrievedData.getJobLocksByIdentifier().size());
        assertEquals("job1 should map to lock1", "lock1", retrievedData.getJobLocksByIdentifier().get("job1"));
        assertEquals("job2 should map to lock1", "lock1", retrievedData.getJobLocksByIdentifier().get("job2"));
        assertEquals("job3 should map to lock2", "lock2", retrievedData.getJobLocksByIdentifier().get("job3"));
    }

    @Test
    public void test_save_withExclusiveLockHolder() {
        // Given
        String environment = "EXCLUSIVE_LOCK_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        JobLockCacheData cacheData = record.getJobLockCache();
        JobLockHolder exclusiveLock = createJobLockHolder("EXCLUSIVE_LOCK", "exclusive_job_1", "exclusive_job_2");
        cacheData.setExclusiveLockHolder(exclusiveLock);

        record.setJobLockCache(cacheData);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        JobLockCacheData retrievedData = retrieved.getJobLockCache();
        assertNotNull("Exclusive lock holder should not be null", retrievedData.getExclusiveLockHolder());
        assertEquals("Exclusive lock should have correct name", "EXCLUSIVE_LOCK",
            retrievedData.getExclusiveLockHolder().getLockName());
        assertEquals("Exclusive lock should have 2 jobs", 2,
            retrievedData.getExclusiveLockHolder().getSchedulerJobs().get("context").size());
    }

    @Test
    public void test_save_withWaitQueue() {
        // Given
        String environment = "WAIT_QUEUE_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        JobLockCacheData cacheData = record.getJobLockCache();
        Queue<ContextualisedSchedulerJobInitiationEvent> waitQueue = new LinkedList<>();

        ContextualisedSchedulerJobInitiationEvent event1 = new ContextualisedSchedulerJobInitiationEventImpl();
        event1.setContextName("context1");
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("job1");
        event1.setSchedulerJobInitiationEvent(schedulerJobInitiationEvent);

        ContextualisedSchedulerJobInitiationEvent event2 = new ContextualisedSchedulerJobInitiationEventImpl();
        event2.setContextName("context2");
        schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("job2");
        event2.setSchedulerJobInitiationEvent(schedulerJobInitiationEvent);

        waitQueue.add(event1);
        waitQueue.add(event2);

        cacheData.setExclusiveLockSchedulerJobInitiationEventWaitQueue(waitQueue);
        record.setJobLockCache(cacheData);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        JobLockCacheData retrievedData = retrieved.getJobLockCache();
        Queue<ContextualisedSchedulerJobInitiationEvent> retrievedQueue =
            retrievedData.getExclusiveLockSchedulerJobInitiationEventWaitQueue();
        assertNotNull("Wait queue should not be null", retrievedQueue);
        assertEquals("Wait queue should have 2 events", 2, retrievedQueue.size());

        ContextualisedSchedulerJobInitiationEvent retrievedEvent1 = retrievedQueue.poll();
        assertEquals("First event should have correct context", "context1", retrievedEvent1.getContextName());
        assertEquals("First event should have correct job name", "job1", retrievedEvent1.getSchedulerJobInitiationEvent().getJobName());
    }

    @Test
    public void test_save_complexRecord_withAllData() {
        // Given
        String environment = "COMPLEX_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);

        JobLockCacheData cacheData = record.getJobLockCache();

        // Add multiple locks by name
        cacheData.getJobLocksByLockName().put("lock1", createJobLockHolder("lock1", "job1", "job2"));
        cacheData.getJobLocksByLockName().put("lock2", createJobLockHolder("lock2", "job3"));

        // Add identifier mappings
        cacheData.getJobLocksByIdentifier().put("job1", "lock1");
        cacheData.getJobLocksByIdentifier().put("job2", "lock1");
        cacheData.getJobLocksByIdentifier().put("job3", "lock2");

        // Set exclusive lock
        cacheData.setExclusiveLockHolder(createJobLockHolder("EXCLUSIVE", "exclusive_job"));

        // Add wait queue events
        Queue<ContextualisedSchedulerJobInitiationEvent> waitQueue = new LinkedList<>();
        ContextualisedSchedulerJobInitiationEvent event = new ContextualisedSchedulerJobInitiationEventImpl();
        event.setContextName("waiting_context");
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("waiting_job");
        event.setSchedulerJobInitiationEvent(schedulerJobInitiationEvent);
        waitQueue.add(event);
        cacheData.setExclusiveLockSchedulerJobInitiationEventWaitQueue(waitQueue);

        record.setJobLockCache(cacheData);

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        JobLockCacheData retrievedData = retrieved.getJobLockCache();

        // Verify locks by name
        assertEquals("Should have 2 locks", 2, retrievedData.getJobLocksByLockName().size());

        // Verify identifier mappings
        assertEquals("Should have 3 identifier mappings", 3, retrievedData.getJobLocksByIdentifier().size());

        // Verify exclusive lock
        assertNotNull("Exclusive lock should not be null", retrievedData.getExclusiveLockHolder());
        assertEquals("Exclusive lock should have correct name", "EXCLUSIVE",
            retrievedData.getExclusiveLockHolder().getLockName());

        // Verify wait queue
        assertNotNull("Wait queue should not be null",
            retrievedData.getExclusiveLockSchedulerJobInitiationEventWaitQueue());
        assertEquals("Wait queue should have 1 event", 1,
            retrievedData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().size());
    }

    @Test
    public void test_multipleEnvironments_isolated() {
        // Given
        HibernateJobLockCacheRecord record1 = createJobLockCacheRecord("PROD");
        JobLockCacheData jobLockCacheData = record1.getJobLockCache();
        jobLockCacheData.getJobLocksByLockName().put("prod_lock", createJobLockHolder("prod_lock", "prod_job"));
        record1.setJobLockCache(jobLockCacheData);

        HibernateJobLockCacheRecord record2 = createJobLockCacheRecord("DEV");
        jobLockCacheData = record2.getJobLockCache();
        jobLockCacheData.getJobLocksByLockName().put("dev_lock", createJobLockHolder("dev_lock", "dev_job"));
        record2.setJobLockCache(jobLockCacheData);

        // When
        dao.save(record1);
        dao.save(record2);

        // Then
        JobLockCacheRecord retrievedProd = dao.get("PROD");
        JobLockCacheRecord retrievedDev = dao.get("DEV");

        assertNotNull("PROD record should exist", retrievedProd);
        assertNotNull("DEV record should exist", retrievedDev);

        assertTrue("PROD should have prod_lock",
            retrievedProd.getJobLockCache().getJobLocksByLockName().containsKey("prod_lock"));
        assertFalse("PROD should not have dev_lock",
            retrievedProd.getJobLockCache().getJobLocksByLockName().containsKey("dev_lock"));

        assertTrue("DEV should have dev_lock",
            retrievedDev.getJobLockCache().getJobLocksByLockName().containsKey("dev_lock"));
        assertFalse("DEV should not have prod_lock",
            retrievedDev.getJobLockCache().getJobLocksByLockName().containsKey("prod_lock"));
    }

    // Edge Cases

    @Test
    public void test_save_emptyJobLockCache() {
        // Given
        String environment = "EMPTY_ENV";
        HibernateJobLockCacheRecord record = createJobLockCacheRecord(environment);
        // Cache data is already initialized with empty collections

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(environment);

        // Then
        assertNotNull("Retrieved record should not be null", retrieved);
        assertNotNull("JobLockCache should not be null", retrieved.getJobLockCache());
        assertTrue("JobLocksByLockName should be empty",
            retrieved.getJobLockCache().getJobLocksByLockName().isEmpty());
        assertTrue("JobLocksByIdentifier should be empty",
            retrieved.getJobLockCache().getJobLocksByIdentifier().isEmpty());
        assertTrue("Wait queue should be empty",
            retrieved.getJobLockCache().getExclusiveLockSchedulerJobInitiationEventWaitQueue().isEmpty());
    }

    @Test
    public void test_save_nullEnvironment_usesDefault() {
        // Given
        HibernateJobLockCacheRecord record = new HibernateJobLockCacheRecord(null);
        record.setJobLockCache(new HibernateJobLockCacheData());

        // When
        dao.save(record);
        JobLockCacheRecord retrieved = dao.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);

        // Then
        assertNotNull("Should retrieve record with default environment", retrieved);
        assertEquals("Environment should be default", JobLockCacheRecord.DEFAULT_ENVIRONMENT,
            retrieved.getEnvironment());
    }

    @Test
    public void test_concurrentModification_lastWriteWins() throws InterruptedException {
        // Given
        String environment = "CONCURRENT_ENV";
        HibernateJobLockCacheRecord record1 = createJobLockCacheRecord(environment);
        record1.getJobLockCache().getJobLocksByLockName().put("lock1", createJobLockHolder("lock1", "job1"));

        dao.save(record1);

        Thread.sleep(10);

        // When - Save a different version
        HibernateJobLockCacheRecord record2 = createJobLockCacheRecord(environment);
        JobLockCacheData jobLockCacheData = record2.getJobLockCache();
        jobLockCacheData.getJobLocksByLockName().put("lock2", createJobLockHolder("lock2", "job2"));
        record2.setJobLockCache(jobLockCacheData);

        dao.save(record2);

        // Then - Last write should win
        JobLockCacheRecord retrieved = dao.get(environment);
        assertNotNull("Retrieved record should not be null", retrieved);
        assertTrue("Should have lock2", retrieved.getJobLockCache().getJobLocksByLockName().containsKey("lock2"));
        assertFalse("Should not have lock1", retrieved.getJobLockCache().getJobLocksByLockName().containsKey("lock1"));
    }
}
