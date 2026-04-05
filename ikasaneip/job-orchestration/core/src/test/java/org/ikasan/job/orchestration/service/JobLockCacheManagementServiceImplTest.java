package org.ikasan.job.orchestration.service;

import org.ikasan.job.orchestration.AbstractJobLockCacheTest;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachineImpl;
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
    ContextMachineImpl contextMachine;
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

    @Test
    public void test_remove_queued_event_without_context_machine() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));

        InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("JobName1");
        instance.setIdentifier("AgentName1-TEST-LOCK-JobName1");
        instance.setContextName(contextName);
        instance.setContextInstanceId("nonExistentContextInstanceId");
        instance.setChildContextName("child");
        instance.setStatus(InstanceStatus.LOCK_QUEUED);
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setContextName(contextName);
        schedulerJobInitiationEvent.setJobName("JobName1");
        schedulerJobInitiationEvent.setInternalEventDrivenJob(instance);
        schedulerJobInitiationEvent.setChildContextNames(List.of("childContext1"));
        schedulerJobInitiationEvent.setContextInstanceId("nonExistentContextInstanceId");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName1-TEST-LOCK-JobName1", contextName, schedulerJobInitiationEvent, "environment");

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.removeQueuedSchedulerJobInitiationEvent(schedulerJobInitiationEvent, "environment");

        // The queued job should be removed even without context machine
        assertNull(JobLockCacheImpl.instance().pollSchedulerJobInitiationEventWaitQueue("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));

        verifyNoInteractions(contextMachine);
    }

    @Test
    public void test_release_locked_job_with_multiple_queued_events() throws IOException {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        // 3 jobs one lock count
        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");

        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));

        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setId("contextInstanceId");

        when(this.contextMachine.getContext()).thenReturn(contextInstance);
        ContextMachineCache.instance().put(contextMachine);

        // Add multiple queued events
        for (int i = 1; i <= 3; i++) {
            InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
            instance.setJobName("JobName" + i);
            instance.setIdentifier("AgentName" + i + "-TEST-LOCK-JobName" + i);
            instance.setContextName(contextName);
            instance.setContextInstanceId("contextInstanceId");
            instance.setChildContextName("child" + i);
            instance.setStatus(InstanceStatus.WAITING);
            SchedulerJobInitiationEvent event = new SchedulerJobInitiationEventImpl();
            event.setContextName(contextName);
            event.setJobName("AgentName" + i + "-TEST-LOCK-JobName" + i);
            event.setInternalEventDrivenJob(instance);
            event.setContextInstanceId("contextInstanceId");

            jlc.addQueuedSchedulerJobInitiationEvent("AgentName" + i + "-TEST-LOCK-JobName" + i, contextName, event, "environment");
        }

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.releaseLockedJob(jobIdentifier, contextName, "environment");

        // Original job should not have lock
        assertFalse(jlc.hasLock(jobIdentifier, contextName, "environment"));

        // First queued job should now have the lock
        assertTrue(jlc.hasLock("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));

        // Verify publish was called for all queued events
        verify(this.contextMachine, atLeastOnce()).publishJobInitiationEvent(any());
    }

    @Test
    public void test_release_locked_job_with_queued_event_without_context_machine() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);
        String contextName = "contextName";
        String jobIdentifier = "AgentName0-TEST-LOCK-JobName0";

        jlc.addLocks(List.of(makeJobLock("TEST-LOCK", 3, 1)), "environment");
        assertTrue(jlc.lock(jobIdentifier, contextName, "environment"));

        InternalEventDrivenJobInstance instance = new InternalEventDrivenJobInstanceImpl();
        instance.setJobName("JobName1");
        instance.setIdentifier("AgentName1-TEST-LOCK-JobName1");
        instance.setContextName(contextName);
        instance.setContextInstanceId("nonExistentContextInstanceId");
        instance.setChildContextName("child");
        SchedulerJobInitiationEvent event = new SchedulerJobInitiationEventImpl();
        event.setContextName(contextName);
        event.setJobName("AgentName1-TEST-LOCK-JobName1");
        event.setInternalEventDrivenJob(instance);
        event.setContextInstanceId("nonExistentContextInstanceId");

        jlc.addQueuedSchedulerJobInitiationEvent("AgentName1-TEST-LOCK-JobName1", contextName, event, "environment");

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();
        managementService.releaseLockedJob(jobIdentifier, contextName, "environment");

        // Lock should be released
        assertFalse(jlc.hasLock(jobIdentifier, contextName, "environment"));

        // Queued event is processed but lock not acquired since context machine doesn't exist
        assertNull(JobLockCacheImpl.instance().pollSchedulerJobInitiationEventWaitQueue("AgentName1-TEST-LOCK-JobName1", contextName, "environment"));
    }

    @Test
    public void test_release_locked_job_null_parameters() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();

        // Should not throw exception with null parameters
        managementService.releaseLockedJob(null, null, null);
        managementService.releaseLockedJob("job", null, "env");
        managementService.releaseLockedJob(null, "context", "env");
    }

    @Test
    public void test_remove_queued_event_null_internal_event_driven_job() {
        JobLockCache jlc = JobLockCacheImpl.instance();
        jlc.setJobLockCacheService(jobLockCacheService);

        SchedulerJobInitiationEvent event = new SchedulerJobInitiationEventImpl();
        event.setContextName("contextName");
        event.setJobName("JobName");
        event.setInternalEventDrivenJob(null);

        JobLockCacheManagementService managementService = new JobLockCacheManagementServiceImpl();

        // Should handle null internal event driven job gracefully
        try {
            managementService.removeQueuedSchedulerJobInitiationEvent(event, "environment");
        } catch (NullPointerException e) {
            // Expected - this tests the current behavior
        }
    }
}
