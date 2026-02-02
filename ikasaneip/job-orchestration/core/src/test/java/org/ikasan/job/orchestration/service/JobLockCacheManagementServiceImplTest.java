package org.ikasan.job.orchestration.service;

import org.ikasan.job.orchestration.AbstractJobLockCacheTest;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheManagementService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheManagementServiceImplTest extends AbstractJobLockCacheTest {

    @Mock
    ContextMachine contextMachine;
    @Before
    public void setup() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void test_released_locked_job_no_queued_events() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));
        assertTrue(jlc.hasLock(jobIdentifier, contextName, "environment"));

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.releaseLockedJob("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");

        assertFalse(jlc.hasLock(jobIdentifier, contextName, "environment"));

        verifyNoMoreInteractions(contextMachine);
    }

    @Test
    public void test_released_locked_job_with_queued_events() throws IOException {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));
        assertTrue(jlc.locked(jobIdentifier, contextName, "environment"));

        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setId("contextInstanceId");

        when(this.contextMachine.getContext()).thenReturn(contextInstance);

        ContextMachineCache.instance().put(contextMachine);

        InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("JobName1");
        instance.setIdentifier("AgentName1-TEST-LOCK-JobName1");
        instance.setContextName(contextName);
        instance.setContextInstanceId("contextInstanceId");
        instance.setChildContextName("child");
        instance.setStatus(InstanceStatus.WAITING);
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setContextName(contextName);
        schedulerJobInitiationEvent.setJobName("AgentName1-TEST-LOCK-JobName1");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(instance);
        schedulerJobInitiationEvent.setChildContextNames(List.of("childContext1", "childContext2"));
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName1-TEST-LOCK-JobName1", contextName, schedulerJobInitiationEvent, "environment");

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.releaseLockedJob("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");

        // The original job no longer has the lock because we released it!
        assertFalse(jlc.hasLock(jobIdentifier, contextName, "environment"));

        // The job that was queued now has the lock
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));

        // The job that was queued no longer is in the queue
        assertNull(JobLockCacheImpl.instance().pollSchedulerJobInitiationEventWaitQueue("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));

        verify(this.contextMachine, times(3)).getContext();
        verify(this.contextMachine, times(1)).registerToNotificationMonitors();
        verify(this.contextMachine, times(1)).publishJobInitiationEvent(any());
        verifyNoMoreInteractions(contextMachine);
    }

    @Test(expected = RuntimeException.class)
    public void test_released_locked_job_with_queued_events_throws_exception() throws IOException {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));
        assertTrue(jlc.locked(jobIdentifier, contextName, "environment"));

        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setId("contextInstanceId");

        when(this.contextMachine.getContext()).thenReturn(contextInstance);
        doThrow(new IOException("error!")).when(this.contextMachine).publishJobInitiationEvent(any(SchedulerJobInitiationEvent.class));

        ContextMachineCache.instance().put(contextMachine);

        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setContextName(contextName);
        schedulerJobInitiationEvent.setJobName("AgentName1-TEST-LOCK-JobName1");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(new InternalEventDrivenJobInstanceImpl());
        schedulerJobInitiationEvent.setChildContextNames(List.of("childContext1", "childContext2"));
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName1-TEST-LOCK-JobName1", contextName, schedulerJobInitiationEvent, "environment");

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.releaseLockedJob("AgentName0-TEST-LOCK-JobName0", "contextName", "environment");
    }

    @Test
    public void test_remove_queued_event() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));
        assertTrue(jlc.locked(jobIdentifier, contextName, "environment"));

        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setId("contextInstanceId");

        when(this.contextMachine.getContext()).thenReturn(contextInstance);

        ContextMachineCache.instance().put(contextMachine);

        InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("JobName1");
        instance.setIdentifier("AgentName1-TEST-LOCK-JobName1");
        instance.setContextName(contextName);
        instance.setContextInstanceId("contextInstanceId");
        instance.setChildContextName("child");
        instance.setStatus(InstanceStatus.LOCK_QUEUED);
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setContextName(contextName);
        schedulerJobInitiationEvent.setJobName("JobName1");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(instance);
        schedulerJobInitiationEvent.setChildContextNames(List.of("childContext1", "childContext2"));
        schedulerJobInitiationEvent.setContextInstanceId("contextInstanceId");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName1-TEST-LOCK-JobName1", contextName, schedulerJobInitiationEvent, "environment");

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.removeQueuedSchedulerJobInitiationEvent(schedulerJobInitiationEvent, "environment");

        // The original job will still hold the lock!
        assertTrue(jlc.locked(jobIdentifier, contextName, "environment"));

        // But the queued job is no longer in the queue!
        assertNull(JobLockCacheImpl.instance().pollSchedulerJobInitiationEventWaitQueue("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));

        verify(this.contextMachine, times(3)).getContext();
        verify(this.contextMachine, times(1)).registerToNotificationMonitors();
        verify(this.contextMachine, times(1)).resetJob(anyString(), anyString());
        verifyNoMoreInteractions(contextMachine);
    }
}
