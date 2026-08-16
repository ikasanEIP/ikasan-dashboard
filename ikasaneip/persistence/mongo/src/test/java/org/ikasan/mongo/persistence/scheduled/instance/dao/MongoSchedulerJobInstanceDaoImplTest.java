package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoSchedulerJobInstanceDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoSchedulerJobInstanceDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private SchedulerJobInstanceDao dao;

    @Autowired
    private MongoSchedulerJobInstanceRecordRepository repository;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @After
    public void tearDown() {
        mongoDBContainer.getReplicaSetUrl(); // Ensure container is running
        this.repository.deleteAll();
    }

    private SchedulerJobInstance createJobInstance(String jobName, String contextName,
                                                   String contextInstanceId, InstanceStatus status) {
        SchedulerJobInstance jobInstance = new QuartzScheduleDrivenJobInstanceImpl();
        jobInstance.setJobName(jobName);
        jobInstance.setDisplayName(jobName + " Display");
        jobInstance.setContextName(contextName);
        jobInstance.setContextInstanceId(contextInstanceId);
        jobInstance.setStatus(status);
        return jobInstance;
    }

    private MongoSchedulerJobInstanceRecordImpl createJobInstanceRecord(String jobName,
                                                                        String contextName,
                                                                        String contextInstanceId,
                                                                        InstanceStatus status) {
        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl(contextInstanceId);
        record.setId(contextInstanceId + "-" + jobName);
        record.setJobName(jobName);
        record.setDisplayName(jobName + " Display");
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setStatus(status.name());
        record.setType("InternalEventDrivenJob");
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis() + 1000);
        record.setModifiedBy("test-user");

        SchedulerJobInstance jobInstance = createJobInstance(jobName, contextName, contextInstanceId, status);
        record.setSchedulerJobInstance(jobInstance);

        return record;
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void testConstructorValidation() {
        // Test null repository
        try {
            new MongoSchedulerJobInstanceDaoImpl(null, null);
            fail("Expected IllegalArgumentException for null repository");
        } catch (IllegalArgumentException e) {
            assertEquals("repository cannot be null", e.getMessage());
        }
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        MongoSchedulerJobInstanceRecordImpl record = createJobInstanceRecord(
            "test-job-1", "test-context", "ctx-instance-1", InstanceStatus.RUNNING
        );

        // When
        dao.save(record);

        // Then
        SchedulerJobInstanceRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context", found.getContextName());
        assertEquals("ctx-instance-1", found.getContextInstanceId());
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
    public void testSaveUpdatesTimestamps() throws InterruptedException {
        // Given
        MongoSchedulerJobInstanceRecordImpl record = createJobInstanceRecord(
            "test-job-2", "test-context", "ctx-instance-2", InstanceStatus.COMPLETE
        );

        long initialModifiedTime = record.getModifiedTimestamp();

        Thread.sleep(10);

        // When
        dao.save(record);

        // Then
        assertTrue(record.getModifiedTimestamp() > initialModifiedTime);
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceId() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-2", "context-1", "ctx-inst-1", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-3", "context-2", "ctx-inst-2", InstanceStatus.RUNNING));

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-1", -1, -1, "jobName", "ASC"
        );

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextInstanceIdWithPagination() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createJobInstanceRecord("job-" + i, "context-1", "ctx-inst-1", InstanceStatus.WAITING));
        }

        // When - First page
        SearchResults<SchedulerJobInstanceRecord> page1 = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-1", 5, 0, "jobName", "ASC"
        );

        // Then
        assertEquals(10L, page1.getTotalNumberOfResults());
        assertEquals(5, page1.getResultList().size());

        // When - Second page
        SearchResults<SchedulerJobInstanceRecord> page2 = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-1", 5, 5, "jobName", "ASC"
        );

        // Then
        assertEquals(10L, page2.getTotalNumberOfResults());
        assertEquals(5, page2.getResultList().size());
    }

    @Test
    public void testGetSchedulerJobInstancesByContextName() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-A", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-2", "context-A", "ctx-inst-2", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-3", "context-B", "ctx-inst-3", InstanceStatus.ERROR));

        // When
        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextName(
            "context-A", -1, -1, "jobName", "DESC"
        );

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testDoesJobPlanInstanceContainRepeatingJobs() {
        // Given - Add non-repeating job
        MongoSchedulerJobInstanceRecordImpl record1 = createJobInstanceRecord(
            "job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING
        );
        record1.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);

        InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("job-1");
        instance.setJobRepeatable(false);
        record1.setSchedulerJobInstance(instance);

        dao.save(record1);

        // When
        boolean containsRepeating = dao.doesJobPlanInstanceContainRepeatingJobs("ctx-inst-1");

        // Then
        assertFalse(containsRepeating);

        // Given - Add repeating job
        MongoSchedulerJobInstanceRecordImpl record2 = createJobInstanceRecord(
            "job-2", "context-1", "ctx-inst-2", InstanceStatus.WAITING
        );
        record2.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("job-2");
        instance.setJobRepeatable(true);
        record2.setSchedulerJobInstance(instance);

        dao.save(record2);

        // When
        containsRepeating = dao.doesJobPlanInstanceContainRepeatingJobs("ctx-inst-2");

        // Then
        assertTrue(containsRepeating);
    }

    @Test
    public void testGetScheduledContextInstancesByFilterJobName() {
        // Given
        dao.save(createJobInstanceRecord("my-job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("my-job-2", "context-1", "ctx-inst-2", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("other-job", "context-1", "ctx-inst-3", InstanceStatus.ERROR));

        // When
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName("my-job");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, "jobName", "ASC"
        );

        // Then
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterStatus() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-1", "ctx-inst-1", InstanceStatus.ERROR));
        dao.save(createJobInstanceRecord("job-2", "context-1", "ctx-inst-2", InstanceStatus.ERROR));
        dao.save(createJobInstanceRecord("job-3", "context-1", "ctx-inst-3", InstanceStatus.COMPLETE));

        // When
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setStatus("ERROR");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, "jobName", "ASC"
        );

        // Then
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterMultipleCriteria() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-A", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-2", "context-A", "ctx-inst-2", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-3", "context-B", "ctx-inst-3", InstanceStatus.RUNNING));

        // When
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context-A");
        filter.setStatus("RUNNING");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, "jobName", "ASC"
        );

        // Then
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("job-1", results.getResultList().get(0).getJobName());
    }

    @Test
    public void testGetScheduledContextInstancesByFilterTimeWindow() {
        // Given
        long now = System.currentTimeMillis();

        MongoSchedulerJobInstanceRecordImpl record1 = createJobInstanceRecord(
            "job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING
        );
        record1.setStartTime(now - 10000);
        record1.setEndTime(now - 5000);
        dao.save(record1);

        MongoSchedulerJobInstanceRecordImpl record2 = createJobInstanceRecord(
            "job-2", "context-1", "ctx-inst-2", InstanceStatus.COMPLETE
        );
        record2.setStartTime(now + 10000);
        record2.setEndTime(now + 15000);
        dao.save(record2);

        // When
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setStartTimeWindowStart(now - 15000);
        filter.setStartTimeWindowEnd(now);

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, -1, -1, "jobName", "ASC"
        );

        // Then
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("job-1", results.getResultList().get(0).getJobName());
    }

    @Test
    public void testGetJobStatusCountForContextInstances() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-2", "context-1", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-3", "context-1", "ctx-inst-1", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-4", "context-1", "ctx-inst-1", InstanceStatus.ERROR));

        dao.save(createJobInstanceRecord("job-5", "context-2", "ctx-inst-2", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-6", "context-2", "ctx-inst-2", InstanceStatus.COMPLETE));

        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId("ctx-inst-1");
        SearchResults<SchedulerJobInstanceRecord> searchResults = dao.getScheduledContextInstancesByFilter(filter, 100, 0, null, null);

        // When
        List<ContextInstanceAggregateJobStatus> results = dao.getJobStatusCountForContextInstances(
            Arrays.asList("ctx-inst-1", "ctx-inst-2")
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        ContextInstanceAggregateJobStatus ctx1Status = results.stream()
            .filter(r -> "ctx-inst-1".equals(r.getContextInstanceId()))
            .findFirst()
            .orElse(null);

        assertNotNull(ctx1Status);
        assertEquals(2, ctx1Status.getStatusCount(InstanceStatus.RUNNING));
        assertEquals(1, ctx1Status.getStatusCount(InstanceStatus.COMPLETE));
        assertEquals(1, ctx1Status.getStatusCount(InstanceStatus.ERROR));

        ContextInstanceAggregateJobStatus ctx2Status = results.stream()
            .filter(r -> "ctx-inst-2".equals(r.getContextInstanceId()))
            .findFirst()
            .orElse(null);

        assertNotNull(ctx2Status);
        assertEquals(2, ctx2Status.getStatusCount(InstanceStatus.COMPLETE));
    }

    @Test
    public void testGetJobStatusCountForContextInstancesConsiderNonTargetedDuplication() {
        // Given
        MongoSchedulerJobInstanceRecordImpl record1 = createJobInstanceRecord(
            "job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING
        );
        record1.setTargetResidingContextOnly(true);
        dao.save(record1);

        MongoSchedulerJobInstanceRecordImpl record2 = createJobInstanceRecord(
            "job-2", "context-1", "ctx-inst-1", InstanceStatus.RUNNING
        );
        record2.setTargetResidingContextOnly(false); // Should be excluded
        dao.save(record2);

        MongoSchedulerJobInstanceRecordImpl record3 = createJobInstanceRecord(
            "job-3", "context-1", "ctx-inst-1", InstanceStatus.COMPLETE
        );
        record3.setTargetResidingContextOnly(true);
        dao.save(record3);

        // When
        List<ContextInstanceAggregateJobStatus> results =
            dao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(
                Arrays.asList("ctx-inst-1")
            );

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        ContextInstanceAggregateJobStatus status = results.get(0);
        assertEquals(1, status.getStatusCount(InstanceStatus.RUNNING)); // Only targeted job counted
        assertEquals(1, status.getStatusCount(InstanceStatus.COMPLETE));
    }

    @Test
    public void testDeleteSchedulerJobInstances() {
        // Given
        dao.save(createJobInstanceRecord("job-1", "context-1", "ctx-inst-1", InstanceStatus.RUNNING));
        dao.save(createJobInstanceRecord("job-2", "context-1", "ctx-inst-1", InstanceStatus.COMPLETE));
        dao.save(createJobInstanceRecord("job-3", "context-2", "ctx-inst-2", InstanceStatus.RUNNING));

        // Verify initial state
        SearchResults<SchedulerJobInstanceRecord> initialResults = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-1", -1, -1, "jobName", "ASC"
        );
        assertEquals(2L, initialResults.getTotalNumberOfResults());

        // When
        dao.deleteSchedulerJobInstances("ctx-inst-1");

        // Then
        SearchResults<SchedulerJobInstanceRecord> afterDeleteResults = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-1", -1, -1, "jobName", "ASC"
        );
        assertEquals(0L, afterDeleteResults.getTotalNumberOfResults());

        // Verify other context instances are not affected
        SearchResults<SchedulerJobInstanceRecord> otherContextResults = dao.getSchedulerJobInstancesByContextInstanceId(
            "ctx-inst-2", -1, -1, "jobName", "ASC"
        );
        assertEquals(1L, otherContextResults.getTotalNumberOfResults());
    }

    @Test
    public void testSaveInvalidType() {
        // Given
        SchedulerJobInstanceRecord invalidRecord = new SchedulerJobInstanceRecord() {
            @Override
            public String getId() { return "test"; }
            @Override
            public String getType() { return "test"; }
            @Override
            public String getJobName() { return "test"; }
            @Override
            public void setJobName(String jobName) {}
            @Override
            public String getDisplayName() { return "test"; }
            @Override
            public void setDisplayName(String displayName) {}
            @Override
            public String getContextName() { return "test"; }
            @Override
            public void setContextName(String contextName) {}
            @Override
            public String getChildContextName() { return null; }
            @Override
            public void setChildContextName(String childContextName) {}
            @Override
            public String getContextInstanceId() { return "test"; }
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
            public String getModifiedBy() { return "test"; }
            @Override
            public void setModifiedBy(String modifiedBy) {}
            @Override
            public String getManuallySubmittedBy() { return null; }
            @Override
            public void setManuallySubmittedBy(String manuallySubmittedBy) {}
        };

        // When/Then
        try {
            dao.save(invalidRecord);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MongoSchedulerJobInstanceRecordImpl"));
        }
    }

    /**
     * Simple implementation of SchedulerJobInstanceSearchFilter for testing.
     */
    private static class SchedulerJobInstanceSearchFilterImpl implements SchedulerJobInstanceSearchFilter {
        private String jobName;
        private boolean includeStartAndTerminalJobsInSearchResults = true;
        private String displayNameFilter;
        private String jobType;
        private String contextName;
        private String contextInstanceId;
        private String childContextName;
        private String status;
        private Boolean targetResidingContextOnly;
        private Boolean participatesInLock;
        private long startTimeWindowStart;
        private long startTimeWindowEnd;
        private long endTimeWindowStart;
        private long endTimeWindowEnd;

        @Override
        public String getJobName() { return jobName; }
        @Override
        public void setJobName(String jobName) { this.jobName = jobName; }
        @Override
        public boolean includeStartAndTerminalJobsInSearchResults() { return includeStartAndTerminalJobsInSearchResults; }
        @Override
        public void setIncludeStartAndTerminalJobsInSearchResults(boolean include) { this.includeStartAndTerminalJobsInSearchResults = include; }
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
        public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) { this.targetResidingContextOnly = targetResidingContextOnly; }
        @Override
        public Boolean isTargetResidingContextOnly() { return targetResidingContextOnly; }
        @Override
        public void setParticipatesInLock(Boolean participatesInLock) { this.participatesInLock = participatesInLock; }
        @Override
        public Boolean isParticipatesInLock() { return participatesInLock; }
        @Override
        public long getStartTimeWindowStart() { return startTimeWindowStart; }
        @Override
        public void setStartTimeWindowStart(long startTimeWindowStart) { this.startTimeWindowStart = startTimeWindowStart; }
        @Override
        public long getStartTimeWindowEnd() { return startTimeWindowEnd; }
        @Override
        public void setStartTimeWindowEnd(long startTimeWindowEnd) { this.startTimeWindowEnd = startTimeWindowEnd; }
        @Override
        public long getEndTimeWindowStart() { return endTimeWindowStart; }
        @Override
        public void setEndTimeWindowStart(long endTimeWindowStart) { this.endTimeWindowStart = endTimeWindowStart; }
        @Override
        public long getEndTimeWindowEnd() { return endTimeWindowEnd; }
        @Override
        public void setEndTimeWindowEnd(long endTimeWindowEnd) { this.endTimeWindowEnd = endTimeWindowEnd; }
    }

    @Test
    public void testGetSchedulerJobInstance_FileEventDrivenJob() throws Exception {
        // Given
        FileEventDrivenJobInstance jobInstance = new FileEventDrivenJobInstanceImpl();
        jobInstance.setJobName("file-event-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.WAITING);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-file-job");
        record.setJobName("file-event-job");
        record.setContextName("test-context");
        record.setType(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-file-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof FileEventDrivenJobInstance);
        assertEquals("file-event-job", retrieved.getJobName());
        assertEquals(InstanceStatus.WAITING, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_QuartzScheduleDrivenJob() throws Exception {
        // Given
        QuartzScheduleDrivenJobInstanceImpl jobInstance = new QuartzScheduleDrivenJobInstanceImpl();
        jobInstance.setJobName("quartz-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.RUNNING);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-quartz-job");
        record.setJobName("quartz-job");
        record.setContextName("test-context");
        record.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-quartz-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof QuartzScheduleDrivenJobInstance);
        assertEquals("quartz-job", retrieved.getJobName());
        assertEquals(InstanceStatus.RUNNING, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_InternalEventDrivenJob() throws Exception {
        // Given
        InternalEventDrivenJobInstanceImpl jobInstance = new InternalEventDrivenJobInstanceImpl();
        jobInstance.setJobName("internal-event-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.COMPLETE);
        jobInstance.setIdentifier("internal-id-123");

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-internal-job");
        record.setJobName("internal-event-job");
        record.setContextName("test-context");
        record.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-internal-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof InternalEventDrivenJobInstance);
        assertEquals("internal-event-job", retrieved.getJobName());
        assertEquals(InstanceStatus.COMPLETE, retrieved.getStatus());
        assertEquals("internal-id-123", ((InternalEventDrivenJobInstance) retrieved).getIdentifier());
    }

    @Test
    public void testGetSchedulerJobInstance_GlobalEventJob() throws Exception {
        // Given
        GlobalEventJobInstanceImpl jobInstance = new GlobalEventJobInstanceImpl();
        jobInstance.setJobName("global-event-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.WAITING);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-global-job");
        record.setJobName("global-event-job");
        record.setContextName("test-context");
        record.setType(JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-global-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof GlobalEventJobInstance);
        assertEquals("global-event-job", retrieved.getJobName());
        assertEquals(InstanceStatus.WAITING, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_ContextStartJob() throws Exception {
        // Given
        ContextStartJobInstanceImpl jobInstance = new ContextStartJobInstanceImpl();
        jobInstance.setJobName("context-start-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.COMPLETE);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-start-job");
        record.setJobName("context-start-job");
        record.setContextName("test-context");
        record.setType(JobConstants.CONTEXT_START_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-start-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof ContextStartJobInstance);
        assertEquals("context-start-job", retrieved.getJobName());
        assertEquals(InstanceStatus.COMPLETE, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_ContextTerminalJob() throws Exception {
        // Given
        ContextTerminalJobInstanceImpl jobInstance = new ContextTerminalJobInstanceImpl();
        jobInstance.setJobName("context-terminal-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.COMPLETE);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-terminal-job");
        record.setJobName("context-terminal-job");
        record.setContextName("test-context");
        record.setType(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-terminal-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof ContextTerminalJobInstance);
        assertEquals("context-terminal-job", retrieved.getJobName());
        assertEquals(InstanceStatus.COMPLETE, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_LocalEventJob() throws Exception {
        // Given
        LocalEventJobInstanceImpl jobInstance = new LocalEventJobInstanceImpl();
        jobInstance.setJobName("local-event-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.WAITING);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-local-job");
        record.setJobName("local-event-job");
        record.setContextName("test-context");
        record.setType(JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-local-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof LocalEventJobInstance);
        assertEquals("local-event-job", retrieved.getJobName());
        assertEquals(InstanceStatus.WAITING, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_BridgingJob() throws Exception {
        // Given
        BridgingJobInstanceImpl jobInstance = new BridgingJobInstanceImpl();
        jobInstance.setJobName("bridging-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.RUNNING);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-bridging-job");
        record.setJobName("bridging-job");
        record.setContextName("test-context");
        record.setType(JobConstants.BRIDGING_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-bridging-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof BridgingJobInstance);
        assertEquals("bridging-job", retrieved.getJobName());
        assertEquals(InstanceStatus.RUNNING, retrieved.getStatus());
    }

    @Test
    public void testGetSchedulerJobInstance_SkippedStatus() throws Exception {
        // Given
        QuartzScheduleDrivenJobInstanceImpl jobInstance = new QuartzScheduleDrivenJobInstanceImpl();
        jobInstance.setJobName("skipped-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.SKIPPED);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-skipped-job");
        record.setJobName("skipped-job");
        record.setContextName("test-context");
        record.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-skipped-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertEquals(InstanceStatus.SKIPPED, retrieved.getStatus());
        assertTrue(retrieved.isSkip());
    }

    @Test
    public void testGetSchedulerJobInstance_NonSkippedStatus() throws Exception {
        // Given
        QuartzScheduleDrivenJobInstanceImpl jobInstance = new QuartzScheduleDrivenJobInstanceImpl();
        jobInstance.setJobName("normal-job");
        jobInstance.setContextName("test-context");
        jobInstance.setContextInstanceId("ctx-1");
        jobInstance.setStatus(InstanceStatus.COMPLETE);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl("ctx-1");
        record.setId("test-normal-job");
        record.setJobName("normal-job");
        record.setContextName("test-context");
        record.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        record.setSchedulerJobInstance(jobInstance);

        // When
        dao.save(record);
        SchedulerJobInstanceRecord saved = dao.findById("test-normal-job");

        // Then
        assertNotNull(saved);
        SchedulerJobInstance retrieved = saved.getSchedulerJobInstance();
        assertNotNull(retrieved);
        assertEquals(InstanceStatus.COMPLETE, retrieved.getStatus());
        assertFalse(retrieved.isSkip());
    }
}
