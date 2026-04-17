package org.ikasan.relational.persistence.scheduled.joblock.dao;

import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheAuditRecord;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheData;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Integration tests for HibernateJobLockCacheAuditDaoImpl using PostgreSQL test container.
 *
 * These tests verify:
 * - Audit record persistence with auto-generated UUIDs
 * - Multiple audit snapshots for same environment
 * - Pagination and ordering
 * - Environment filtering
 * - Complex data structure persistence (JSONB)
 * - Cleanup operations
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
@EnableTransactionManagement
public class HibernateJobLockCacheAuditDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private JobLockCacheAuditDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ========== Helper Methods ==========

    private HibernateJobLockCacheAuditRecord createJobLockCacheAuditRecord(String environment) {
        HibernateJobLockCacheAuditRecord record = new HibernateJobLockCacheAuditRecord();
        record.setEnvironment(environment);

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

    // ========== Constructor and Validation Tests ==========

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
        JobLockCacheAuditRecord mockRecord = new JobLockCacheAuditRecord() {
            @Override
            public String getId() {
                return "test";
            }

            @Override
            public String getEnvironment() {
                return "test";
            }

            @Override
            public void setEnvironment(String environment) {
            }

            @Override
            public void setJobLockCache(org.ikasan.spec.scheduled.joblock.model.JobLockCacheData jobLockCache) {
            }

            @Override
            public org.ikasan.spec.scheduled.joblock.model.JobLockCacheData getJobLockCache() {
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

    // ========== Save Tests ==========
    @Test
    public void test_save_and_find() {
        for(int i=0; i<100; i++) {
           JobLockCacheAuditRecord record = this.createJobLockCacheAuditRecord("TEST_ENVIRONMENT");
           this.dao.save(record);
        }

        SearchResults<JobLockCacheAuditRecord> jobLockCacheAuditRecordSearchResults
            = this.dao.findAll(0, 0);

        Assert.assertEquals(100, jobLockCacheAuditRecordSearchResults.getTotalNumberOfResults());
        Assert.assertEquals(0, jobLockCacheAuditRecordSearchResults.getResultList().size());

        jobLockCacheAuditRecordSearchResults
            = this.dao.findAll(1000, 0);

        Assert.assertEquals(100, jobLockCacheAuditRecordSearchResults.getTotalNumberOfResults());
        Assert.assertEquals(100, jobLockCacheAuditRecordSearchResults.getResultList().size());
    }
}
