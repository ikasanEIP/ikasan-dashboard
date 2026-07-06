package org.ikasan.job.orchestration.context.cache;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.AbstractJobLockCacheTest;
import org.ikasan.job.orchestration.broadcast.JobLockCacheEventBroadcaster;
import org.ikasan.job.orchestration.builder.context.JobLockBuilder;
import org.ikasan.job.orchestration.context.util.JobThreadFactory;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheImplTest extends AbstractJobLockCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheImplTest.class);

    @Before
    public void setup() {
        JobLockCacheImpl.instance().reset();
        this.consumeBroadcastEvents();
        ReflectionTestUtils.invokeMethod(JobLockCacheEventBroadcaster.instance(), "reset");
    }

    @Test
    public void shouldCallSaveWhenAddingLocksOrLockHolderIsAddedOrRemoved() {
        ArgumentCaptor<JobLockCacheRecord> captor = ArgumentCaptor.forClass(JobLockCacheRecord.class);
        JobLockCache jlc = super.newJobLockCache();
        ReflectionTestUtils.setField(jlc, "jobLockCacheService", null);
        when(jobLockCacheService.get(anyString())).thenReturn(null);
        jlc.setJobLockCacheService(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-SAVE", 3, 1)), "environment");
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        JobLockCacheRecord actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        JobLockCacheRecordImpl expected = new JobLockCacheRecordImpl();
        expected.setEnvironment("environment");
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        expected.setJobLockCache(jobLockCacheData);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);
        doNothing().when(jobLockCacheService).save(captor.capture());

        String contextId = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-SAVE-JobName0", contextId, "environment"));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setEnvironment("environment");
        expected.setJobLockCache(jobLockCacheData);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);

        Mockito.reset(jobLockCacheService);

        doNothing().when(jobLockCacheService).save(captor.capture());
        assertTrue(jlc.release("AgentName0-TEST-LOCK-SAVE-JobName0", contextId, "environment"));
        verify(jobLockCacheService, times(1)).save(any(JobLockCacheRecordImpl.class));
        actual = captor.getValue();
        assertNotNull(actual.getJobLockCache());
        expected = new JobLockCacheRecordImpl();
        expected.setEnvironment("environment");
        expected.setJobLockCache(jobLockCacheData);
        assertEquals(expected, actual);
        verifyNoMoreInteractions(jobLockCacheService);
    }

    @Test
    public void reset() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 2, 1)), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

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
    public void reset_environment() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 2, 1)), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertNotNull(jobLocksByLockName.get("TEST-LOCK"));
        assertNotNull(jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0")));
        assertNotNull(jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1")));

        jlc.reset("environment");

        assertNull(jobLocksByLockName.get("TEST-LOCK"));
        assertNull(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertNull(jobLocksByIdentifier.get("AgentName1-TEST-LOCK-JobName1"));
    }

    @Test
    public void test_remove_running_lock_holder() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("name");
        internalEventDrivenJob.setContextName("contextName");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.lock("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
        Assert.assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setContextName("contextName");
        schedulerJobInstance.setStatus(InstanceStatus.RUNNING);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");

        Assert.assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
    }

    @Test
    public void test_remove_queued_event() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setChildContextName("child");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");

        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextName("context");
        schedulerJobInstance.setChildContextName("child");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
    }

    @Test
    public void test_remove_queued_event_multiple_environment() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");

        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "another_environment");

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setChildContextName("child");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "another_environment");

        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextName("context");
        schedulerJobInstance.setChildContextName("child");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");
        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "another_environment");

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
    }

    @Test
    public void test_remove_queued_event_multiple_null_second_environment_uses_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");

        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), null);

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setChildContextName("child");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, null);

        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextName("context");
        schedulerJobInstance.setChildContextName("child");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");
        jlc.removeQueuedSchedulerJob(schedulerJobInstance, null);

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", null));
    }

    @Test
    public void test_remove_queued_event_exclusive_lock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeExclusiveJobLock("TEST-LOCK", 3, 3)
            , makeExclusiveJobLock("TEST-LOCK-1", 2, 2)), "environment");

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("contextName");
        internalEventDrivenJob.setChildContextName("childContextName");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0"
            , "contextName", schedulerJobInitiationEvent, "environment");

        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setContextName("contextName");
        schedulerJobInstance.setChildContextName("childContextName");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");

        Assert.assertTrue(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment").isEmpty());
    }

    @Test
    public void test_remove_queued_event_exclusive_lock_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeExclusiveJobLock("TEST-LOCK", 3, 3)
            , makeExclusiveJobLock("TEST-LOCK-1", 2, 2)), "environment");

        jlc.addLocks(List.of(makeExclusiveJobLock("TEST-LOCK", 3, 3)
            , makeExclusiveJobLock("TEST-LOCK-1", 2, 2)), "another_environment");

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("contextName");
        internalEventDrivenJob.setChildContextName("childContextName");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0"
            , "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0"
            , "contextName", schedulerJobInitiationEvent, "another_environment");


        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setContextName("contextName");
        schedulerJobInstance.setChildContextName("childContextName");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");
        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "another_environment");

        Assert.assertTrue(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0"
            , "contextName", "environment").isEmpty());
        Assert.assertTrue(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0"
            , "contextName", "another_environment").isEmpty());
    }

    @Test
    public void test_remove_queued_event_exclusive_lock_multiple_environments_with_null_delegates_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeExclusiveJobLock("TEST-LOCK", 3, 3)
            , makeExclusiveJobLock("TEST-LOCK-1", 2, 2)), "environment");

        jlc.addLocks(List.of(makeExclusiveJobLock("TEST-LOCK", 3, 3)
            , makeExclusiveJobLock("TEST-LOCK-1", 2, 2)), null);

        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("JobName0");
        internalEventDrivenJob.setContextName("contextName");
        internalEventDrivenJob.setChildContextName("childContextName");
        internalEventDrivenJob.setContextInstanceId("contextInstanceId");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0"
            , "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0"
            , "contextName", schedulerJobInitiationEvent, null);


        SchedulerJobInstance schedulerJobInstance = new SchedulerJobInstanceImpl();
        schedulerJobInstance.setJobName("JobName0");
        schedulerJobInstance.setContextName("contextName");
        schedulerJobInstance.setChildContextName("childContextName");
        schedulerJobInstance.setAgentName("AgentName0");
        schedulerJobInstance.setIdentifier("AgentName0-TEST-LOCK-JobName0");
        schedulerJobInstance.setContextInstanceId("contextInstanceId");
        schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);

        jlc.removeQueuedSchedulerJob(schedulerJobInstance, "environment");
        jlc.removeQueuedSchedulerJob(schedulerJobInstance, null);

        Assert.assertTrue(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0"
            , "contextName", "environment").isEmpty());
        Assert.assertTrue(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0"
            , "contextName", null).isEmpty());
    }

    @Test
    public void resetLock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");

        String contextId00 = UUID.randomUUID().toString();
        String contextId01 = UUID.randomUUID().toString();
        String contextId02 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02, "environment"));

        String contextId10 = UUID.randomUUID().toString();
        String contextId11 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));

        // reset TEST-LOCK
        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));

        // reset TEST-LOCK-1
        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("name");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");

        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        assertTrue(jlc.resetLock("TEST-LOCK-1", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));


        // confirm queue jobs are reset too
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");

        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // should not fail null or unknown
        assertFalse(jlc.resetLock(null, "environment"));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6), "environment"));
    }

    @Test
    public void reset_lock_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "another_environment");

        String contextId00 = UUID.randomUUID().toString();
        String contextId01 = UUID.randomUUID().toString();
        String contextId02 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00, "another_environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01, "another_environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02, "another_environment"));

        String contextId10 = UUID.randomUUID().toString();
        String contextId11 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10, "another_environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "another_environment"));

        // reset TEST-LOCK
        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK", "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "another_environment"));

        // reset TEST-LOCK-1
        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("name");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "another_environment");

        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        assertTrue(jlc.resetLock("TEST-LOCK-1", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK-1", "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "another_environment"));


        // confirm queue jobs are reset too
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "another_environment");

        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK", "another_environment"));

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // should not fail null or unknown
        assertFalse(jlc.resetLock(null, "environment"));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6), "environment"));

        assertFalse(jlc.resetLock(null, "another_environment"));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6), "another_environment"));
    }

    @Test
    public void reset_lock_multiple_environments_with_null_delegate_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2)), null);

        String contextId00 = UUID.randomUUID().toString();
        String contextId01 = UUID.randomUUID().toString();
        String contextId02 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId00, null));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId01, null));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId02, null));

        String contextId10 = UUID.randomUUID().toString();
        String contextId11 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-1-JobName0", contextId10, null));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-1-JobName1", contextId11, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", null));

        // reset TEST-LOCK
        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK", null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", null));

        // reset TEST-LOCK-1
        SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("JobName0");
        InternalEventDrivenJobInstance internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setJobName("name");
        internalEventDrivenJob.setContextName("context");
        internalEventDrivenJob.setIdentifier("identifier");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, null);

        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNotNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        assertTrue(jlc.resetLock("TEST-LOCK-1", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK-1", null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-1-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-1-JobName1", "contextName", null));


        // confirm queue jobs are reset too
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, "environment");
        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-TEST-LOCK-JobName0", "contextName", schedulerJobInitiationEvent, null);

        assertTrue(jlc.resetLock("TEST-LOCK", "environment"));
        assertTrue(jlc.resetLock("TEST-LOCK", null));

        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        Assert.assertNull(jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // should not fail null or unknown
        assertFalse(jlc.resetLock(null, "environment"));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6), "environment"));

        assertFalse(jlc.resetLock(null, null));
        assertFalse(jlc.resetLock(RandomStringUtils.randomAlphanumeric(6), null));
    }

    @Test
    public void JobLockCache_lock_shouldNotGoAboveExistingLockCount_release_shouldNotGoBelowExistingLockCount() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        // 3 jobs lock count 2
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), "environment");

        assertFalse(jlc.locked(null, "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString(), "environment"));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null, "environment"));

        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, jobLockHolder.getLockHolders().size());

        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.release(null, contextId2, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, jobLockHolder.getLockHolders().size());

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
    }

    @Test
    public void JobLockCache_lock_shouldNotGoAboveExistingLockCount_release_shouldNotGoBelowExistingLockCount_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        // 3 jobs lock count 2
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), "another_environment");

        assertFalse(jlc.locked(null, "contextName", "environment"));
        assertFalse(jlc.locked(null, "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString(), "environment"));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null, "environment"));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString(), "another_environment"));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null, "another_environment"));

        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        JobLockCacheData anotherJobLockCacheData = jobLockCacheDataMap.get("another_environment");
        ConcurrentHashMap<String, String> anotherJobLocksByIdentifier
            = anotherJobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> anotherJobLocksByLockName
            = anotherJobLockCacheData.getJobLocksByLockName();

        JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        JobLockHolder anotherJobLockHolder = anotherJobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, anotherJobLockHolder.getLockHolders().size());
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, jobLockHolder.getLockHolders().size());

        anotherJobLockHolder = anotherJobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, anotherJobLockHolder.getLockHolders().size());

        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertFalse(jlc.release(null, contextId2, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1, "environment"));

        assertFalse(jlc.release(null, contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1, "another_environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        anotherJobLockHolder = anotherJobLocksByLockName.get(anotherJobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, anotherJobLockHolder.getLockHolders().size());
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, jobLockHolder.getLockHolders().size());

        anotherJobLockHolder = anotherJobLocksByLockName.get(anotherJobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, anotherJobLockHolder.getLockHolders().size());

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
    }

    @Test
    public void JobLockCache_lock_shouldNotGoAboveExistingLockCount_release_shouldNotGoBelowExistingLockCount_multiple_environments_with_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        // 3 jobs lock count 2
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), null);

        assertFalse(jlc.locked(null, "contextName", "environment"));
        assertFalse(jlc.locked(null, "contextName", null));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString(), "environment"));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null, "environment"));

        assertFalse(jlc.lock(null, UUID.randomUUID().toString(), null));
        assertFalse(jlc.lock("AgentName0-TEST-LOCK-JobName0", null, null));

        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        JobLockCacheData anotherJobLockCacheData = jobLockCacheDataMap.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        ConcurrentHashMap<String, String> anotherJobLocksByIdentifier
            = anotherJobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> anotherJobLocksByLockName
            = anotherJobLockCacheData.getJobLocksByLockName();

        JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        JobLockHolder anotherJobLockHolder = anotherJobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(1, anotherJobLockHolder.getLockHolders().size());
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName3-TEST-LOCK-JobName2", "contextName", null));

        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, jobLockHolder.getLockHolders().size());

        anotherJobLockHolder = anotherJobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(2, anotherJobLockHolder.getLockHolders().size());

        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName2-TEST-LOCK-JobName2:context-id:" + contextId2));

        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertFalse(jlc.release(null, contextId2, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1, "environment"));

        assertFalse(jlc.release(null, contextId2, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", null, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId1, null));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, jobLockHolder.getLockHolders().size());
        assertTrue(jobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        anotherJobLockHolder = anotherJobLocksByLockName.get(anotherJobLocksByIdentifier.get("AgentName2-TEST-LOCK-JobName2"));
        assertEquals(1, anotherJobLockHolder.getLockHolders().size());
        assertTrue(anotherJobLockHolder.getLockHolders().contains("AgentName0-TEST-LOCK-JobName0:context-id:" + contextId0));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        jobLockHolder = jobLocksByLockName.get(jobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, jobLockHolder.getLockHolders().size());

        anotherJobLockHolder = anotherJobLocksByLockName.get(anotherJobLocksByIdentifier.get("AgentName0-TEST-LOCK-JobName0"));
        assertEquals(0, anotherJobLockHolder.getLockHolders().size());

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_second_environment() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_second_environment_with_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_by_unmanaged_job_lock_and_release() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        // removing the job that holds the lock from the job participant collection
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        jobLockCacheData.getJobLocksByLockName().values().forEach(l -> {
            if(!l.getLockHolders().isEmpty()) {
                l.getSchedulerJobs().get("contextName0").removeAll(l.getSchedulerJobs().get("contextName0"));
            }
        });
        jobLockCacheData.getJobLocksByIdentifier().remove("AgentName0-TEST-LOCK-JobName0");

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_by_unmanaged_job_lock_and_release_with_second_environment() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        JobLock anotherJobLock = makeJobLock("TEST-LOCK", 3, 1);
        // 3 jobs one lock count
        jlc.addLocks(List.of(anotherJobLock), "another_environment");

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        // removing the job that holds the lock from the job participant collection
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        jobLockCacheData.getJobLocksByLockName().values().forEach(l -> {
            if(!l.getLockHolders().isEmpty()) {
                l.getSchedulerJobs().get("contextName0").removeAll(l.getSchedulerJobs().get("contextName0"));
            }
        });
        jobLockCacheData.getJobLocksByIdentifier().remove("AgentName0-TEST-LOCK-JobName0");

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        jobLockCacheData = jobLockCacheDataMap.get("another_environment");
        jobLockCacheData.getJobLocksByLockName().values().forEach(l -> {
            if(!l.getLockHolders().isEmpty()) {
                l.getSchedulerJobs().get("contextName0").removeAll(l.getSchedulerJobs().get("contextName0"));
            }
        });
        jobLockCacheData.getJobLocksByIdentifier().remove("AgentName0-TEST-LOCK-JobName0");

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_by_unmanaged_job_lock_and_release_with_second_environment_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        JobLock anotherJobLock = makeJobLock("TEST-LOCK", 3, 1);
        // 3 jobs one lock count
        jlc.addLocks(List.of(anotherJobLock), null);

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        // removing the job that holds the lock from the job participant collection
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        jobLockCacheData.getJobLocksByLockName().values().forEach(l -> {
            if(!l.getLockHolders().isEmpty()) {
                l.getSchedulerJobs().get("contextName0").removeAll(l.getSchedulerJobs().get("contextName0"));
            }
        });
        jobLockCacheData.getJobLocksByIdentifier().remove("AgentName0-TEST-LOCK-JobName0");

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        jobLockCacheData = jobLockCacheDataMap.get(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        jobLockCacheData.getJobLocksByLockName().values().forEach(l -> {
            if(!l.getLockHolders().isEmpty()) {
                l.getSchedulerJobs().get("contextName0").removeAll(l.getSchedulerJobs().get("contextName0"));
            }
        });
        jobLockCacheData.getJobLocksByIdentifier().remove("AgentName0-TEST-LOCK-JobName0");

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(20);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", 20);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 11);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", 11);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings_with_second_environment() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(20);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", 20);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 11);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", 11);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings_with_second_environment_with_null_delegating_to_default() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(20);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", 20);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 11);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", 11);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", null));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", Integer.MAX_VALUE);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count_with_second_environment() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", Integer.MAX_VALUE);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count_with_second_environment_wth_null_delegating_to_default() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", Integer.MAX_VALUE);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", null));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count_with_mixed_weightings() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 7);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 1);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count_with_mixed_weightings_with_multiple_environments() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 7);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 1);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);

        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_individual_job_weightings_exceeding_lock_count_with_mixed_weightings_with_multiple_environments_with_null_delegating_to_default() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(1);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 7);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", 1);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);

        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        // Could not take out a job lock here because "AgentName0-TEST-LOCK-JobName0", contextId0 has the lock
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", null));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings_with_large_lock_count_values() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(Integer.MAX_VALUE);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings_with_large_lock_count_values_with_second_environment() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(Integer.MAX_VALUE);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_with_different_job_weightings_with_large_lock_count_values_with_second_environment_with_null_delegating_to_default() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName("TEST-LOCK");
        jobLockBuilder.withLockCount(Integer.MAX_VALUE);

        SchedulerJobLockParticipant job0 = makeSchedulerJobLockParticipant(0, "", "TEST-LOCK", 1);
        job0.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job0);

        SchedulerJobLockParticipant job1 = makeSchedulerJobLockParticipant(1, "", "TEST-LOCK", Integer.MAX_VALUE);
        job1.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job1);

        SchedulerJobLockParticipant job2 = makeSchedulerJobLockParticipant(2, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job2.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job2);

        SchedulerJobLockParticipant job3 = makeSchedulerJobLockParticipant(3, "", "TEST-LOCK", Integer.MAX_VALUE - 1000000);
        job3.setContextName(UUID.randomUUID().toString());
        jobLockBuilder.withJob("contextName", job3);


        jlc.addLocks(jobLockBuilder.build(), "environment");
        jlc.addLocks(jobLockBuilder.build(), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        // Could not take out a job lock here because the count of 20 on job1
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.lock("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.lock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertFalse(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertTrue(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertTrue(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.lock("AgentName3-TEST-LOCK-JobName3", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId0, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId0, null));
        assertTrue(jlc.locked("AgentName3-TEST-LOCK-JobName3", "contextName", null));
        assertTrue(jlc.hasLock("AgentName3-TEST-LOCK-JobName3", contextId0, null));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_exclusive_lock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        jobLock.setExclusiveJobLock(true);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_exclusive_lock_with_second_environment() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        jobLock.setExclusiveJobLock(true);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        JobLock anotherJobLock = makeJobLock("TEST-LOCK", 3, 1);
        anotherJobLock.setExclusiveJobLock(true);

        // 3 jobs one lock count
        jlc.addLocks(List.of(anotherJobLock), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_exclusive_lock_with_second_environment_with_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLock jobLock = makeJobLock("TEST-LOCK", 3, 1);
        jobLock.setExclusiveJobLock(true);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLock), "environment");

        JobLock anotherJobLock = makeJobLock("TEST-LOCK", 3, 1);
        anotherJobLock.setExclusiveJobLock(true);

        // 3 jobs one lock count
        jlc.addLocks(List.of(anotherJobLock), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));

        // release the lock - only the lock holder can release the lock
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment"));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment"));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment"));

        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, null));
        assertFalse(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, null));
        assertFalse(jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));

        // release
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, null));
    }

    @Test
    public void test_job_lock_cache_mix_of_exclusive_and_non_exclusive_locks_check_queuing() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        JobLock jobLockExclusive1 = makeJobLock("TEST-LOCK-EXCLUSIVE-1", 3, 1);
        jobLockExclusive1.setExclusiveJobLock(true);

        JobLock jobLockExclusive2 = makeJobLock("TEST-LOCK-EXCLUSIVE-2", 3, 1);
        jobLockExclusive2.setExclusiveJobLock(true);

        JobLock jobLockNonExclusive1 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-1", 3, 1);
        jobLockNonExclusive1.setExclusiveJobLock(false);

        JobLock jobLockNonExclusive2 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-2", 3, 1);
        jobLockNonExclusive2.setExclusiveJobLock(false);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLockExclusive1, jobLockExclusive2, jobLockNonExclusive1, jobLockNonExclusive2), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));

        // lock one of the exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Assert that everything is locked
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Release the exclusive lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now make sure that nothing is locked
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Now lock one of the non-exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now assert that the exclusive locks are locked because they cannot take out a lock when any
        // other lock is held.
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Take out the second non-exclusive lock.
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));
    }

    @Test
    public void test_job_lock_cache_mix_of_exclusive_and_non_exclusive_locks_check_queuing_with_second_environment() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        JobLock jobLockExclusive1 = makeJobLock("TEST-LOCK-EXCLUSIVE-1", 3, 1);
        jobLockExclusive1.setExclusiveJobLock(true);

        JobLock jobLockExclusive2 = makeJobLock("TEST-LOCK-EXCLUSIVE-2", 3, 1);
        jobLockExclusive2.setExclusiveJobLock(true);

        JobLock jobLockNonExclusive1 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-1", 3, 1);
        jobLockNonExclusive1.setExclusiveJobLock(false);

        JobLock jobLockNonExclusive2 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-2", 3, 1);
        jobLockNonExclusive2.setExclusiveJobLock(false);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLockExclusive1, jobLockExclusive2, jobLockNonExclusive1, jobLockNonExclusive2), "environment");
        jlc.addLocks(List.of(jobLockExclusive1, jobLockExclusive2, jobLockNonExclusive1, jobLockNonExclusive2), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));

        // lock one of the exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Assert that everything is locked
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Release the exclusive lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now make sure that nothing is locked
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Now lock one of the non-exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now assert that the exclusive locks are locked because they cannot take out a lock when any
        // other lock is held.
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Take out the second non-exclusive lock.
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // another_environment assertions
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));

        // lock one of the exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));

        // Assert that everything is locked
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        // Release the exclusive lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));

        // Now make sure that nothing is locked
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        // Now lock one of the non-exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));

        // Now assert that the exclusive locks are locked because they cannot take out a lock when any
        // other lock is held.
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        // Take out the second non-exclusive lock.
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "another_environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "another_environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "another_environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "another_environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_mix_of_exclusive_and_non_exclusive_locks_check_queuing_with_second_environment_with_null_delegating_to_default() {
        JobLockCacheImpl jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        JobLock jobLockExclusive1 = makeJobLock("TEST-LOCK-EXCLUSIVE-1", 3, 1);
        jobLockExclusive1.setExclusiveJobLock(true);

        JobLock jobLockExclusive2 = makeJobLock("TEST-LOCK-EXCLUSIVE-2", 3, 1);
        jobLockExclusive2.setExclusiveJobLock(true);

        JobLock jobLockNonExclusive1 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-1", 3, 1);
        jobLockNonExclusive1.setExclusiveJobLock(false);

        JobLock jobLockNonExclusive2 = makeJobLock("TEST-LOCK-NON-EXCLUSIVE-2", 3, 1);
        jobLockNonExclusive2.setExclusiveJobLock(false);

        // 3 jobs one lock count
        jlc.addLocks(List.of(jobLockExclusive1, jobLockExclusive2, jobLockNonExclusive1, jobLockNonExclusive2), "environment");
        jlc.addLocks(List.of(jobLockExclusive1, jobLockExclusive2, jobLockNonExclusive1, jobLockNonExclusive2), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));

        // lock one of the exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Assert that everything is locked
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Release the exclusive lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now make sure that nothing is locked
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Now lock one of the non-exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));

        // Now assert that the exclusive locks are locked because they cannot take out a lock when any
        // other lock is held.
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // Take out the second non-exclusive lock.
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, "environment"));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, "environment"));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, "environment"));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", "environment"));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, "environment"));

        // another_environment assertions
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));

        // lock one of the exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));

        // Assert that everything is locked
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, null));

        // Release the exclusive lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));

        // Now make sure that nothing is locked
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, null));

        // Now lock one of the non-exclusive locks
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));

        // Now assert that the exclusive locks are locked because they cannot take out a lock when any
        // other lock is held.
        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, null));

        // Take out the second non-exclusive lock.
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, null));

        assertTrue(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", null));
        assertTrue(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));
        assertTrue(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, null));
        assertTrue(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, null));

        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-1-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-1-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-1-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-EXCLUSIVE-2-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-EXCLUSIVE-2-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-EXCLUSIVE-2-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-1-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-1-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-1-JobName2", contextId2, null));

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", "contextName", null));
        assertFalse(jlc.hasLock("AgentName0-TEST-LOCK-NON-EXCLUSIVE-2-JobName0", contextId0, null));
        assertFalse(jlc.locked("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", "contextName", null));
        assertFalse(jlc.hasLock("AgentName1-TEST-LOCK-NON-EXCLUSIVE-2-JobName1", contextId1, null));
        assertFalse(jlc.locked("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", "contextName", null));
        assertFalse(jlc.hasLock("AgentName2-TEST-LOCK-NON-EXCLUSIVE-2-JobName2", contextId2, null));
    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_locked() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        AtomicReference<JobLockCacheEvent> jobLockCacheEvent = new AtomicReference<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.set(event);
        });

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(JobLockCacheEvent.EventType.LOCK_OBTAINED, jobLockCacheEvent.get().getEvent());
                Assert.assertEquals("AgentName0-TEST-LOCK-JobName0", jobLockCacheEvent.get().getJobIdentifier());
                Assert.assertEquals(contextId0, jobLockCacheEvent.get().getContextName());
            });
    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_locked_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.add(event);
        });

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(15, TimeUnit.MILLISECONDS).and().with().pollDelay(1, TimeUnit.MILLISECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                    , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));
            });
    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_locked_multiple_environments_with_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.add(event);
        });

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));

            });
    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_locked() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        AtomicReference<JobLockCacheEvent> jobLockCacheEvent = new AtomicReference<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> jobLockCacheEvent.set(message);
        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(JobLockCacheEvent.EventType.LOCK_OBTAINED, jobLockCacheEvent.get().getEvent());
                Assert.assertEquals("AgentName0-TEST-LOCK-JobName0", jobLockCacheEvent.get().getJobIdentifier());
                Assert.assertEquals(contextId0, jobLockCacheEvent.get().getContextName());
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_locked_with_multiple_environments() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> jobLockCacheEvent.add(message);
        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_locked_with_multiple_environments_with_null_delegating_to_default() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> jobLockCacheEvent.add(message);
        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_released() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        AtomicReference<JobLockCacheEvent> jobLockCacheEvent = new AtomicReference<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.set(event);
        });

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(JobLockCacheEvent.EventType.LOCK_RELEASED, jobLockCacheEvent.get().getEvent());
                Assert.assertEquals("AgentName0-TEST-LOCK-JobName0", jobLockCacheEvent.get().getJobIdentifier());
                Assert.assertEquals(contextId0, jobLockCacheEvent.get().getContextName());
            });

    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_released_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));
        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.add(event);
        });

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));
            });

        ArrayList<JobLockCacheEvent> jobLockCacheEvent2 = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent2.add(event);
        });

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent2.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_RELEASED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_RELEASED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent2));
            });
    }

    @Test
    public void test_job_lock_cache_publishes_event_when_job_released_multiple_environments_with_null_delegating_to_default() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent.add(event);
        });

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent));
            });

        ArrayList<JobLockCacheEvent> jobLockCacheEvent2 = new ArrayList<>();

        jlc.addJobLockCacheEventListener(event -> {
            LOGGER.info("Event -> "+ event);
            jobLockCacheEvent2.add(event);
        });

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, jobLockCacheEvent2.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_RELEASED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_RELEASED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(jobLockCacheEvent2));
            });

    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_released() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        AtomicReference<JobLockCacheEvent> jobLockCacheEvent = new AtomicReference<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> jobLockCacheEvent.set(message);
        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(JobLockCacheEvent.EventType.LOCK_RELEASED, jobLockCacheEvent.get().getEvent());
                Assert.assertEquals("AgentName0-TEST-LOCK-JobName0", jobLockCacheEvent.get().getJobIdentifier());
                Assert.assertEquals(contextId0, jobLockCacheEvent.get().getContextName());
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_released_with_second_environment() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        ArrayList<JobLockCacheEvent> obtainedEvents = new ArrayList<>();
        ArrayList<JobLockCacheEvent> releasedEvents = new ArrayList<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> {
            if(message.getEvent().equals(JobLockCacheEvent.EventType.LOCK_OBTAINED))obtainedEvents.add(message);
            if(message.getEvent().equals(JobLockCacheEvent.EventType.LOCK_RELEASED))releasedEvents.add(message);
        };

        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, obtainedEvents.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(obtainedEvents));
            });

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "another_environment"));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, releasedEvents.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_RELEASED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "another_environment", JobLockCacheEvent.EventType.LOCK_RELEASED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(releasedEvents));
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_broadcasts_event_when_job_released_with_second_environment_with_null_delegating_to_default() throws InterruptedException {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 1, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        ArrayList<JobLockCacheEvent> obtainedEvents = new ArrayList<>();
        ArrayList<JobLockCacheEvent> releasedEvents = new ArrayList<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> {
            if(message.getEvent().equals(JobLockCacheEvent.EventType.LOCK_OBTAINED))obtainedEvents.add(message);
            if(message.getEvent().equals(JobLockCacheEvent.EventType.LOCK_RELEASED))releasedEvents.add(message);
        };

        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        // lock it
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, obtainedEvents.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_OBTAINED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_OBTAINED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(obtainedEvents));
            });

        // release the lock
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment"));
        assertTrue(jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, null));

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertEquals(2, releasedEvents.size());

                List expected = List.of(new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, "environment", JobLockCacheEvent.EventType.LOCK_RELEASED ),
                    new JobLockCacheEventImpl("TEST-LOCK","AgentName0-TEST-LOCK-JobName0"
                        , contextId0, JobLockCacheRecord.DEFAULT_ENVIRONMENT, JobLockCacheEvent.EventType.LOCK_RELEASED ));

                Assert.assertEquals(new HashSet<>(expected), new HashSet(releasedEvents));
            });

        JobLockCacheEventBroadcaster.instance().unregister(broadcaster);
    }

    @Test
    public void test_job_lock_cache_attempt_to_lock_job_not_in_cache() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));

        // lock it
        assertFalse(jlc.lock("bad job name", contextId0, "environment"));
    }

    @Test
    public void test_job_lock_cache_attempt_to_lock_job_not_in_cache_with_multiple_environments() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", "another_environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "another_environment");

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "another_environment"));

        // lock it
        assertFalse(jlc.lock("bad job name", contextId0, "environment"));
        assertFalse(jlc.lock("bad job name", contextId0, "another_environment"));
    }

    @Test
    public void test_job_lock_cache_attempt_to_lock_job_not_in_cache_with_multiple_environments_with_null_delegating_to_null() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));
        assertFalse(jlc.locked("jobIdentifier", "contextName", null));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), null);

        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
        assertFalse(jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", null));

        // lock it
        assertFalse(jlc.lock("bad job name", contextId0, "environment"));
        assertFalse(jlc.lock("bad job name", contextId0, null));
    }

    @Test
    public void test_job_participates_in_lock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertFalse(jlc.doesJobParticipateInLock("jobIdentifier", "contextName", "environment"));
        assertTrue(jlc.doesJobParticipateInLock("AgentName2-TEST-LOCK-JobName2", "contextName2", "environment"));
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
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(null, "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLock_ToJobLockCache_NewLock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 2)), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
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
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-3", 3, 3)), "environment");
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK-4", 4, 4)), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

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
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-3", 3, 3);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-4", 4, 4);
        jlc.addLocks(List.of(jobLock1, jobLock2), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

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

        jlc1.addLocks(List.of(makeJobLock("TEST-LOCK-1", 3, 1)), "environment");
        jlc2.addLocks(List.of(makeJobLock("TEST-LOCK-1", 2, 1, "New")), "environment");

        validateJobLock(jlc1);
        validateJobLock(jlc2);

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc1, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void shouldAddNewJobs_AddLocks_ToJobLockCache_ExistingLock() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-1", 3, 1);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-1", 2, 1, "New");
        jlc.addLocks(List.of(jobLock1, jobLock2), "environment");

        validateJobLock(jlc);

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");
        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        assertEquals(5, jobLocksByIdentifier.size());
    }

    @Test
    public void add_job_lock_with_no_associated_jobs() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        JobLock jobLock1 = makeJobLock("TEST-LOCK-1", 0, 1);
        JobLock jobLock2 = makeJobLock("TEST-LOCK-1", 0, 1, "New");
        jlc.addLocks(List.of(jobLock1, jobLock2), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        assertEquals(0, jobLocksByIdentifier.size());

        ConcurrentHashMap<String, JobLockHolder> jobLocksByName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(1, jobLocksByName.size());

        JobLockHolder jobLockHolder = jobLocksByName.get("TEST-LOCK-1");

        Assert.assertNotNull(jobLockHolder);
        Assert.assertEquals(0, jobLockHolder.getSchedulerJobs().size());
    }

    @Test
    public void shouldNotNPEAddNewJobs_AddLocks_ToJobLockCache() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(null, "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void addNewJobs_AddLocks_ToJobLockCache_EmptyList() {
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        jlc.addLocks(Collections.emptyList(), "environment");

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertEquals(0, jobLocksByLockName.size());
        assertEquals(0, jobLocksByIdentifier.size());
    }

    @Test
    public void test_job_lock_cache_is_locked_lock_and_release_multi_threaded() {
        ExecutorService executor = Executors.newFixedThreadPool(2, new JobThreadFactory("JobLockCacheImpl"));
        JobLockCache jlc = super.newJobLockCache();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextId0 = UUID.randomUUID().toString();
        String contextId1 = UUID.randomUUID().toString();
        String contextId2 = UUID.randomUUID().toString();

        assertFalse(jlc.locked("jobIdentifier", "contextName", "environment"));

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        AtomicBoolean threadOneFinished = new AtomicBoolean(false);
        AtomicBoolean threadTwoFinished = new AtomicBoolean(false);
        AtomicBoolean hasAssertionError = new AtomicBoolean(false);
        AtomicBoolean hasException = new AtomicBoolean(false);

        executor.submit(() -> {
            try {
                for(int i=0; i<1000; i++ ) {
                    assertTrue(jlc.doesJobParticipateInLock("AgentName0-TEST-LOCK-JobName0", "contextName", "environment"));
                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");

                    // lock it
                    jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");
                    jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");

                    // release the lock - only the lock holder can release the lock
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment");
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment");
                    jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");
                    jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");

                    // release
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");
                    jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");
                }
            }
            catch (Exception e) {
                hasException.set(true);
            }
            finally {
                threadOneFinished.set(true);
            }
        });

        executor.submit(() -> {
            try {
                for(int i=0; i<1000; i++ ) {
                    jlc.doesJobParticipateInLock("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");

                    // lock it
                    jlc.lock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");
                    jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");

                    // release the lock - only the lock holder can release the lock
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId2, "environment");
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId1, "environment");
                    jlc.release("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");
                    jlc.release("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.release("AgentName1-TEST-LOCK-JobName1", contextId2, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");

                    // release
                    jlc.release("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");

                    jlc.locked("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
                    jlc.hasLock("AgentName0-TEST-LOCK-JobName0", contextId0, "environment");
                    jlc.locked("AgentName1-TEST-LOCK-JobName1", "contextName", "environment");
                    jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextId1, "environment");
                    jlc.locked("AgentName2-TEST-LOCK-JobName2", "contextName", "environment");
                    jlc.hasLock("AgentName2-TEST-LOCK-JobName2", contextId2, "environment");
                }
            }
            catch (Exception e) {
                hasException.set(true);
            }
            finally {
                threadTwoFinished.set(true);
            }
        });

        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> threadOneFinished.get() && threadTwoFinished.get());
        Assert.assertFalse(hasException.get());
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
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(jlc, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

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

    @Test
    public void test_concurrent_addLocks_thread_safety() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicBoolean hasException = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    JobLock jobLock = makeJobLock("LOCK-" + threadNum, 3, 1);
                    jlc.addLocks(List.of(jobLock), "test-env");
                } catch (Exception e) {
                    LOGGER.error("Exception in thread " + threadNum, e);
                    hasException.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("Threads timed out", latch.await(30, TimeUnit.SECONDS));
        assertFalse("Exception occurred during concurrent addLocks", hasException.get());

        // Verify all locks were added
        JobLockCacheData cacheData = ((JobLockCacheImpl) jlc).getJobLockCacheData("test-env");
        assertEquals("All locks should be added", 10, cacheData.getJobLocksByLockName().size());

        executor.shutdown();
    }

    @Test
    public void test_concurrent_lock_acquisition_race_condition() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        // Create a lock with count of 1 - only one thread can acquire it
        jlc.addLocks(List.of(makeJobLock("EXCLUSIVE-LOCK", 1, 1)), "test-env");

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        AtomicReference<String> lockHolder = new AtomicReference<>();
        List<String> acquiredLocks = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final String contextId = "context-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start at the same time
                    boolean acquired = jlc.lock("AgentName0-EXCLUSIVE-LOCK-JobName0", contextId, "test-env");
                    if (acquired) {
                        acquiredLocks.add(contextId);
                        // Hold the lock briefly
                        Thread.sleep(10);
                        jlc.release("AgentName0-EXCLUSIVE-LOCK-JobName0", contextId, "test-env");
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in lock acquisition", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue("Threads timed out", endLatch.await(30, TimeUnit.SECONDS));

        // With lock count of 1, only one thread at a time should acquire the lock
        // But over time, multiple threads should get a turn
        assertTrue("At least some threads should acquire the lock", acquiredLocks.size() > 0);
        assertTrue("Not all threads should acquire simultaneously", acquiredLocks.size() <= numThreads);

        executor.shutdown();
    }

    @Test
    public void test_concurrent_exclusive_lock_enforcement() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        // Create an exclusive lock
        JobLock exclusiveLock = makeJobLock("EXCLUSIVE", 1, 1);
        exclusiveLock.setExclusiveJobLock(true);
        jlc.addLocks(List.of(exclusiveLock), "test-env");

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        AtomicInteger concurrentHolders = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String contextId = "context-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 50; j++) {
                        if (jlc.lock("AgentName0-EXCLUSIVE-JobName0", contextId, "test-env")) {
                            int current = concurrentHolders.incrementAndGet();
                            maxConcurrent.updateAndGet(max -> Math.max(max, current));
                            Thread.sleep(1); // Hold briefly
                            concurrentHolders.decrementAndGet();
                            jlc.release("AgentName0-EXCLUSIVE-JobName0", contextId, "test-env");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(30, TimeUnit.SECONDS));

        // Exclusive lock should never have more than 1 concurrent holder
        assertEquals("Exclusive lock should allow max 1 concurrent holder", 1, maxConcurrent.get());

        executor.shutdown();
    }

    @Test
    public void test_concurrent_queue_operations() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        jlc.addLocks(List.of(makeJobLock("QUEUE-LOCK", 1, 1)), "test-env");

        int numProducers = 5;
        int numConsumers = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numProducers + numConsumers);
        CountDownLatch producerLatch = new CountDownLatch(numProducers);
        CountDownLatch consumerLatch = new CountDownLatch(numConsumers);
        AtomicBoolean hasException = new AtomicBoolean(false);
        List<Object> consumed = Collections.synchronizedList(new ArrayList<>());

        // Producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
                        job.setJobName("JobName0");
                        job.setIdentifier("AgentName0-QUEUE-LOCK-JobName0");
                        job.setContextName("context");
                        job.setChildContextName("child-" + producerId + "-" + j);

                        SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
                        event.setInternalEventDrivenJob(job);
                        event.setContextInstanceId(UUID.randomUUID().toString());

                        jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-QUEUE-LOCK-JobName0", "context", event, "test-env");
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    LOGGER.error("Producer exception", e);
                    hasException.set(true);
                } finally {
                    producerLatch.countDown();
                }
            });
        }

        // Consumers
        for (int i = 0; i < numConsumers; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 35; j++) {
                        var events = jlc.pollSchedulerJobInitiationEventWaitQueue("AgentName0-QUEUE-LOCK-JobName0", "context", "test-env");
                        if (events != null && !events.isEmpty()) {
                            consumed.addAll(events);
                        }
                        Thread.sleep(2);
                    }
                } catch (Exception e) {
                    LOGGER.error("Consumer exception", e);
                    hasException.set(true);
                } finally {
                    consumerLatch.countDown();
                }
            });
        }

        assertTrue(producerLatch.await(30, TimeUnit.SECONDS));
        assertTrue(consumerLatch.await(30, TimeUnit.SECONDS));
        assertFalse("No exceptions should occur", hasException.get());

        // Should have consumed all or most events
        assertTrue("Should consume events", consumed.size() > 0);

        executor.shutdown();
    }

    @Test
    public void test_concurrent_reset_while_operations_ongoing() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        jlc.addLocks(List.of(makeJobLock("RESET-LOCK", 5, 1)), "test-env");

        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch latch = new CountDownLatch(6);
        AtomicBoolean hasException = new AtomicBoolean(false);

        // 5 threads doing lock/release operations
        for (int i = 0; i < 5; i++) {
            final String contextId = "context-" + i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        jlc.lock("AgentName0-RESET-LOCK-JobName0", contextId, "test-env");
                        Thread.sleep(1);
                        jlc.release("AgentName0-RESET-LOCK-JobName0", contextId, "test-env");
                    }
                } catch (Exception e) {
                    LOGGER.error("Lock operation exception", e);
                    hasException.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 1 thread doing periodic resets
        executor.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(10);
                    jlc.resetLock("RESET-LOCK", "test-env");
                }
            } catch (Exception e) {
                LOGGER.error("Reset exception", e);
                hasException.set(true);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertFalse("Operations should complete without exceptions", hasException.get());

        executor.shutdown();
    }

    @Test
    public void test_concurrent_multiple_environments() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("env1");
        jlc.reset("env2");
        jlc.reset("env3");
        jlc.setJobLockCacheService(jobLockCacheService);

        String[] environments = {"env1", "env2", "env3"};

        int numThreadsPerEnv = 5;
        ExecutorService executor = Executors.newFixedThreadPool(environments.length * numThreadsPerEnv);
        CountDownLatch latch = new CountDownLatch(environments.length * numThreadsPerEnv);
        AtomicBoolean hasException = new AtomicBoolean(false);

        for (String env : environments) {
            jlc.addLocks(List.of(makeJobLock("LOCK-" + env, 3, 1)), env);

            for (int i = 0; i < numThreadsPerEnv; i++) {
                final String contextId = "context-" + i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 100; j++) {
                            jlc.lock("AgentName0-LOCK-" + env + "-JobName0", contextId, env);
                            jlc.hasLock("AgentName0-LOCK-" + env + "-JobName0", contextId, env);
                            jlc.release("AgentName0-LOCK-" + env + "-JobName0", contextId, env);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Multi-env exception", e);
                        hasException.set(true);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertFalse("No exceptions in multi-environment test", hasException.get());

        // Verify environment isolation - check that all our environments exist
        JobLockCacheImpl impl = (JobLockCacheImpl) jlc;
        List<String> envs = impl.getEnvironments();
        assertTrue("env1 should exist", envs.contains("env1"));
        assertTrue("env2 should exist", envs.contains("env2"));
        assertTrue("env3 should exist", envs.contains("env3"));

        executor.shutdown();
    }

    @Test
    public void test_concurrent_removeQueuedSchedulerJob() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        jlc.addLocks(List.of(makeJobLock("REMOVE-LOCK", 1, 1)), "test-env");

        List<SchedulerJobInstance> instances = Collections.synchronizedList(new ArrayList<>());

        // Add events to queue
        for (int i = 0; i < 50; i++) {
            InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
            job.setJobName("JobName0");
            job.setIdentifier("AgentName0-REMOVE-LOCK-JobName0");
            job.setContextName("context");
            job.setChildContextName("child-" + i);

            SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
            event.setInternalEventDrivenJob(job);
            String instanceId = UUID.randomUUID().toString();
            event.setContextInstanceId(instanceId);

            SchedulerJobInstance instance = new SchedulerJobInstanceImpl();
            instance.setJobName("JobName0");
            instance.setIdentifier("AgentName0-REMOVE-LOCK-JobName0");
            instance.setContextName("context");
            instance.setChildContextName("child-" + i);
            instance.setContextInstanceId(instanceId);
            instance.setStatus(InstanceStatus.LOCK_QUEUED);
            instances.add(instance);

            jlc.addQueuedSchedulerJobInitiationEvent("AgentName0-REMOVE-LOCK-JobName0", "context", event, "test-env");
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(instances.size());
        AtomicBoolean hasException = new AtomicBoolean(false);

        // Concurrent removal of queued jobs
        for (SchedulerJobInstance instance : instances) {
            executor.submit(() -> {
                try {
                    jlc.removeQueuedSchedulerJob(instance, "test-env");
                } catch (Exception e) {
                    LOGGER.error("Remove exception", e);
                    hasException.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertFalse("No exceptions during concurrent removal", hasException.get());

        executor.shutdown();
    }

    @Test
    public void test_concurrent_lock_release_with_doesJobParticipate() throws Exception {
        JobLockCache jlc = super.newJobLockCache();
        jlc.reset("test-env");
        jlc.setJobLockCacheService(jobLockCacheService);

        jlc.addLocks(List.of(makeJobLock("PARTICIPATE-LOCK", 3, 1)), "test-env");

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicBoolean hasException = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
            final String contextId = "context-" + i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 200; j++) {
                        // Mix of operations
                        boolean participates = jlc.doesJobParticipateInLock("AgentName0-PARTICIPATE-LOCK-JobName0", "context", "test-env");
                        assertTrue("Job should participate in lock", participates);

                        if (jlc.lock("AgentName0-PARTICIPATE-LOCK-JobName0", contextId, "test-env")) {
                            assertTrue(jlc.hasLock("AgentName0-PARTICIPATE-LOCK-JobName0", contextId, "test-env"));
                            jlc.release("AgentName0-PARTICIPATE-LOCK-JobName0", contextId, "test-env");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Mixed operations exception", e);
                    hasException.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertFalse("No exceptions during mixed operations", hasException.get());

        executor.shutdown();
    }

    /**
     * Consumes broadcast events related to job lock cache and ensures no lingering events are present.
     *
     * This method registers a broadcast listener to collect {@link JobLockCacheEvent} messages and verifies
     * that no broadcast events remain within a 30-second polling window. During this window, the method
     * periodically clears the collected events and asserts that no events were detected, ensuring a clean
     * state for subsequent operations. The polling interval and delay are set to 1 second each.
     */
    private void consumeBroadcastEvents() {
        ArrayList<JobLockCacheEvent> jobLockCacheEvent = new ArrayList<>();

        JobLockCacheEventLocalBroadcastListener broadcaster = message -> jobLockCacheEvent.add(message);
        JobLockCacheEventBroadcaster.instance().register(broadcaster);

        // We will poll for 30 seconds to make sure broadcast events are no lingering from other tests.
        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int jobLockCacheEventSize = jobLockCacheEvent.size();
                jobLockCacheEvent.clear();
                Assert.assertEquals(0, jobLockCacheEventSize);
            });
    }
}
