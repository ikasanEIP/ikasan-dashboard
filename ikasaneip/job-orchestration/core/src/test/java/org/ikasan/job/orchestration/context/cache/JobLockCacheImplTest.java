package org.ikasan.job.orchestration.context.cache;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.builder.context.JobLockBuilder;
import org.ikasan.job.orchestration.builder.job.SchedulerJobBuilder;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheImplTest {

    @Mock
    private JobLockCacheService jobLockCacheService;

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
    }

    @Test
    public void shouldCallSaveWhenAddingLocksOrLockHolderIsAddedOrRemoved() {
        ArgumentCaptor<JobLockCacheRecord> captor = ArgumentCaptor.forClass(JobLockCacheRecord.class);
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        ReflectionTestUtils.setField(jlc, "jobLockCacheService", null);
        jlc.setJobLockCacheService(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-SAVE", 3, 1)));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        JobLockCacheRecord actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        JobLockCacheRecordImpl expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache(jlc);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);
        doNothing().when(jobLockCacheService).save(captor.capture());

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-SAVE-JobName0"));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache(jlc);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        assertTrue(jlc.release("AgentName0-TEST-LOCK-SAVE-JobName0"));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache(jlc);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);
    }

    @Test
    public void getJobsForIdentifier() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1), makeJobLock("TEST-LOCK-1", 1, 1)));

        assertTrue(jlc.getJobsForIdentifier(null).isEmpty());
        assertTrue(jlc.getJobsForIdentifier("unknown").isEmpty());

        List<SchedulerJob> jobsForIdentifier = jlc.getJobsForIdentifier("AgentName0-TEST-LOCK-JobName0");
        assertEquals(3, jobsForIdentifier.size());
        SchedulerJob schedulerJob = jobsForIdentifier.get(0);
        assertEquals("AgentName0-TEST-LOCK-JobName0", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(1);
        assertEquals("AgentName1-TEST-LOCK-JobName1", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(2);
        assertEquals("AgentName2-TEST-LOCK-JobName2", schedulerJob.getIdentifier());

        jobsForIdentifier = jlc.getJobsForIdentifier("AgentName1-TEST-LOCK-JobName1");
        assertEquals(3, jobsForIdentifier.size());
        schedulerJob = jobsForIdentifier.get(0);
        assertEquals("AgentName0-TEST-LOCK-JobName0", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(1);
        assertEquals("AgentName1-TEST-LOCK-JobName1", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(2);
        assertEquals("AgentName2-TEST-LOCK-JobName2", schedulerJob.getIdentifier());

        jobsForIdentifier = jlc.getJobsForIdentifier("AgentName2-TEST-LOCK-JobName2");
        assertEquals(3, jobsForIdentifier.size());
        schedulerJob = jobsForIdentifier.get(0);
        assertEquals("AgentName0-TEST-LOCK-JobName0", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(1);
        assertEquals("AgentName1-TEST-LOCK-JobName1", schedulerJob.getIdentifier());
        schedulerJob = jobsForIdentifier.get(2);
        assertEquals("AgentName2-TEST-LOCK-JobName2", schedulerJob.getIdentifier());

        jobsForIdentifier = jlc.getJobsForIdentifier("AgentName0-TEST-LOCK-1-JobName0");
        assertEquals(1, jobsForIdentifier.size());
        schedulerJob = jobsForIdentifier.get(0);
        assertEquals("AgentName0-TEST-LOCK-1-JobName0", schedulerJob.getIdentifier());
    }

    @Test
    public void exists() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLock(makeJobLock("TEST-LOCK", 2, 1));

        assertFalse(jlc.existsByJobLockName(null));
        assertTrue(jlc.existsByJobLockName("TEST-LOCK"));

        assertFalse(jlc.existsByIdentifier(null));
        assertTrue(jlc.existsByIdentifier("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.existsByIdentifier("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.existsByIdentifier("AgentName2-TEST-LOCK-JobName2"));
    }

    @Test
    public void reset() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLock(makeJobLock("TEST-LOCK", 2, 1));
        assertTrue(jlc.existsByJobLockName("TEST-LOCK"));
        assertTrue(jlc.existsByIdentifier("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.existsByIdentifier("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.existsByIdentifier("AgentName2-TEST-LOCK-JobName2"));

        jlc.reset();

        assertFalse(jlc.existsByJobLockName("TEST-LOCK"));
        assertFalse(jlc.existsByIdentifier("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.existsByIdentifier("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.existsByIdentifier("AgentName2-TEST-LOCK-JobName2"));
    }

    @Test
    public void resetLock() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3), makeJobLock("TEST-LOCK-1", 2, 2)));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1"));

        // reset TEST-LOCK
        assertTrue(jlc.resetLock("TEST-LOCK"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1"));

        // reset TEST-LOCK-1
        assertTrue(jlc.resetLock("TEST-LOCK-1"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1"));

        // should not fail null or unknown
        assertFalse(jlc.resetLock(null));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6)));
    }

    @Test
    public void JobLockCache_lock_shouldNotGoAboveExistingLockCount_release_shouldNotGoBelowExistingLockCount() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        // 3 jobs lock count 2
        jlc.addLock(makeJobLock("TEST-LOCK", 3, 2));

        assertFalse(jlc.locked(null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));

        assertFalse(jlc.lock(null));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0"));

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        JobLockHolderImpl jobLockHolder = jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0");
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2"));

        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2"));
        jobLockHolder = jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2");
        assertEquals(2, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2"));

        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertFalse(jlc.release(null));
        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2"));
        jobLockHolder = jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2");
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0"));

        jobLockHolder = jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0");
        assertEquals(0, jobLockHolder.getLockHolders().size());

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));
    }

    @Test
    public void JobLockCache_isLocked_lock_and_release() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);

        assertFalse(jlc.locked("jobIdentifier"));

        // 3 jobs one lock count
        jlc.addLock(makeJobLock("TEST-LOCK", 3, 1));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2"));
    }

    @Test
    public void shouldOnlyCreate_JobLockCache_Once() {
        JobLockCache jobLockCache1 = JobLockCacheImpl.instance();
        jobLockCache1.setJobLockCacheService(jobLockCacheService);
        JobLockCache jobLockCache2 = JobLockCacheImpl.instance();
        jobLockCache2.setJobLockCacheService(jobLockCacheService);

        assertNotNull(jobLockCache1);
        assertNotNull(jobLockCache2);
        assertSame(jobLockCache1, jobLockCache2);
    }

    @Test
    public void shouldNotNPEAddNewJobs_AddLock_ToJobLockCache_NewLockIsNull() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLock(null);

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLock_ToJobLockCache_NewLock() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLock(makeJobLock("TEST-LOCK", 3, 2));

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");

        assertEquals(1, jobLocksByLockName.size());
        JobLockHolderImpl jobLockHolder = jobLocksByLockName.get("TEST-LOCK");
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK");

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        assertEquals(3, jobLocksByIdentifier.size());
        jobLockHolder = jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0");
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK");

        jobLockHolder = jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1");
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK");

        jobLockHolder = jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2");
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK");
    }

    @Test
    public void shouldAddNewJobs_AddLock_DifferentLocks() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLock(makeJobLock("TEST-LOCK-3", 3, 3));
        jlc.addLock(makeJobLock("TEST-LOCK-4", 4, 4));

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");

        assertEquals(2, jobLocksByLockName.size());

        JobLockHolderImpl jobLockHolder = jobLocksByLockName.get("TEST-LOCK-3");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get("TEST-LOCK-4");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        validateJobLocksByIdentifier(jobLocksByIdentifier);
    }

    @Test
    public void shouldAddNewJobs_AddLocks_DifferentLocks() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-3", 3, 3);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-4", 4, 4);
        jlc.addLocks(List.of(jobLock1, jobLock2));

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");

        assertEquals(2, jobLocksByLockName.size());

        JobLockHolderImpl jobLockHolder = jobLocksByLockName.get("TEST-LOCK-3");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get("TEST-LOCK-4");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        validateJobLocksByIdentifier(jobLocksByIdentifier);
    }

    @Test
    public void shouldAddNewJobs_AddLock_ToJobLockCache_ExistingLock() {
        JobLockCache jlc1 = JobLockCacheImpl.instance();
        jlc1.setJobLockCacheService(jobLockCacheService);

        JobLockCache jlc2 = JobLockCacheImpl.instance();
        jlc2.setJobLockCacheService(jobLockCacheService);

        jlc1.addLock(makeJobLock("TEST-LOCK-1", 3, 1));
        jlc2.addLock(makeJobLock("TEST-LOCK-1", 2, 1, "New"));

        validateJobLock(jlc1);
        validateJobLock(jlc2);

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc1, "jobLocksByIdentifier");

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLocks_ToJobLockCache_ExistingLock() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-1", 3, 1);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-1", 2, 1, "New");
        jlc.addLocks(List.of(jobLock1, jobLock2));

        validateJobLock(jlc);

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldNotNPEAddNewJobs_AddLocks_ToJobLockCache() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(null);

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");
        assertEquals(0, jobLocksByLockName.size());

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void addNewJobs_AddLocks_ToJobLockCache_EmptyList() {
        JobLockCacheImpl jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(Collections.emptyList());

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");
        assertEquals(0, jobLocksByLockName.size());

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByIdentifier");
        assertEquals(0, jobLocksByIdentifier.size());
    }

    private void validateJobLocksByIdentifier(ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier) {
        JobLockHolderImpl jobLockHolder;
        assertEquals(7, jobLocksByIdentifier.size());
        jobLockHolder = jobLocksByIdentifier.get("AgentName0-TEST-LOCK-3-JobName0");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-3");

        jobLockHolder = jobLocksByIdentifier.get("AgentName1-TEST-LOCK-3-JobName1");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-3");

        jobLockHolder = jobLocksByIdentifier.get("AgentName2-TEST-LOCK-3-JobName2");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-3");

        jobLockHolder = jobLocksByIdentifier.get("AgentName0-TEST-LOCK-4-JobName0");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");

        jobLockHolder = jobLocksByIdentifier.get("AgentName1-TEST-LOCK-4-JobName1");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");

        jobLockHolder = jobLocksByIdentifier.get("AgentName2-TEST-LOCK-4-JobName2");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");

        jobLockHolder = jobLocksByIdentifier.get("AgentName3-TEST-LOCK-4-JobName3");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs(), "TEST-LOCK-4");
    }

    private void validateJobLock(JobLockCache jlc) {
        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(jlc, "jobLocksByLockName");

        assertEquals(1, jobLocksByLockName.size());
        JobLockHolderImpl jobLockHolder = jobLocksByLockName.get("TEST-LOCK-1");
        assertEquals("TEST-LOCK-1", jobLockHolder.getLockName());
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(5, jobLockHolder.getSchedulerJobs().size());
        List<SchedulerJob> schedulerJobs = jobLockHolder.getSchedulerJobs();

        SchedulerJob schedulerJob = schedulerJobs.get(0);
        assertEquals("TEST-LOCK-1-JobName0", schedulerJob.getJobName());
        assertEquals("AgentName0", schedulerJob.getAgentName());
        assertEquals("Job0 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName0-TEST-LOCK-1-JobName0", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = schedulerJobs.get(1);
        assertEquals("TEST-LOCK-1-JobName1", schedulerJob.getJobName());
        assertEquals("AgentName1", schedulerJob.getAgentName());
        assertEquals("Job1 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName1-TEST-LOCK-1-JobName1", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = schedulerJobs.get(2);
        assertEquals("TEST-LOCK-1-JobName2", schedulerJob.getJobName());
        assertEquals("AgentName2", schedulerJob.getAgentName());
        assertEquals("Job2 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName2-TEST-LOCK-1-JobName2", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = schedulerJobs.get(3);
        assertEquals("TEST-LOCK-1-JobName0New", schedulerJob.getJobName());
        assertEquals("AgentName0New", schedulerJob.getAgentName());
        assertEquals("Job0New Description", schedulerJob.getJobDescription());
        assertEquals("AgentName0New-TEST-LOCK-1-JobName0New", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = schedulerJobs.get(4);
        assertEquals("TEST-LOCK-1-JobName1New", schedulerJob.getJobName());
        assertEquals("AgentName1New", schedulerJob.getAgentName());
        assertEquals("Job1New Description", schedulerJob.getJobDescription());
        assertEquals("AgentName1New-TEST-LOCK-1-JobName1New", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());
    }

    private void validate(List<SchedulerJob> schedulerJobs, String jobLockName) {
        for (int i = 0; i < schedulerJobs.size(); i++) {
            SchedulerJob schedulerJob = schedulerJobs.get(i);
            assertEquals(jobLockName + "-" + "JobName" + i, schedulerJob.getJobName());
            assertEquals("AgentName" + i, schedulerJob.getAgentName());
            assertEquals("Job" + i + " Description", schedulerJob.getJobDescription());
            assertEquals("AgentName" + i + "-" + jobLockName + "-" + "JobName" + i, schedulerJob.getIdentifier());
            assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());
        }
    }

    private JobLock makeJobLock(String jobLockName, int count, long jobLockCount) {
        return makeJobLock(jobLockName, count, jobLockCount, null);
    }

    private JobLock makeJobLock(String jobLockName, int count, long jobLockCount, String newOrNot) {
        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName(jobLockName);
        jobLockBuilder.withLockCount(jobLockCount);
        for (int i = 0; i < count; i++) {
            SchedulerJob job = makeSchedulerJob(i, newOrNot, jobLockName);
            jobLockBuilder.withJob(job);
        }
        return jobLockBuilder.build().get(0);
    }

    private SchedulerJob makeSchedulerJob(int count, String newOrNot, String jobLockName) {
        String extra = newOrNot == null ? "" : newOrNot;
        return new SchedulerJobBuilder()
            .withJobName(jobLockName + "-" + "JobName" + count + extra)
            .withAgentName("AgentName" + count + extra)
            .withDescription("Job" + count + extra + " Description")
            .build();
    }
}
