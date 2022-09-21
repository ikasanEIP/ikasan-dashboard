package org.ikasan.job.orchestration.context.cache;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.builder.context.JobLockBuilder;
import org.ikasan.job.orchestration.builder.job.SchedulerJobBuilder;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
        JobLockCache jlc = JobLockCacheImpl.instance();
        ReflectionTestUtils.setField(jlc, "jobLockCacheService", null);
        when(jobLockCacheService.get()).thenReturn(null);
        jlc.setJobLockCacheService(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-SAVE", 3, 1)));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        JobLockCacheRecord actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        JobLockCacheRecordImpl expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData"));
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);
        doNothing().when(jobLockCacheService).save(captor.capture());

        String contextId = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-SAVE-JobName0", contextId));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData"));
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        assertTrue(jlc.release("AgentName0-TEST-LOCK-SAVE-JobName0", contextId));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData"));
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);
    }

    @Test
    public void reset() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 2, 1)));

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertNotNull(jobLocksByLockName.get("TEST-LOCK"));
        assertNotNull(jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0")));
        assertNotNull(jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1")));

        jlc.reset();

        assertNull(jobLocksByLockName.get("TEST-LOCK"));
        assertNull(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertNull(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1"));
    }

    @Test
    public void resetLock() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3), makeJobLock("TEST-LOCK-1", 2, 2)));

        String contextId00 = UUID.randomUUID().toString();
        String contextId01 = UUID.randomUUID().toString();
        String contextId02 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02));

        String contextId10 = UUID.randomUUID().toString();
        String contextId11 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName"));

        // reset TEST-LOCK
        assertTrue(jlc.resetLock("TEST-LOCK"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName"));

        // reset TEST-LOCK-1
        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent);

        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName"));

        assertTrue(jlc.resetLock("TEST-LOCK-1"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName"));


        // confirm queue jobs are reset too
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent);

        assertTrue(jlc.resetLock("TEST-LOCK"));

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName"));

        // should not fail null or unknown
        assertFalse(jlc.resetLock(null));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6)));
    }

    @Test
    public void JobLockCache_lock_shouldNotGoAboveExistingLockCount_release_shouldNotGoBelowExistingLockCount() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        // 3 jobs lock count 2
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)));

        assertFalse(jlc.locked(null, "contextName"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString()));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null));

        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0));

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName"));

        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2));
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, jobLockHolder.getLockHolders().size());

        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertFalse(jlc.release(null, contextId2));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2));
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, jobLockHolder.getLockHolders().size());

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2));
    }

    @Test
    public void test_job_participates_in_lock() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)));

        assertFalse(jlc.doesJobParticipateInLock("jobIdentifier", "contextName"));
        assertTrue(jlc.doesJobParticipateInLock("AgentName2-TEST-LOCK-JobName2", "contextName2"));
    }

    @Test
    public void test_should_only_create_job_lock_cache_once() {
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
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(null);

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLock_ToJobLockCache_NewLock() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)));

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(1, jobLocksByLockName.size());
        JobLockHolder jobLockHolder = jobLocksByLockName.get("TEST-LOCK");
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK");

        assertEquals(3, jobLocksByIdentifier.size());
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1"));
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals("TEST-LOCK", jobLockHolder.getLockName());
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK");
    }

    @Test
    public void shouldAddNewJobs_AddLock_DifferentLocks() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-3", 3, 3)));
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-4", 4, 4)));

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(2, jobLocksByLockName.size());

        JobLockHolder jobLockHolder = jobLocksByLockName.get("TEST-LOCK-3");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get("TEST-LOCK-4");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");

        validateJobLocksByIdentifier(jobLocksByIdentifier, jobLocksByLockName);
    }

    @Test
    public void shouldAddNewJobs_AddLocks_DifferentLocks() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-3", 3, 3);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-4", 4, 4);
        jlc.addLocks(List.of(jobLock1, jobLock2));

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(2, jobLocksByLockName.size());

        JobLockHolder jobLockHolder = jobLocksByLockName.get("TEST-LOCK-3");
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get("TEST-LOCK-4");
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");


        validateJobLocksByIdentifier(jobLocksByIdentifier, jobLocksByLockName);
    }

    @Test
    public void shouldAddNewJobs_AddLock_ToJobLockCache_ExistingLock() {
        JobLockCache jlc1 = JobLockCacheImpl.instance();
        jlc1.setJobLockCacheService(jobLockCacheService);

        JobLockCache jlc2 = JobLockCacheImpl.instance();
        jlc2.setJobLockCacheService(jobLockCacheService);

        jlc1.addLocks(List.of(makeJobLock("TEST-LOCK-1", 3, 1)));
        jlc2.addLocks(List.of(makeJobLock("TEST-LOCK-1", 2, 1, "New")));

        validateJobLock(jlc1);
        validateJobLock(jlc2);

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc1, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLocks_ToJobLockCache_ExistingLock() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-1", 3, 1);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-1", 2, 1, "New");
        jlc.addLocks(List.of(jobLock1, jobLock2));

        validateJobLock(jlc);

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldNotNPEAddNewJobs_AddLocks_ToJobLockCache() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(null);

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void addNewJobs_AddLocks_ToJobLockCache_EmptyList() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(Collections.emptyList());

        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    private void validateJobLocksByIdentifier(ConcurrentHashMap<String, String> jobLocksByIdentifier, ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName) {

        JobLockHolder jobLockHolder;
        assertEquals(7, jobLocksByIdentifier.size());
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-3-JobName0"));
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-3-JobName1"));
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-3-JobName2"));
        assertEquals("TEST-LOCK-3", jobLockHolder.getLockName());
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-3");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-4-JobName0"));
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-4-JobName1"));
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-4-JobName2"));
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName3-TEST-LOCK-4-JobName3"));
        assertEquals("TEST-LOCK-4", jobLockHolder.getLockName());
        assertEquals(4, jobLockHolder.getLockCount());
        assertEquals(4, jobLockHolder.getSchedulerJobs().size());
        validate(jobLockHolder.getSchedulerJobs().values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList()), "TEST-LOCK-4");
    }

    private void validateJobLock(JobLockCache jlc) {
        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(jlc, "jobLockCacheData");

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(1, jobLocksByLockName.size());
        JobLockHolder jobLockHolder = jobLocksByLockName.get("TEST-LOCK-1");
        assertEquals("TEST-LOCK-1", jobLockHolder.getLockName());
        assertEquals(1, jobLockHolder.getLockCount());
        List<SchedulerJob> schedulerJobs = jobLockHolder.getSchedulerJobs()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        assertEquals(5, schedulerJobs.size());

        Map<String, SchedulerJob> jobMap = this.listToMap(schedulerJobs);

        SchedulerJob schedulerJob = jobMap.get("TEST-LOCK-1-JobName0");
        assertEquals("TEST-LOCK-1-JobName0", schedulerJob.getJobName());
        assertEquals("AgentName0", schedulerJob.getAgentName());
        assertEquals("Job0 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName0-TEST-LOCK-1-JobName0", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = jobMap.get("TEST-LOCK-1-JobName1");
        assertEquals("TEST-LOCK-1-JobName1", schedulerJob.getJobName());
        assertEquals("AgentName1", schedulerJob.getAgentName());
        assertEquals("Job1 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName1-TEST-LOCK-1-JobName1", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = jobMap.get("TEST-LOCK-1-JobName2");
        assertEquals("TEST-LOCK-1-JobName2", schedulerJob.getJobName());
        assertEquals("AgentName2", schedulerJob.getAgentName());
        assertEquals("Job2 Description", schedulerJob.getJobDescription());
        assertEquals("AgentName2-TEST-LOCK-1-JobName2", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = jobMap.get("TEST-LOCK-1-JobName0New");
        assertEquals("TEST-LOCK-1-JobName0New", schedulerJob.getJobName());
        assertEquals("AgentName0New", schedulerJob.getAgentName());
        assertEquals("Job0New Description", schedulerJob.getJobDescription());
        assertEquals("AgentName0New-TEST-LOCK-1-JobName0New", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());

        schedulerJob = jobMap.get("TEST-LOCK-1-JobName1New");
        assertEquals("TEST-LOCK-1-JobName1New", schedulerJob.getJobName());
        assertEquals("AgentName1New", schedulerJob.getAgentName());
        assertEquals("Job1New Description", schedulerJob.getJobDescription());
        assertEquals("AgentName1New-TEST-LOCK-1-JobName1New", schedulerJob.getIdentifier());
        assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());
    }

    private void validate(List<SchedulerJob> schedulerJobs, String jobLockName) {
        Map<String, SchedulerJob> jobMap = this.listToMap(schedulerJobs);
        for (int i = 0; i < schedulerJobs.size(); i++) {
            SchedulerJob schedulerJob = jobMap.get(jobLockName + "-" + "JobName" + i);
            assertEquals(jobLockName + "-" + "JobName" + i, schedulerJob.getJobName());
            assertEquals("AgentName" + i, schedulerJob.getAgentName());
            assertEquals("Job" + i + " Description", schedulerJob.getJobDescription());
            assertEquals("AgentName" + i + "-" + jobLockName + "-" + "JobName" + i, schedulerJob.getIdentifier());
            assertEquals(schedulerJob.getAgentName() + "-" + schedulerJob.getJobName(), schedulerJob.getIdentifier());
        }
    }

    private Map<String, SchedulerJob> listToMap(List<SchedulerJob> schedulerJobs) {
        return schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity()));
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
            job.setContextName(UUID.randomUUID().toString());
            jobLockBuilder.withJob("contextName"+i, job);
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
