package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.*;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.spec.scheduled.job.dao.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateSchedulerJobDaoImpl using Testcontainers with PostgreSQL.
 * Tests polymorphic DAO operations across all job types.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateSchedulerJobDaoImplTest {

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
    private SchedulerJobDao<HibernateSchedulerJobRecord> dao;
    @Autowired
    private FileEventDrivenJobDao<HibernateFileEventDrivenJobRecord> fileEventDrivenJobDao;
    @Autowired
    private GlobalEventJobDao<HibernateGlobalEventJobRecord> globalEventJobRecordGlobalEventJobDao;
    @Autowired
    private InternalEventDrivenJobDao<HibernateInternalEventDrivenJobRecord> internalEventDrivenJobDaoInternalEventDrivenJobDao;
    @Autowired
    private QuartzScheduleDrivenJobDao<HibernateQuartzScheduleDrivenJobRecord> quartzScheduleDrivenJobRecordQuartzScheduleDrivenJobDao;
    @Autowired
    private ContextStartJobDao<HibernateContextStartJobRecord> contextStartJobRecordContextStartJobDao;
    @Autowired
    private ContextTerminalJobDao<HibernateContextTerminalJobRecord> contextTerminalJobRecordContextTerminalJobDao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data using EntityManager
        SearchResults<HibernateSchedulerJobRecord> all = dao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
            this.dao.delete(record);
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // Helper methods to create different types of job records

    private HibernateFileEventDrivenJobRecord createFileEventDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        FileEventDrivenJobImpl job = new FileEventDrivenJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);
        job.setFilePath("/test/path");
        job.setFilenames(Arrays.asList("*.txt"));

        record.setFileEventDrivenJob(job);

        fileEventDrivenJobDao.save(record);

        return record;
    }

    private HibernateQuartzScheduleDrivenJobRecord createQuartzScheduleDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateQuartzScheduleDrivenJobRecord record = new HibernateQuartzScheduleDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        QuartzScheduleDrivenJobImpl job = new QuartzScheduleDrivenJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);
        job.setCronExpression("0 0 * * * ?");

        record.setQuartzScheduleDrivenJob(job);

        quartzScheduleDrivenJobRecordQuartzScheduleDrivenJobDao.save(record);

        return record;
    }

    private HibernateInternalEventDrivenJobRecord createInternalEventDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        InternalEventDrivenJobImpl job = new InternalEventDrivenJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);
        job.setCommandLine("test command");

        record.setInternalEventDrivenJob(job);

        internalEventDrivenJobDaoInternalEventDrivenJobDao.save(record);

        return record;
    }

    private HibernateGlobalEventJobRecord createGlobalEventJobRecord(String jobName, String contextName, String agentName) {
        HibernateGlobalEventJobRecord record = new HibernateGlobalEventJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        GlobalEventJobImpl job = new GlobalEventJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);

        record.setGlobalEventJob(job);

        globalEventJobRecordGlobalEventJobDao.save(record);

        return record;
    }

    private HibernateContextStartJobRecord createContextStartJobRecord(String jobName, String contextName, String agentName) {
        HibernateContextStartJobRecord record = new HibernateContextStartJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        ContextStartJobImpl job = new ContextStartJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);

        record.setContextStartJob(job);

        contextStartJobRecordContextStartJobDao.save(record);

        return record;
    }

    private HibernateContextTerminalJobRecord createContextTerminalJobRecord(String jobName, String contextName, String agentName) {
        HibernateContextTerminalJobRecord record = new HibernateContextTerminalJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setModifiedBy("test-user");

        ContextTerminalJobImpl job = new ContextTerminalJobImpl();
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setAgentName(agentName);

        record.setContextTerminalJob(job);

        contextTerminalJobRecordContextTerminalJobDao.save(record);

        return record;
    }

    // Inner class implementation of SchedulerJobSearchFilter for testing
    private static class TestSchedulerJobSearchFilter implements SchedulerJobSearchFilter {
        private String jobNameFilter;
        private String displayNameFilter;
        private List<String> notJobNameInFilter;
        private String jobTypeFilter;
        private String contextSearchFilter;
        private boolean held;
        private boolean skipped;
        private Boolean targetResidingContextOnly;
        private Boolean participatesInLock;
        private List<String> contextNames;
        private List<String> jobTypes;

        @Override
        public String getJobNameFilter() {
            return jobNameFilter;
        }

        @Override
        public void setJobNameFilter(String jobNameFilter) {
            this.jobNameFilter = jobNameFilter;
        }

        @Override
        public String getDisplayNameFilter() {
            return displayNameFilter;
        }

        @Override
        public void setDisplayNameFilter(String displayNameFilter) {
            this.displayNameFilter = displayNameFilter;
        }

        @Override
        public List<String> getNotJobNameInFilter() {
            return notJobNameInFilter;
        }

        @Override
        public void setNotJobNameInFilter(List<String> notJobNameInFilter) {
            this.notJobNameInFilter = notJobNameInFilter;
        }

        @Override
        public String getJobTypeFilter() {
            return jobTypeFilter;
        }

        @Override
        public void setJobTypeFilter(String jobTypeFilter) {
            this.jobTypeFilter = jobTypeFilter;
        }

        @Override
        public String getContextSearchFilter() {
            return contextSearchFilter;
        }

        @Override
        public void setContextSearchFilter(String contextSearchFilter) {
            this.contextSearchFilter = contextSearchFilter;
        }

        @Override
        public List<String> getJobTypes() {
            return jobTypes;
        }

        @Override
        public void setJobTypes(List<String> jobTypes) {
            this.jobTypes = jobTypes;
        }

        @Override
        public boolean isHeld() {
            return held;
        }

        @Override
        public void setHeld(boolean held) {
            this.held = held;
        }

        @Override
        public boolean isSkipped() {
            return skipped;
        }

        @Override
        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
        }

        @Override
        public Boolean isTargetResidingContextOnly() {
            return targetResidingContextOnly;
        }

        @Override
        public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) {
            this.targetResidingContextOnly = targetResidingContextOnly;
        }

        @Override
        public Boolean isParticipatesInLock() {
            return participatesInLock;
        }

        @Override
        public void setParticipatesInLock(Boolean participatesInLock) {
            this.participatesInLock = participatesInLock;
        }

        @Override
        public void setStatus(String status) {
            // Not needed for these tests
        }

        @Override
        public List<String> getContextNames() {
            return contextNames;
        }

        @Override
        public void setContextNames(List<String> contextNames) {
            this.contextNames = contextNames;
        }
    }

    // Test: findById

    @Test
    @Transactional
    public void testFindById_FileEventDrivenJob() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        HibernateSchedulerJobRecord found = dao.findById(record.getId());

        // Then
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals(JobConstants.FILE_EVENT_DRIVEN_JOB, found.getType());
        assertTrue(found instanceof HibernateFileEventDrivenJobRecord);
        assertNotNull(found.getJob());
    }

    @Test
    @Transactional
    public void testFindById_QuartzScheduleDrivenJob() {
        // Given
        HibernateQuartzScheduleDrivenJobRecord record = createQuartzScheduleDrivenJobRecord("quartz-job-1", "quartz-context-1", "agent-1");

        // When
        HibernateSchedulerJobRecord found = dao.findById(record.getId());

        // Then
        assertNotNull(found);
        assertEquals("quartz-job-1", found.getJobName());
        assertEquals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB, found.getType());
        assertTrue(found instanceof HibernateQuartzScheduleDrivenJobRecord);
        assertNotNull(found.getJob());
    }

    @Test
    public void testFindById_NotFound() {
        // When
        HibernateSchedulerJobRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    // Test: getJob() method

    @Test
    @Transactional
    public void testGetJob_FileEventDrivenJob() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("file-job", "file-context", "agent-1");

        // When
        HibernateSchedulerJobRecord found = dao.findById(record.getId());

        // Then
        assertNotNull(found);
        assertNotNull(found.getJob());
        assertEquals("file-job", found.getJob().getJobName());
        assertEquals("file-context", found.getJob().getContextName());
    }

    @Test
    @Transactional
    public void testGetJob_InternalEventDrivenJob() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("internal-job", "internal-context", "agent-1");

        // When
        HibernateSchedulerJobRecord found = dao.findById(record.getId());

        // Then
        assertNotNull(found);
        assertNotNull(found.getJob());
        assertEquals("internal-job", found.getJob().getJobName());
        assertEquals("internal-context", found.getJob().getContextName());
    }

    // Test: findAll

    @Test
    @Transactional
    public void testFindAll_MultipleJobTypes() {
        // Given - create different job types
        createFileEventDrivenJobRecord("file-job-1", "context-1", "agent-1");
        createQuartzScheduleDrivenJobRecord("quartz-job-1", "context-2", "agent-2");
        createInternalEventDrivenJobRecord("internal-job-1", "context-3", "agent-3");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);

        // Verify we got different job types
        Set<String> jobTypes = new HashSet<>();
        results.getResultList().forEach(record -> jobTypes.add(record.getType()));
        assertTrue(jobTypes.size() >= 3);
    }

    @Test
    @Transactional
    public void testFindAll_WithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            createFileEventDrivenJobRecord("find-job-" + i, "context-" + i, "agent-" + i);
        }

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    // Test: findByContext

    @Test
    @Transactional
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        createFileEventDrivenJobRecord("job-1", contextName, "agent-1");
        createQuartzScheduleDrivenJobRecord("job-2", contextName, "agent-2");
        createInternalEventDrivenJobRecord("job-3", "other-context", "agent-3");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByContext(contextName, -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertEquals(contextName, record.getContextName())
        );
    }

    @Test
    @Transactional
    public void testFindByContext_WithLimitAndOffset() {
        // Given
        String contextName = "test-context";
        for (int i = 1; i <= 5; i++) {
            createFileEventDrivenJobRecord("job-" + i, contextName, "agent-" + i);
        }

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    // Test: findByAgent

    @Test
    @Transactional
    public void testFindByAgent() {
        // Given
        createFileEventDrivenJobRecord("job-1", "context-1", "agent-1");
        createQuartzScheduleDrivenJobRecord("job-2", "context-2", "agent-1");
        createInternalEventDrivenJobRecord("job-3", "context-3", "agent-2");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByAgent("agent-1", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertEquals("agent-1", record.getAgentName())
        );
    }

    // Test: findByContextIdAndJobName

    @Test
    @Transactional
    public void testFindByContextIdAndJobName() {
        // Given
        String contextName = "search-context";
        String jobName = "search-job";
        createFileEventDrivenJobRecord(jobName, contextName, "agent-1");
        createQuartzScheduleDrivenJobRecord("other-job", contextName, "agent-2");

        // When
        HibernateSchedulerJobRecord found = dao.findByContextIdAndJobName(contextName, jobName);

        // Then
        assertNotNull(found);
        assertEquals(jobName, found.getJobName());
        assertEquals(contextName, found.getContextName());
    }

    @Test
    @Transactional
    public void testFindByContextIdAndJobName_NotFound() {
        // When
        HibernateSchedulerJobRecord result = dao.findByContextIdAndJobName("non-existent-context", "non-existent-job");

        // Then
        assertNull(result);
    }

    // Test: findByFilter

    @Test
    @Transactional
    public void testFindByFilter_WithJobTypeFilter() {
        // Given
        createFileEventDrivenJobRecord("file-job-1", "context-1", "agent-1");
        createFileEventDrivenJobRecord("file-job-2", "context-2", "agent-2");
        createQuartzScheduleDrivenJobRecord("quartz-job-1", "context-3", "agent-3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setJobTypeFilter(JobConstants.FILE_EVENT_DRIVEN_JOB);

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertEquals(JobConstants.FILE_EVENT_DRIVEN_JOB, record.getType())
        );
    }

    @Test
    @Transactional
    public void testFindByFilter_WithJobNameFilter() {
        // Given
        createFileEventDrivenJobRecord("search-job-1", "context-1", "agent-1");
        createQuartzScheduleDrivenJobRecord("search-job-2", "context-2", "agent-2");
        createInternalEventDrivenJobRecord("other-job", "context-3", "agent-3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setJobNameFilter("search");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertTrue(record.getJobName().contains("search"))
        );
    }

    @Test
    @Transactional
    public void testFindByFilter_WithDisplayNameFilter() {
        // Given
        createFileEventDrivenJobRecord("job-1", "context-1", "agent-1"); // Display: job-1
        createQuartzScheduleDrivenJobRecord("job-2", "context-2", "agent-2"); // Display: job-2
        createInternalEventDrivenJobRecord("other", "context-3", "agent-3"); // Display: other

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setDisplayNameFilter("job");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    @Transactional
    public void testFindByFilter_WithContextNamesFilter() {
        // Given
        createFileEventDrivenJobRecord("job-1", "context-a", "agent-1");
        createQuartzScheduleDrivenJobRecord("job-2", "context-b", "agent-2");
        createInternalEventDrivenJobRecord("job-3", "context-c", "agent-3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setContextNames(Arrays.asList("context-a", "context-b"));

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    @Transactional
    public void testFindByFilter_WithHeldFilter() {
        // Given
        HibernateFileEventDrivenJobRecord heldRecord = createFileEventDrivenJobRecord("held-job", "context-1", "agent-1");
        heldRecord.setHeld(true);
        fileEventDrivenJobDao.save(heldRecord);

        createQuartzScheduleDrivenJobRecord("normal-job", "context-2", "agent-2");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setHeld(true);

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().get(0).isHeld());
    }

    @Test
    @Transactional
    public void testFindByFilter_WithSkippedFilter() {
        // Given
        HibernateFileEventDrivenJobRecord skippedRecord = createFileEventDrivenJobRecord("skipped-job", "context-1", "agent-1");
        skippedRecord.setSkipped(true);
        fileEventDrivenJobDao.save(skippedRecord);

        createQuartzScheduleDrivenJobRecord("normal-job", "context-2", "agent-2");


        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setSkipped(true);

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().get(0).isSkipped());
    }

    @Test
    @Transactional
    public void testFindByFilter_WithSorting() {
        // Given
        createFileEventDrivenJobRecord("zzz-job", "context-1", "agent-1");
        createQuartzScheduleDrivenJobRecord("aaa-job", "context-2", "agent-2");
        createInternalEventDrivenJobRecord("mmm-job", "context-3", "agent-3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();

        // When - ascending
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByFilter(filter, -1, -1, "jobName", "ASCENDING");

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        List<HibernateSchedulerJobRecord> list = results.getResultList();
        assertTrue(list.get(0).getJobName().compareTo(list.get(list.size()-1).getJobName()) <= 0);
    }

    // Test: delete

    @Test
    @Transactional
    public void testDelete() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("delete-job", "delete-context", "agent-1");
        String id = record.getId();

        // When
        dao.delete(record);

        // Then
        HibernateSchedulerJobRecord found = dao.findById(id);
        assertNull(found);
    }

    @Test
    @Transactional
    public void testDelete_NullRecord() {
        // When - should not throw exception
        dao.delete(null);

        // Then - no exception
    }

    // Test: deleteByAgentName

    @Test
    @Transactional
    public void testDeleteByAgentName() {
        // Given
        createFileEventDrivenJobRecord("job-1", "context-1", "agent-to-delete");
        createQuartzScheduleDrivenJobRecord("job-2", "context-2", "agent-to-delete");
        createInternalEventDrivenJobRecord("job-3", "context-3", "agent-keep");

        // When
        dao.deleteByAgentName("agent-to-delete");

        // Then
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByAgent("agent-to-delete", -1, -1);
        assertEquals(0L, results.getTotalNumberOfResults());

        SearchResults<HibernateSchedulerJobRecord> keptResults = dao.findByAgent("agent-keep", -1, -1);
        assertEquals(1L, keptResults.getTotalNumberOfResults());
    }

    // Test: deleteByContextName

    @Test
    @Transactional
    public void testDeleteByContextName() {
        // Given
        String contextToDelete = "context-to-delete";
        createFileEventDrivenJobRecord("job-1", contextToDelete, "agent-1");
        createQuartzScheduleDrivenJobRecord("job-2", contextToDelete, "agent-2");
        createInternalEventDrivenJobRecord("job-3", "other-context", "agent-3");

        // When
        dao.deleteByContextName(contextToDelete);

        // Then
        SearchResults<HibernateSchedulerJobRecord> results = dao.findByContext(contextToDelete, -1, -1);
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    // Test: save operations throw UnsupportedOperationException

    @Test(expected = UnsupportedOperationException.class)
    public void testSave_ThrowsException() {
        // When - should throw UnsupportedOperationException
        dao.save(new HibernateFileEventDrivenJobRecord());
    }


    // Test: Polymorphic queries across all job types

    @Test
    @Transactional
    public void testPolymorphicQuery_AllJobTypes() {
        // Given - create one of each job type
        createFileEventDrivenJobRecord("file-job", "context-1", "agent-1");
        createQuartzScheduleDrivenJobRecord("quartz-job", "context-2", "agent-2");
        createInternalEventDrivenJobRecord("internal-job", "context-3", "agent-3");
        createGlobalEventJobRecord("global-job", "context-4", "agent-4");
        createContextStartJobRecord("start-job", "context-5", "agent-5");
        createContextTerminalJobRecord("terminal-job", "context-6", "agent-6");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 6);

        // Verify we got all job types
        Set<String> jobTypes = new HashSet<>();
        results.getResultList().forEach(record -> jobTypes.add(record.getType()));
        assertTrue(jobTypes.contains(JobConstants.FILE_EVENT_DRIVEN_JOB));
        assertTrue(jobTypes.contains(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB));
        assertTrue(jobTypes.contains(JobConstants.INTERNAL_EVENT_DRIVEN_JOB));
        assertTrue(jobTypes.contains(JobConstants.GLOBAL_EVENT_JOB));
        assertTrue(jobTypes.contains(JobConstants.CONTEXT_START_JOB));
        assertTrue(jobTypes.contains(JobConstants.CONTEXT_TERMINAL_JOB));
    }


    @Test
    @Transactional
    public void testMultipleJobTypeInstances() {
        // Given - create multiple instances of different types
        createFileEventDrivenJobRecord("file-1", "ctx-1", "agent-1");
        createFileEventDrivenJobRecord("file-2", "ctx-2", "agent-2");
        createQuartzScheduleDrivenJobRecord("quartz-1", "ctx-3", "agent-3");

        // When
        SearchResults<HibernateSchedulerJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertTrue(results.getTotalNumberOfResults() >= 3);

        // Verify inheritance works correctly
        results.getResultList().forEach(record -> {
            assertTrue(record instanceof HibernateSchedulerJobRecord);
            assertNotNull(record.getType());
            assertNotNull(record.getJob());
        });
    }
}
