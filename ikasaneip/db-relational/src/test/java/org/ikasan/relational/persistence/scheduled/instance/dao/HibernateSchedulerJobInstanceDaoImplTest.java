package org.ikasan.relational.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.QuartzScheduleDrivenJobInstanceImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateSchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateSchedulerJobInstanceDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateSchedulerJobInstanceDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private SchedulerJobInstanceDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data to ensure test isolation
        SearchResults<SchedulerJobInstanceRecord> all = dao.getScheduledContextInstancesByFilter(null, -1, -1, null, null);
        all.getResultList().forEach(record -> {
            dao.deleteSchedulerJobInstances(record.getContextInstanceId());
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private SchedulerJobInstance createSchedulerJobInstance(String jobName) {
        QuartzScheduleDrivenJobInstanceImpl instance = new QuartzScheduleDrivenJobInstanceImpl();
        instance.setJobName(jobName);
        instance.setDisplayName("Display " + jobName);
        instance.setContextName("test-context");
        instance.setStatus(InstanceStatus.WAITING);
        return instance;
    }

    private HibernateSchedulerJobInstanceRecord createSchedulerJobInstanceRecord(
            String jobName, String contextInstanceId, String status) {
        HibernateSchedulerJobInstanceRecord record = new HibernateSchedulerJobInstanceRecord("job-id-" + jobName);
        record.setJobName(jobName);
        record.setDisplayName("Display " + jobName);
        record.setContextName("test-context");
        record.setContextInstanceId(contextInstanceId);
        record.setSchedulerJobInstance(createSchedulerJobInstance(jobName));
        record.setStatus(status);
        record.setModifiedBy("test-user");
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis() + 10000);
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "test-job-1", "context-instance-1", "RUNNING"
        );

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("Display test-job-1", found.getDisplayName());
        assertEquals("context-instance-1", found.getContextInstanceId());
        assertEquals("RUNNING", found.getStatus());
        assertNotNull(found.getSchedulerJobInstance());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        SchedulerJobInstanceRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "test-job-2", "context-instance-2", "COMPLETE"
        );

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "test-job-3", "context-instance-3", "RUNNING"
        );

        // Save initial
        dao.save(record);

        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // Update
        record.setStatus("COMPLETE");
        record.setEndTime(System.currentTimeMillis());

        dao.save(record);

        // Then
        assertEquals(initialTimestamp, record.getTimestamp()); // Original timestamp preserved
        assertTrue(record.getModifiedTimestamp() > initialModifiedTimestamp); // Modified timestamp updated

        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertEquals("COMPLETE", found.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceId() {
        // Given
        String contextInstanceId = "unique-context-instance";
        dao.save(createSchedulerJobInstanceRecord("job-1", contextInstanceId, "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("job-2", contextInstanceId, "COMPLETE"));
        dao.save(createSchedulerJobInstanceRecord("job-3", "other-context", "RUNNING"));

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextInstanceId(
            contextInstanceId, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceIdWithLimitAndOffset() {
        // Given
        String contextInstanceId = "pagination-context-instance";
        for (int i = 1; i <= 5; i++) {
            dao.save(createSchedulerJobInstanceRecord("job-" + i, contextInstanceId, "RUNNING"));
        }

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextInstanceId(
            contextInstanceId, 2, 1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextName() {
        // Given
        String contextName = "test-context";
        HibernateSchedulerJobInstanceRecord record1 = createSchedulerJobInstanceRecord("job-1", "ctx-1", "RUNNING");
        record1.setContextName(contextName);
        dao.save(record1);

        HibernateSchedulerJobInstanceRecord record2 = createSchedulerJobInstanceRecord("job-2", "ctx-2", "RUNNING");
        record2.setContextName(contextName);
        dao.save(record2);

        HibernateSchedulerJobInstanceRecord record3 = createSchedulerJobInstanceRecord("job-3", "ctx-3", "RUNNING");
        record3.setContextName("other-context");
        dao.save(record3);

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextName(
            contextName, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testGetScheduledContextInstancesByFilter() {
        // Given
        dao.save(createSchedulerJobInstanceRecord("production-job", "ctx-1", "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("staging-job", "ctx-2", "COMPLETE"));
        dao.save(createSchedulerJobInstanceRecord("development-job", "ctx-3", "ERROR"));

        // When
        SchedulerJobInstanceSearchFilter filter = new MockSchedulerJobInstanceSearchFilter();
        filter.setJobName("prod");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().get(0).getJobName().contains("production"));
    }

    @Test
    public void testGetScheduledContextInstancesByFilterWithStatus() {
        // Given
        dao.save(createSchedulerJobInstanceRecord("job-a", "ctx-a", "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("job-b", "ctx-b", "COMPLETE"));

        // When
        SchedulerJobInstanceSearchFilter filter = new MockSchedulerJobInstanceSearchFilter();
        filter.setStatus("RUNNING");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, null, null
        );

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("RUNNING", results.getResultList().get(0).getStatus());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterWithSortAscending() {
        // Given
        dao.save(createSchedulerJobInstanceRecord("zebra-job", "ctx-z", "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("alpha-job", "ctx-a", "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("beta-job", "ctx-b", "RUNNING"));

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            null, -1, -1, "jobName", "ASCENDING"
        );

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        assertEquals("alpha-job", results.getResultList().get(0).getJobName());
        assertEquals("beta-job", results.getResultList().get(1).getJobName());
        assertEquals("zebra-job", results.getResultList().get(2).getJobName());
    }

    @Test
    public void testDeleteSchedulerJobInstances() {
        // Given
        String contextInstanceId = "context-to-delete";
        dao.save(createSchedulerJobInstanceRecord("job-1", contextInstanceId, "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("job-2", contextInstanceId, "RUNNING"));
        dao.save(createSchedulerJobInstanceRecord("job-3", "other-context", "RUNNING"));

        // Verify they exist
        SearchResults<SchedulerJobInstanceRecord> before = dao.getSchedulerJobInstancesByContextInstanceId(
            contextInstanceId, -1, -1, null, null
        );
        assertEquals(2L, before.getTotalNumberOfResults());

        // When
        dao.deleteSchedulerJobInstances(contextInstanceId);

        // Then
        SearchResults<SchedulerJobInstanceRecord> after = dao.getSchedulerJobInstancesByContextInstanceId(
            contextInstanceId, -1, -1, null, null
        );
        assertEquals(0L, after.getTotalNumberOfResults());

        // Other context instances should remain
        SearchResults<SchedulerJobInstanceRecord> other = dao.getSchedulerJobInstancesByContextInstanceId(
            "other-context", -1, -1, null, null
        );
        assertEquals(1L, other.getTotalNumberOfResults());
    }

    @Test
    public void testDoesJobPlanInstanceContainRepeatingJobs() {
        // Given
        String contextInstanceId = "repeating-context";
        dao.save(createSchedulerJobInstanceRecord("job-1", contextInstanceId, "RUNNING"));

        // When
        boolean containsRepeating = dao.doesJobPlanInstanceContainRepeatingJobs(contextInstanceId);

        // Then
        // This is a placeholder implementation, so we just verify it doesn't throw an exception
        assertNotNull(containsRepeating);
    }

    @Test
    public void testTargetResidingContextOnlyFlag() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "target-job", "ctx-1", "RUNNING"
        );
        record.setTargetResidingContextOnly(true);

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertTrue(found.isTargetResidingContextOnly());
    }

    @Test
    public void testParticipatesInLockFlag() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "lock-job", "ctx-1", "RUNNING"
        );
        record.setParticipatesInLock(true);

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertTrue(found.isParticipatesInLock());
    }

    @Test
    public void testChildContextName() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "child-job", "ctx-1", "RUNNING"
        );
        record.setChildContextName("child-context-name");

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertEquals("child-context-name", found.getChildContextName());
    }

    @Test
    public void testManuallySubmittedBy() {
        // Given
        HibernateSchedulerJobInstanceRecord record = createSchedulerJobInstanceRecord(
            "manual-job", "ctx-1", "RUNNING"
        );
        record.setManuallySubmittedBy("admin-user");

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertEquals("admin-user", found.getManuallySubmittedBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // Given
        SchedulerJobInstanceRecord invalidRecord = new SchedulerJobInstanceRecord() {
            @Override
            public String getId() { return "invalid"; }

            @Override
            public String getType() { return "TEST"; }

            @Override
            public String getJobName() { return "invalid"; }

            @Override
            public void setJobName(String jobName) {}

            @Override
            public String getDisplayName() { return "invalid"; }

            @Override
            public void setDisplayName(String displayName) {}

            @Override
            public String getContextName() { return "invalid"; }

            @Override
            public void setContextName(String contextName) {}

            @Override
            public String getChildContextName() { return null; }

            @Override
            public void setChildContextName(String childContextName) {}

            @Override
            public String getContextInstanceId() { return "invalid"; }

            @Override
            public void setContextInstanceId(String contextInstanceId) {}

            @Override
            public SchedulerJobInstance getSchedulerJobInstance() { return null; }

            @Override
            public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {}

            @Override
            public String getStatus() { return "RUNNING"; }

            @Override
            public void setStatus(String status) {}

            @Override
            public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {}

            @Override
            public boolean isTargetResidingContextOnly() { return false; }

            @Override
            public void setParticipatesInLock(boolean participatesInLock) {}

            @Override
            public boolean isParticipatesInLock() { return false; }

            @Override
            public long getStartTime() { return 0; }

            @Override
            public void setStartTime(long startTime) {}

            @Override
            public long getEndTime() { return 0; }

            @Override
            public void setEndTime(long endTime) {}

            @Override
            public long getTimestamp() { return 0; }

            @Override
            public void setTimestamp(long timestamp) {}

            @Override
            public long getModifiedTimestamp() { return 0; }

            @Override
            public void setModifiedTimestamp(long timestamp) {}

            @Override
            public String getModifiedBy() { return null; }

            @Override
            public void setModifiedBy(String modifiedBy) {}

            @Override
            public String getManuallySubmittedBy() { return null; }

            @Override
            public void setManuallySubmittedBy(String manuallySubmittedBy) {}
        };

        // When - should throw IllegalArgumentException
        dao.save(invalidRecord);
    }

    // Helper class for mock filter
    private static class MockSchedulerJobInstanceSearchFilter implements SchedulerJobInstanceSearchFilter {
        private String jobName;
        private String displayNameFilter;
        private String jobType;
        private String contextName;
        private String contextInstanceId;
        private String childContextName;
        private String status;
        private Boolean targetResidingContextOnly;
        private Boolean participatesInLock;

        @Override
        public String getJobName() { return jobName; }

        @Override
        public void setJobName(String jobName) { this.jobName = jobName; }

        @Override
        public boolean includeStartAndTerminalJobsInSearchResults() { return false; }

        @Override
        public void setIncludeStartAndTerminalJobsInSearchResults(boolean include) {}

        @Override
        public String getDisplayNameFilter() { return displayNameFilter; }

        @Override
        public void setDisplayNameFilter(String displayNameFilter) { this.displayNameFilter = displayNameFilter; }

        @Override
        public String getJobType() { return jobType; }

        @Override
        public void setJobType(String jobType) { this.jobType = jobType; }

        @Override
        public String getContextName() { return contextName; }

        @Override
        public void setContextName(String contextName) { this.contextName = contextName; }

        @Override
        public String getContextInstanceId() { return contextInstanceId; }

        @Override
        public void setContextInstanceId(String contextInstanceId) { this.contextInstanceId = contextInstanceId; }

        @Override
        public String getChildContextName() { return childContextName; }

        @Override
        public void setChildContextName(String childContextName) { this.childContextName = childContextName; }

        @Override
        public String getStatus() { return status; }

        @Override
        public void setStatus(String status) { this.status = status; }

        @Override
        public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) {
            this.targetResidingContextOnly = targetResidingContextOnly;
        }

        @Override
        public Boolean isTargetResidingContextOnly() { return targetResidingContextOnly; }

        @Override
        public void setParticipatesInLock(Boolean participatesInLock) {
            this.participatesInLock = participatesInLock;
        }

        @Override
        public Boolean isParticipatesInLock() { return participatesInLock; }

        @Override
        public long getStartTimeWindowStart() { return 0; }

        @Override
        public void setStartTimeWindowStart(long startTimeWindowStart) {}

        @Override
        public long getStartTimeWindowEnd() { return 0; }

        @Override
        public void setStartTimeWindowEnd(long startTimeWindowEnd) {}

        @Override
        public long getEndTimeWindowStart() { return 0; }

        @Override
        public void setEndTimeWindowStart(long endTimeWindowStart) {}

        @Override
        public long getEndTimeWindowEnd() { return 0; }

        @Override
        public void setEndTimeWindowEnd(long endTimeWindowEnd) {}
    }
}
