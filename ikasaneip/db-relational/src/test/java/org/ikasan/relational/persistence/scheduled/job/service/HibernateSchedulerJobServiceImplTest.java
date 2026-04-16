package org.ikasan.relational.persistence.scheduled.job.service;

import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.dao.*;
import org.ikasan.relational.persistence.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.search.SearchResults;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for HibernateSchedulerJobServiceImpl using Mockito.
 *
 * Tests all service methods including CRUD operations, filtering, bulk operations,
 * skip/hold/release functionality, and context renaming.
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateSchedulerJobServiceImplTest {

    /**
     * Inner class implementation of SchedulerJobSearchFilter for testing purposes.
     */
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
            // Not needed for basic testing
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

    @Mock
    private HibernateFileEventDrivenJobDaoImpl fileEventDrivenJobDao;

    @Mock
    private HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao;

    @Mock
    private HibernateQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao;

    @Mock
    private HibernateGlobalEventJobDaoImpl globalEventJobDao;

    @Mock
    private HibernateContextStartJobDaoImpl contextStartJobDao;

    @Mock
    private HibernateContextTerminalJobDaoImpl contextTerminalJobDao;

    @Mock
    private HibernateSchedulerJobDaoImpl schedulerJobDao;

    @InjectMocks
    private HibernateSchedulerJobServiceImpl service;

    // Constructor Tests

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_fileEventDrivenJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                null,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                contextStartJobDao,
                contextTerminalJobDao,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_internalEventDrivenJobTemplateDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                null,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                contextStartJobDao,
                contextTerminalJobDao,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_quartzScheduleDrivenJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                null,
                globalEventJobDao,
                contextStartJobDao,
                contextTerminalJobDao,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_globalEventJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                null,
                contextStartJobDao,
                contextTerminalJobDao,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_contextStartJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                null,
                contextTerminalJobDao,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_contextTerminalJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                contextStartJobDao,
                null,
                schedulerJobDao,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_schedulerJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                contextStartJobDao,
                contextTerminalJobDao,
                null,
                internalEventDrivenJobDao
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_internalEventDrivenJobDao_throwsException() {
        new HibernateSchedulerJobServiceImpl(
                fileEventDrivenJobDao,
                internalEventDrivenJobDao,
                quartzScheduleDrivenJobDao,
                globalEventJobDao,
                contextStartJobDao,
                contextTerminalJobDao,
                schedulerJobDao,
                null
        );
    }

    // Save Tests

    @Test
    public void test_save_null_records_should_not_throw_npe() {
        service.save(null, "system");
        verifyNoInteractions(fileEventDrivenJobDao, internalEventDrivenJobDao, quartzScheduleDrivenJobDao,
                globalEventJobDao, contextStartJobDao, contextTerminalJobDao);
    }

    @Test
    public void test_save_empty_list_should_not_throw_exception() {
        service.save(new ArrayList<>(), "system");
        verifyNoInteractions(fileEventDrivenJobDao, internalEventDrivenJobDao, quartzScheduleDrivenJobDao,
                globalEventJobDao, contextStartJobDao, contextTerminalJobDao);
    }

    // CRUD Tests - Internal Event Driven Job

    @Test
    public void test_saveInternalEventDrivenJob() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");

        when(internalEventDrivenJobDao.findById(anyString())).thenReturn(null);

        service.saveInternalEventDrivenJob(job, "tester");

        verify(internalEventDrivenJobDao).save(any(HibernateInternalEventDrivenJobRecord.class));
    }

    @Test
    public void test_findById() {
        String id = "testId";
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord(id);

        when(schedulerJobDao.findById(id)).thenReturn(record);

        HibernateSchedulerJobRecord result = service.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals(id, result.getId());
        verify(schedulerJobDao).findById(id);
    }

    @Test
    public void test_findByAgent() {
        String agentName = "testAgent";
        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 5L, 0L);

        when(schedulerJobDao.findByAgent(agentName, 10, 0)).thenReturn(searchResults);

        SearchResults<HibernateSchedulerJobRecord> results = service.findByAgent(agentName, 10, 0);

        Assert.assertNotNull(results);
        verify(schedulerJobDao).findByAgent(agentName, 10, 0);
    }

    @Test
    public void test_findByContext() {
        String contextName = "testContext";
        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 3L, 0L);

        when(schedulerJobDao.findByContext(contextName, 10, 0)).thenReturn(searchResults);

        SearchResults<HibernateSchedulerJobRecord> results = service.findByContext(contextName, 10, 0);

        Assert.assertNotNull(results);
        verify(schedulerJobDao).findByContext(contextName, 10, 0);
    }

    @Test
    public void test_findByContextNameAndJobName() {
        String contextName = "context";
        String jobName = "job";
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();

        when(schedulerJobDao.findByContextIdAndJobName(contextName, jobName)).thenReturn(record);

        HibernateSchedulerJobRecord result = service.findByContextNameAndJobName(contextName, jobName);

        Assert.assertNotNull(result);
        verify(schedulerJobDao).findByContextIdAndJobName(contextName, jobName);
    }

    @Test
    public void test_findByFilter() {
        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);

        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 1L, 0L);

        when(schedulerJobDao.findByFilter(any(), anyInt(), anyInt(), any(), any())).thenReturn(searchResults);

        SearchResults<HibernateSchedulerJobRecord> results = service.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        verify(schedulerJobDao).findByFilter(filter, 10, 0, null, null);
    }

    // Delete Tests

    @Test
    public void test_delete() {
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord("testId");

        service.delete(record);

        verify(schedulerJobDao).delete(record);
    }

    @Test
    public void test_deleteByAgentName() {
        String agentName = "agentToDelete";

        service.deleteByAgentName(agentName);

        verify(schedulerJobDao).deleteByAgentName(agentName);
    }

    @Test
    public void test_deleteByContextName() {
        String contextName = "contextToDelete";

        service.deleteByContextName(contextName);

        verify(schedulerJobDao).deleteByContextName(contextName);
    }

    // Save Job Types Tests

    @Test
    public void test_saveFileEventDrivenJob() {
        FileEventDrivenJob job = createFileEventDrivenJob("agent", "job", "context", "/path");

        when(fileEventDrivenJobDao.findById(anyString())).thenReturn(null);

        service.saveFileEventDrivenJob(job, "tester");

        verify(fileEventDrivenJobDao).save(any(HibernateFileEventDrivenJobRecord.class));
    }

    @Test
    public void test_saveQuartzScheduledJob() {
        QuartzScheduleDrivenJob job = createQuartzScheduleDrivenJob("agent", "job", "context", "0 0 * * * ?");

        when(quartzScheduleDrivenJobDao.findById(anyString())).thenReturn(null);

        service.saveQuartzScheduledJob(job, "tester");

        verify(quartzScheduleDrivenJobDao).save(any(HibernateQuartzScheduleDrivenJobRecord.class));
    }

    @Test
    public void test_saveGlobalEventJob() {
        GlobalEventJob job = createGlobalEventJob("agent", "job", "context");

        when(globalEventJobDao.findById(anyString())).thenReturn(null);

        service.saveGlobalEventJob(job, "tester");

        verify(globalEventJobDao).save(any(HibernateGlobalEventJobRecord.class));
    }

    @Test
    public void test_saveContextStartJob() {
        ContextStartJob job = createContextStartJob("agent", "job", "context");

        when(contextStartJobDao.findById(anyString())).thenReturn(null);

        service.saveContextStartJob(job, "tester");

        verify(contextStartJobDao).save(any(HibernateContextStartJobRecord.class));
    }

    @Test
    public void test_saveContextTerminalJob() {
        ContextTerminalJob job = createContextTerminalJob("agent", "job", "context");

        when(contextTerminalJobDao.findById(anyString())).thenReturn(null);

        service.saveContextTerminalJob(job, "tester");

        verify(contextTerminalJobDao).save(any(HibernateContextTerminalJobRecord.class));
    }

    // Batch Save Tests

    @Test
    public void test_save_mixedJobTypes() {
        List<SchedulerJob> jobs = new ArrayList<>();
        jobs.add(createInternalEventDrivenJob("agent1", "job1", "context", "cmd"));
        jobs.add(createFileEventDrivenJob("agent2", "job2", "context", "/path"));
        jobs.add(createQuartzScheduleDrivenJob("agent3", "job3", "context", "0 0 * * * ?"));

        service.save(jobs, "tester");

        verify(internalEventDrivenJobDao).save(any());
        verify(fileEventDrivenJobDao).save(any());
        verify(quartzScheduleDrivenJobDao).save(any());
    }

    @Test
    public void test_saveInternalEventDrivenJobs_batch() {
        List<InternalEventDrivenJob> jobs = Arrays.asList(
                createInternalEventDrivenJob("agent", "job1", "context", "cmd1"),
                createInternalEventDrivenJob("agent", "job2", "context", "cmd2")
        );

        service.saveInternalEventDrivenJobs(jobs, "tester");

        verify(internalEventDrivenJobDao, times(2)).save(any());
    }

    // Skip/Hold/Release Tests

    @Test
    public void test_skip_internalEventDrivenJob() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");
        job.setChildContextNames(Arrays.asList("child1", "child2"));
        job.setTargetResidingContextOnly(true);

        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setInternalEventDrivenJob(job);
        record.setTimestamp(System.currentTimeMillis());

        List<String> childContexts = Arrays.asList("child1", "child2");

        service.skip(record, childContexts, "tester");

        verify(internalEventDrivenJobDao).skip(any(HibernateInternalEventDrivenJobRecord.class),
                eq(childContexts), eq("tester"));
    }

    @Test
    public void test_hold_internalEventDrivenJob() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");
        job.setChildContextNames(Arrays.asList("child1"));

        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setInternalEventDrivenJob(job);
        record.setTimestamp(System.currentTimeMillis());

        List<String> childContexts = Arrays.asList("child1");

        service.hold(record, childContexts, "tester");

        verify(internalEventDrivenJobDao).hold(any(HibernateInternalEventDrivenJobRecord.class),
                eq(childContexts), eq("tester"));
    }

    @Test
    public void test_enable_internalEventDrivenJob() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");

        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setInternalEventDrivenJob(job);
        record.setTimestamp(System.currentTimeMillis());

        service.enable(record, "context", "tester");

        verify(internalEventDrivenJobDao).enable(any(HibernateInternalEventDrivenJobRecord.class), eq("tester"));
    }

    @Test
    public void test_release_internalEventDrivenJob() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");

        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setInternalEventDrivenJob(job);
        record.setTimestamp(System.currentTimeMillis());

        service.release(record, "tester");

        verify(internalEventDrivenJobDao).release(any(HibernateInternalEventDrivenJobRecord.class), eq("tester"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_skip_fileEventDrivenJob_throwsException() {
        FileEventDrivenJob job = createFileEventDrivenJob("agent", "job", "context", "/path");

        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setFileEventDrivenJob(job);

        service.skip(record, Arrays.asList("child1"), "tester");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_hold_fileEventDrivenJob_throwsException() {
        FileEventDrivenJob job = createFileEventDrivenJob("agent", "job", "context", "/path");

        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setFileEventDrivenJob(job);

        service.hold(record, Arrays.asList("child1"), "tester");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_release_fileEventDrivenJob_throwsException() {
        FileEventDrivenJob job = createFileEventDrivenJob("agent", "job", "context", "/path");

        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setFileEventDrivenJob(job);

        service.release(record, "tester");
    }

    // Bulk Operations Tests

    @Test
    public void test_holdAll() {
        String contextName = "holdAllContext";
        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 0L, 0L);

        when(schedulerJobDao.findByContextAndType(eq(contextName), eq(JobConstants.INTERNAL_EVENT_DRIVEN_JOB),
                eq(-1), eq(-1))).thenReturn(searchResults);

        service.holdAll(contextName, "tester");

        verify(schedulerJobDao).findByContextAndType(contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);
    }

    @Test
    public void test_releaseAll() {
        String contextName = "releaseAllContext";
        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 0L, 0L);

        when(schedulerJobDao.findByContextAndType(eq(contextName), eq(JobConstants.INTERNAL_EVENT_DRIVEN_JOB),
                eq(-1), eq(-1))).thenReturn(searchResults);

        service.releaseAll(contextName, "tester");

        verify(schedulerJobDao).findByContextAndType(contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);
    }

    @Test
    public void test_enableAll() {
        String contextName = "enableAllContext";
        List<HibernateSchedulerJobRecord> internalRecords = new ArrayList<>();
        List<HibernateSchedulerJobRecord> globalRecords = new ArrayList<>();

        SearchResults<HibernateSchedulerJobRecord> internalResults = new SearchResultsImpl<>(internalRecords, 0L, 0L);
        SearchResults<HibernateSchedulerJobRecord> globalResults = new SearchResultsImpl<>(globalRecords, 0L, 0L);

        when(schedulerJobDao.findByContextAndType(eq(contextName), eq(JobConstants.INTERNAL_EVENT_DRIVEN_JOB),
                eq(-1), eq(-1))).thenReturn(internalResults);
        when(schedulerJobDao.findByType(eq(JobConstants.GLOBAL_EVENT_JOB), eq(-1), eq(-1)))
                .thenReturn(globalResults);

        service.enableAll(contextName, "tester");

        verify(schedulerJobDao).findByContextAndType(contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);
        verify(schedulerJobDao).findByType(JobConstants.GLOBAL_EVENT_JOB, -1, -1);
    }

    // Rename Context Tests

    @Test
    public void test_renameContextForJobs() {
        String oldContextName = "oldContext";
        String newContextName = "newContext";

        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 0L, 0L);

        when(schedulerJobDao.findByContext(oldContextName, -1, -1)).thenReturn(searchResults);

        service.renameContextForJobs(oldContextName, newContextName, "tester");

        verify(schedulerJobDao).findByContext(oldContextName, -1, -1);
        verify(schedulerJobDao).deleteByContextName(oldContextName);
    }

    // Template Job Tests

    @Test
    public void test_saveInternalEventDrivenJobTemplate() {
        InternalEventDrivenJob job = createInternalEventDrivenJob("agent", "job", "context", "cmd");
        job.setTemplateJob(true);

        when(internalEventDrivenJobDao.findById(anyString())).thenReturn(null);

        service.saveInternalEventDrivenJobTemplate(job, "tester");

        verify(internalEventDrivenJobDao).save(any(HibernateInternalEventDrivenJobRecord.class));
    }

    @Test
    public void test_saveInternalEventDrivenJobTemplates_batch() {
        List<InternalEventDrivenJob> templates = Arrays.asList(
                createInternalEventDrivenJob("agent", "template1", "context", "cmd1"),
                createInternalEventDrivenJob("agent", "template2", "context", "cmd2")
        );

        service.saveInternalEventDrivenJobTemplates(templates, "tester");

        verify(internalEventDrivenJobDao, times(2)).save(any());
        
        verifyNoMoreInteractions(this.internalEventDrivenJobDao);
    }

    @Test
    public void test_getCommandExecutionJobsForContext() {
        String contextName = "testContext";
        List<HibernateSchedulerJobRecord> records = new ArrayList<>();
        SearchResults<HibernateSchedulerJobRecord> searchResults = new SearchResultsImpl<>(records, 0L, 0L);

        when(schedulerJobDao.findByContextAndType(eq(contextName), eq(JobConstants.INTERNAL_EVENT_DRIVEN_JOB),
                eq(-1), eq(-1))).thenReturn(searchResults);

        Map<String, InternalEventDrivenJob> result = service.getCommandExecutionJobsForContext(contextName);

        Assert.assertNotNull(result);
        verify(schedulerJobDao).findByContextAndType(contextName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB, -1, -1);
    }

    // Global Event Job Tests

    @Test
    public void test_skip_globalEventJob() {
        GlobalEventJob job = createGlobalEventJob(JobConstants.GLOBAL_EVENT, "globalJob", "context");
        job.setChildContextNames(Arrays.asList("child1", "child2"));

        HibernateGlobalEventJobRecord record = new HibernateGlobalEventJobRecord();
        record.setGlobalEventJob(job);
        record.setTimestamp(System.currentTimeMillis());

        List<String> childContexts = Arrays.asList("child1", "child2");

        service.skip(record, childContexts, "tester");

        verify(globalEventJobDao).skip(any(HibernateGlobalEventJobRecord.class),
                eq(childContexts), eq("tester"));
    }

    @Test
    public void test_enable_globalEventJob() {
        GlobalEventJob job = createGlobalEventJob(JobConstants.GLOBAL_EVENT, "globalJob", "context");
        job.setSkippedContexts(new HashMap<>());
        job.getSkippedContexts().put("childToEnable", true);

        HibernateGlobalEventJobRecord record = new HibernateGlobalEventJobRecord();
        record.setGlobalEventJob(job);
        record.setTimestamp(System.currentTimeMillis());

        service.enable(record, "childToEnable", "tester");

        verify(globalEventJobDao).enable(any(HibernateGlobalEventJobRecord.class), eq("tester"));
    }

    // Helper methods to create job instances

    private InternalEventDrivenJob createInternalEventDrivenJob(String agentName, String jobName,
                                                                  String contextName, String commandLine) {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setCommandLine(commandLine);
        job.setSkippedContexts(new HashMap<>());
        job.setHeldContexts(new HashMap<>());
        return job;
    }

    private FileEventDrivenJob createFileEventDrivenJob(String agentName, String jobName,
                                                         String contextName, String filePath) {
        FileEventDrivenJob job = new FileEventDrivenJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setFilePath(filePath);
        job.setSkippedContexts(new HashMap<>());
        return job;
    }

    private QuartzScheduleDrivenJob createQuartzScheduleDrivenJob(String agentName, String jobName,
                                                                    String contextName, String cronExpression) {
        QuartzScheduleDrivenJob job = new QuartzScheduleDrivenJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setCronExpression(cronExpression);
        job.setSkippedContexts(new HashMap<>());
        return job;
    }

    private GlobalEventJob createGlobalEventJob(String agentName, String jobName, String contextName) {
        GlobalEventJob job = new GlobalEventJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setSkippedContexts(new HashMap<>());
        return job;
    }

    private ContextStartJob createContextStartJob(String agentName, String jobName, String contextName) {
        ContextStartJob job = new ContextStartJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setSkippedContexts(new HashMap<>());
        return job;
    }

    private ContextTerminalJob createContextTerminalJob(String agentName, String jobName, String contextName) {
        ContextTerminalJob job = new ContextTerminalJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setIdentifier(agentName + "_" + jobName);
        job.setContextName(contextName);
        job.setSkippedContexts(new HashMap<>());
        return job;
    }
}
