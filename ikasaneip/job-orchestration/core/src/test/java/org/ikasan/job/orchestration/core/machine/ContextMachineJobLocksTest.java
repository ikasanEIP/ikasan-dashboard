package org.ikasan.job.orchestration.core.machine;

import static org.ikasan.job.orchestration.core.machine.ContextMachineTestHelper.createInternalJobsMap;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.ikasan.job.orchestration.JobLockCacheServiceTestImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ContextMachineJobLocksTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private String queueDir = "./target";
    private ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;
    
    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
    }

    @Test
    public void test_context_machine_locks_in_two_different_contexts_one_lock_one_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context1 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));
        ContextInstance instance1 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));

        ContextTemplate context2 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));
        ContextInstance instance2 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context1.getAllNestedJobLocks());
        jobLockCache.addLocks(context2.getAllNestedJobLocks());

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);

        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // jobName1 completes
        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        assertEquals(1, events1.size());
        //raises event for jobName2
        assertEquals("jobName2", events1.get(0).getJobName());

        // jobName5 completes
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        // jobName2 has the lock so completion of job 5 does not raise any events
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // jobName2 starting
        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // errored for jobName2. jobName3 should be raised as iy has no dependency on job2
        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(1, events1.size());
        assertEquals("jobName3", events1.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.ERROR, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // error for jobName3 (2 and 3 did not complete so 4 not raised)
        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", false);
        events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(1, events1.size());
        // job 6 raised here because it is released from the queue
        assertEquals("jobName6", events1.get(0).getJobName());

        // complete 6 which currently has the lock and raises 7 which is queued waiting for the lock to be released.
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events2.size());
        assertEquals("jobName7", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.ERROR, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // complete 7 raises 8
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events2.size());
        assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.ERROR, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        // complete 8
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.ERROR, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.ERROR, jobStatus);
        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_one_lock() throws IOException, InvalidContextTemplateException {
        ContextTemplate context1 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));
        ContextInstance instance1 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));

        ContextTemplate context2 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));
        ContextInstance instance2 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context1.getAllNestedJobLocks());
        jobLockCache.addLocks(context2.getAllNestedJobLocks());

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        assertEquals(1, events1.size());
        assertEquals("jobName2", events1.get(0).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        // job2 has the lock so completion of job 5 does not raise any events
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events1 = contextMachine1.eventReceived(eventInstance1);

        assertEquals(1, events1.size());
        assertEquals("jobName3", events1.get(0).getJobName());
        // job3 has the lock so completion of job 6 does not raise any events
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events1 = contextMachine1.eventReceived(eventInstance1);

        // all locks released so events for job4 and job8 can continue
        assertEquals(2, events1.size());
        assertEquals("jobName6", events1.get(0).getJobName());
        assertEquals("jobName4", events1.get(1).getJobName());
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events have been raised so not more events to raise
        assertEquals(0, events1.size());
        assertEquals(1, events2.size());
        assertEquals("jobName7", events2.get(0).getJobName());

        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events2 = contextMachine2.eventReceived(eventInstance2);

        assertEquals(1, events2.size());
        assertEquals("jobName8", events2.get(0).getJobName());

        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events2 = contextMachine2.eventReceived(eventInstance2);

        assertEquals(0, events2.size());


        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_lock_count_2() throws IOException, InvalidContextTemplateException {
        String context1Json = loadDataFile("/data/locks/context-with-same-job-locks-1.json");
        context1Json = context1Json.replace("\"lockCount\": 1", "\"lockCount\": 2");

        ContextTemplate context1 = contextService.getContextTemplate(context1Json);
        ContextInstance instance1 = contextService.getContextInstance(context1Json);

        String context2Json = loadDataFile("/data/locks/context-with-same-job-locks-2.json");
        context2Json = context2Json.replace("\"lockCount\": 1", "\"lockCount\": 2");

        ContextTemplate context2 = contextService.getContextTemplate(context2Json);
        ContextInstance instance2 = contextService.getContextInstance(context2Json);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context1.getAllNestedJobLocks());
        jobLockCache.addLocks(context2.getAllNestedJobLocks());

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        assertEquals(2, events1.size());
        assertEquals("jobName2", events1.get(0).getJobName());
        assertEquals("jobName3", events1.get(1).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        // jobs 2 and 3 get first dibs of events not job4 and job5
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // job 3 still has a lock and in waiting state no events raised
        assertEquals(1, events1.size());
        // job 6 fired and job 7 can now get the lock hence event raised for job7
        assertEquals(1, events2.size());
        assertEquals("jobName7", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events1.size());
        //job4 event raised
        assertEquals("jobName4", events1.get(0).getJobName());
        assertEquals(1, events2.size());
        //job8 event raised
        assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // no more events to raise
        assertEquals(0, events1.size());
        assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_lock_count_huge() throws IOException, InvalidContextTemplateException {
        String context1Json = loadDataFile("/data/locks/context-with-same-job-locks-1.json");
        context1Json = context1Json.replace("\"lockCount\": 1", "\"lockCount\": 2147483647");

        ContextTemplate context1 = contextService.getContextTemplate(context1Json);
        ContextInstance instance1 = contextService.getContextInstance(context1Json);

        String context2Json = loadDataFile("/data/locks/context-with-same-job-locks-2.json");
        context2Json = context2Json.replace("\"lockCount\": 1", "\"lockCount\": 2147483647");

        ContextTemplate context2 = contextService.getContextTemplate(context2Json);
        ContextInstance instance2 = contextService.getContextInstance(context2Json);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context1.getAllNestedJobLocks());
        jobLockCache.addLocks(context2.getAllNestedJobLocks());

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        assertEquals(2, events1.size());
        assertEquals("jobName2", events1.get(0).getJobName());
        assertEquals("jobName3", events1.get(1).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        assertEquals(2, events2.size());
        assertEquals("jobName6", events2.get(0).getJobName());
        assertEquals("jobName7", events2.get(1).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events already raised
        assertEquals(0, events1.size());
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events1.size());
        assertEquals("jobName4", events1.get(0).getJobName());
        assertEquals(1, events2.size());
        assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events raised
        assertEquals(0, events1.size());
        assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_different_locks() throws IOException, InvalidContextTemplateException {
        String context1Json = loadDataFile("/data/locks/context-with-different-job-locks-1.json");
        ContextTemplate context1 = contextService.getContextTemplate(context1Json);
        ContextInstance instance1 = contextService.getContextInstance(context1Json);

        String context2Json = loadDataFile("/data/locks/context-with-different-job-locks-2.json");
        ContextTemplate context2 = contextService.getContextTemplate(context2Json);
        ContextInstance instance2 = contextService.getContextInstance(context2Json);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context1.getAllNestedJobLocks());
        jobLockCache.addLocks(context2.getAllNestedJobLocks());

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        assertEquals(1, events1.size());
        assertEquals("jobName2", events1.get(0).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        assertEquals(1, events2.size());
        assertEquals("jobName6", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events1.size());
        assertEquals("jobName3", events1.get(0).getJobName());
        assertEquals(1, events2.size());
        assertEquals("jobName7", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.LOCK_QUEUED, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        assertEquals(1, events1.size());
        assertEquals("jobName4", events1.get(0).getJobName());
        assertEquals(1, events2.size());
        assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events raised
        assertEquals(0, events1.size());
        assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        assertEquals(InstanceStatus.COMPLETE, instanceStatus);

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_locks_one_lock_multi_same_name_jobs() throws Exception {
        String agentName = "asset-control-scheduler-agent";
        Map<String, InstanceStatus> workingStatuses = createInitialStatuses();

        String jsonContext = loadDataFile("/data/contexts/CONTEXT-369160711-with-or-logic.json");
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-140537370",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-97656185",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1559391736"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "97656185",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-97656185"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "140537370_ScheduledJob_15:30:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1559391736",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1559391736"
         *       } ]
         */

        assertEquals(0, this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "140537370_ScheduledJob_15:30:00", true).size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"));

        workingStatuses.put("CONTEXT-369160711", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-769949213", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-140537370", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        List<SchedulerJobInitiationEvent> events = this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "-1559391736", true);
        assertEquals(1, events.size());
        assertEquals("97656185", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent--1559391736"));

        assertContextStatuses(contextMachine, workingStatuses);

        assertEquals(0, this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "97656185", true).size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent-97656185"));


        // CONTEXT-140537370 completes
        workingStatuses.put("CONTEXT-140537370", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1167353422",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-991999604",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-1666702606",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-991999604"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--854506457",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1666702606"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--457154928",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--854506457"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-1483324359",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--457154928"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-199248836",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1483324359"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--1222568013",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-199248836"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--259049314",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1222568013"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-812176495",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--259049314"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "991999604",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-991999604"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1167353422_ScheduledJob_15:45:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1666702606",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1666702606"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-854506457",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--854506457"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-457154928",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--457154928"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1483324359",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1483324359"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "199248836",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-199248836"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1222568013",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1222568013"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-259049314",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--259049314"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "812176495",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-812176495"
         *       } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1167353422_ScheduledJob_15:45:00", true);
        assertEquals(1, events.size());
        assertEquals("991999604", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"));

        workingStatuses.put("CONTEXT-1167353422", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "991999604", true);
        assertEquals(1, events.size());
        assertEquals("1666702606", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-991999604"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1666702606", true);
        assertEquals(1, events.size());
        assertEquals("-854506457", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1666702606"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-854506457", true);
        assertEquals(1, events.size());
        assertEquals("-457154928", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--854506457"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-457154928", true);
        assertEquals(1, events.size());
        assertEquals("1483324359", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--457154928"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1483324359", true);
        assertEquals(1, events.size());
        assertEquals("199248836", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1483324359"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "199248836", true);
        assertEquals(1, events.size());
        assertEquals("-1222568013", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-199248836"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-1222568013", true);
        assertEquals(1, events.size());
        assertEquals("-259049314", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--1222568013"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-259049314", true);
        assertEquals(1, events.size());
        assertEquals("812176495", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--259049314"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "812176495", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-199248836"));

        // CONTEXT-1167353422 now complete
        workingStatuses.put("CONTEXT-1167353422", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         * {
         *         "name" : "CONTEXT--2125239559",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--391607565"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--391607564"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-391607565",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--391607565"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-391607564",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--391607564"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // CONTEXT-1724676333 now running
        // CONTEXT--2125239559 now running
        workingStatuses.put("CONTEXT--2125239559", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1724676333", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "-391607565", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent--391607565"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "-391607564", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent--391607564"));
        // CONTEXT--2125239559 now complete
        workingStatuses.put("CONTEXT--2125239559", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);


        /**
         *         "name" : "CONTEXT-1208521119",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-761640318"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-761640319"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "761640318",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-761640318"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "761640319",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-761640319"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // CONTEXT-1208521119 now running
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "761640318", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-761640318"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "761640319", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-761640319"));

        // CONTEXT-1208521119 now complete
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1034431901",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--167881790"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--167881789"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-167881790",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--167881790"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-167881789",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--167881789"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // CONTEXT-1208521119 now running
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "-167881790", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent--167881790"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "-167881789", true);
        assertEquals(0, events.size());

        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent--167881789"));
        // CONTEXT-1034431901 now complete
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1405621029",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1781592066"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1781592067"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1781592066",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1781592066"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1781592067",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1781592067"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1781592066", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1781592066"));

        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1781592067", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1781592067"));

        // CONTEXT--1405621029 now complete
        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1596119798",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1685894164"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1685894165"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1685894164",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1685894164"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1685894165",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1685894165"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1685894165", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1685894165"));

        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1685894164", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1685894164"));

        // CONTEXT--1596119798 now complete
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1967431808",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1779796514"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1779796515"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1779796514",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1779796514"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1779796515",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1779796515"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1779796514", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1779796514"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1164721449"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1779796515", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1779796515"));

        // CONTEXT-1967431808 now complete
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1589044962",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--1178291132"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--1178291131"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1178291132",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1178291132"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1178291131",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1178291131"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "-1178291131", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent--1178291131"));

        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "-1178291132", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent--1178291132"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent-1164721449"));

        // CONTEXT-1589044962 now complete
        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1872161100",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--1244387358"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--1244387357"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1244387358",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1244387358"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1244387357",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1244387357"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "-1244387358", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent--1244387358"));

        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "-1244387357", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent--1244387357"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent-1164721449"));

        // CONTEXT--1872161100 now complete
        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1724676333",
         *       "contexts" : [ {
         *         "name" : "CONTEXT--2017369407",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--928291134"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--928291133"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291134",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291134"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291133",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291133"
         *         } ]
         */

        // CONTEXT-1724676333 completes when the next contexts complete
        assertEquals(InstanceStatus.WAITING, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1892741766",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-185916817",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1010295672"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1568132585"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--532050073",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1515829064"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-185916817"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-131944233",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-200769144"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--532050073"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-2074200534",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--2047526486"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-131944233"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-959606163",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1657966748"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-2074200534"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--144197246",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1487547688"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-581199512"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1951960644"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--605707438"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--716901243"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--375330452"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-959606163"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--1352189442",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--598161030"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--144197246"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "185916817",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-185916817"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1892741766_ScheduledJob_17:00:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1010295672",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1010295672"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1568132585",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1568132585"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-532050073",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--532050073"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1515829064",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1515829064"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "131944233",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-131944233"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "200769144",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-200769144"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "2074200534",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-2074200534"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-2047526486",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--2047526486"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "959606163",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-959606163"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1657966748",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1657966748"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-144197246",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--144197246"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1487547688",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1487547688"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "581199512",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-581199512"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1951960644",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1951960644"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-605707438",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--605707438"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-716901243",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--716901243"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-375330452",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--375330452"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1352189442",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1352189442"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-598161030",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--598161030"
         *       } ]
         */

        /**
         *       "name" : "CONTEXT--1281498017",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent--958075417",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-185916817"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--532050073"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-131944233"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-2074200534"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-959606163"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--144197246"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1352189442"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-958075417",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--958075417"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1281498017_ScheduledJob_17:00:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1281498017_ScheduledJob_17:00:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "185916817",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-185916817"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-532050073",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--532050073"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "131944233",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-131944233"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "2074200534",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-2074200534"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "959606163",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-959606163"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-144197246",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--144197246"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1352189442",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1352189442"
         *       } ]
         *     } ]
         */

        // SPECIAL NOTE interleaving CONTEXT-1892741766 and CONTEXT--1281498017 here
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.RUNNING);
        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1892741766_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"));

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT--1281498017"), agentName, "-1281498017_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1281498017", "asset-control-scheduler-agent--1281498017_ScheduledJob_17:00:00"));

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1568132585", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1568132585"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1010295672", true);
        assertEquals(1, events.size());
        assertEquals("185916817", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1010295672"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "185916817", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-185916817"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1515829064", true);
        assertEquals(1, events.size());
        assertEquals("-532050073", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1515829064"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-532050073", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--532050073"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "200769144", true);
        assertEquals(1, events.size());
        assertEquals("131944233", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-200769144"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "131944233", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-131944233"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-2047526486", true);
        assertEquals(1, events.size());
        assertEquals("2074200534", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--2047526486"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "2074200534", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-2074200534"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1657966748", true);
        assertEquals(1, events.size());
        assertEquals("959606163", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1657966748"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "959606163", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-959606163"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-375330452", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--375330452"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-716901243", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--716901243"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-605707438", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--605707438"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1951960644", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1951960644"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "581199512", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-581199512"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1487547688", true);
        assertEquals(1, events.size());
        assertEquals("-144197246", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1487547688"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-144197246", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--144197246"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-598161030", true);
        assertEquals(1, events.size());
        assertEquals("-1352189442", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--598161030"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-1352189442", true);
        assertEquals(1, events.size());
        assertEquals("-958075417", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1352189442"));

        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT--1281498017"), agentName, "-958075417", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1281498017", "asset-control-scheduler-agent--958075417"));

        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1724676333",
         *       "contexts" : [ {
         *         "name" : "CONTEXT--2017369407",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--928291134"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--928291133"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291134",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291134"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291133",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291133"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "-928291134", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent--928291134"));
        workingStatuses.put("CONTEXT--2017369407", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "-928291133", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent--928291133"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1164721449"));

        // CONTEXT--2017369407 now complete
        // CONTEXT-1724676333 now complete
        // CONTEXT-769949213 now complete
        // CONTEXT-369160711 now complete
        workingStatuses.put("CONTEXT--2017369407", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-1724676333", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-769949213", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-369160711", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        // make sure all statuses in the map completed
        workingStatuses.entrySet().forEach(s -> assertEquals(InstanceStatus.COMPLETE, s.getValue()));

        // make sure there are no locks still locked
        validateAllLocksCleared();
    }


    @Test
    public void test_context_machine_full_locks_one_lock_multi_same_name_jobs_with_jobs_fired_in_order_to_queue_waiting_initiation_events() throws Exception {
        String agentName = "asset-control-scheduler-agent";
        Map<String, InstanceStatus> workingStatuses = createInitialStatuses();

        String jsonContext = loadDataFile("/data/contexts/CONTEXT-369160711-with-or-logic.json");
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextInstance contextInstance = this.contextService.getContextInstance(jsonContext);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-140537370",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-97656185",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1559391736"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "97656185",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-97656185"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "140537370_ScheduledJob_15:30:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1559391736",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1559391736"
         *       } ]
         */

        assertEquals(0, this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "140537370_ScheduledJob_15:30:00", true).size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent-140537370_ScheduledJob_15:30:00"));

        workingStatuses.put("CONTEXT-369160711", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-769949213", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-140537370", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        List<SchedulerJobInitiationEvent> events = this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "-1559391736", true);
        assertEquals(1, events.size());
        assertEquals("97656185", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent--1559391736"));

        assertContextStatuses(contextMachine, workingStatuses);

        assertEquals(0, this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-140537370", agentName, "97656185", true).size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-140537370", "asset-control-scheduler-agent-97656185"));


        // CONTEXT-140537370 completes
        workingStatuses.put("CONTEXT-140537370", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1167353422",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-991999604",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-1666702606",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-991999604"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--854506457",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1666702606"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--457154928",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--854506457"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-1483324359",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--457154928"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-199248836",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1483324359"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--1222568013",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-199248836"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--259049314",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1222568013"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-812176495",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--259049314"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "991999604",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-991999604"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1167353422_ScheduledJob_15:45:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1666702606",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1666702606"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-854506457",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--854506457"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-457154928",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--457154928"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1483324359",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1483324359"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "199248836",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-199248836"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1222568013",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1222568013"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-259049314",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--259049314"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "812176495",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-812176495"
         *       } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine,
            "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1167353422_ScheduledJob_15:45:00", true);
        assertEquals(1, events.size());
        assertEquals("991999604", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1167353422_ScheduledJob_15:45:00"));

        workingStatuses.put("CONTEXT-1167353422", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "991999604", true);
        assertEquals(1, events.size());
        assertEquals("1666702606", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-991999604"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1666702606", true);
        assertEquals(1, events.size());
        assertEquals("-854506457", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1666702606"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-854506457", true);
        assertEquals(1, events.size());
        assertEquals("-457154928", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--854506457"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-457154928", true);
        assertEquals(1, events.size());
        assertEquals("1483324359", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--457154928"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "1483324359", true);
        assertEquals(1, events.size());
        assertEquals("199248836", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-1483324359"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "199248836", true);
        assertEquals(1, events.size());
        assertEquals("-1222568013", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-199248836"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-1222568013", true);
        assertEquals(1, events.size());
        assertEquals("-259049314", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--1222568013"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "-259049314", true);
        assertEquals(1, events.size());
        assertEquals("812176495", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent--259049314"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1167353422", agentName, "812176495", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1167353422", "asset-control-scheduler-agent-199248836"));

        // CONTEXT-1167353422 now complete
        workingStatuses.put("CONTEXT-1167353422", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         * {
         *         "name" : "CONTEXT--2125239559",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--391607565"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--391607564"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-391607565",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--391607565"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-391607564",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--391607564"
         *         } ]
         */

        // Firstly fire the 17:00 time based job into the machine across all contexts that the asset-control-scheduler-agent-1164721449 repeating job resides in.
        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711"
            , List.of("CONTEXT--2125239559"
                , "CONTEXT-1208521119"
                , "CONTEXT-1034431901"
                , "CONTEXT--1405621029"
                , "CONTEXT--1596119798"
                , "CONTEXT-1967431808"
                , "CONTEXT-1589044962"
                , "CONTEXT--1872161100"
                , "CONTEXT-1724676333")
            , agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // Make sure all the following contexts are running.
        workingStatuses.put("CONTEXT--2125239559", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1724676333", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "-391607565", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent--391607565"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "-391607564", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent--391607564"));
        // CONTEXT--2125239559 now complete
        workingStatuses.put("CONTEXT--2125239559", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);


        /**
         *         "name" : "CONTEXT-1208521119",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-761640318"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-761640319"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "761640318",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-761640318"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "761640319",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-761640319"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // CONTEXT-1208521119 now running
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "761640318", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-761640318"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "761640319", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-761640319"));

        // CONTEXT-1208521119 now complete
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1034431901",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--167881790"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--167881789"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-167881790",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--167881790"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-167881789",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--167881789"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));

        // CONTEXT-1208521119 now running
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "-167881790", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent--167881790"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "-167881789", true);
        assertEquals(0, events.size());

        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent--167881789"));
        // CONTEXT-1034431901 now complete
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1405621029",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1781592066"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1781592067"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1781592066",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1781592066"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1781592067",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1781592067"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1781592066", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1781592066"));

        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1781592067", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1781592067"));

        // CONTEXT--1405621029 now complete
        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1596119798",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1685894164"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1685894165"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1685894164",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1685894164"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1685894165",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1685894165"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1685894165", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1685894165"));

        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1685894164", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1685894164"));

        // CONTEXT--1596119798 now complete
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1967431808",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent-1779796514"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent-1779796515"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1779796514",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1779796514"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1779796515",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1779796515"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1779796514", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1779796514"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1779796515", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1779796515"));

        // CONTEXT-1967431808 now complete
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT-1589044962",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--1178291132"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--1178291131"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1178291132",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1178291132"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1178291131",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1178291131"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "-1178291131", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent--1178291131"));

        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        // CONTEXT-1589044962 now complete
        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *         "name" : "CONTEXT--1872161100",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--1244387358"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--1244387357"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1244387358",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1244387358"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-1244387357",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--1244387357"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "-1244387358", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent--1244387358"));

        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "-1244387357", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent--1244387357"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);


        // Now we start fining in all of the repeating 1164721449 jobs and this will release queued jobs from the cache.
        JobLockCacheData jobLockCacheData = ((JobLockCacheData) ReflectionTestUtils.getField(JobLockCacheImpl.instance(), "jobLockCacheData"));

        JobLockHolder jobLockHolder = jobLockCacheData.getJobLocksByLockName().get("%Partition%.AC_BB_LK");
        Queue jobLockQueue = ((Queue) ReflectionTestUtils.getField(jobLockHolder, "queuedSchedulerJobInitiationEvents"));

        Assert.assertEquals(7, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2125239559", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2125239559", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT--2125239559", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(6, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1208521119", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1208521119", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT-1208521119", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(5, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1034431901", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1034431901", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT-1034431901", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(4, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1405621029", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1405621029", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT--1405621029", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(3, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1596119798", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1596119798", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT--1596119798", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(2, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1967431808", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1967431808", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT-1967431808", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(1, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "-1178291132", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent--1178291132"));
        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT-1589044962", agentName, "1164721449", true);
        assertEquals(1, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1589044962", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT-1589044962", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        Assert.assertEquals(0, jobLockQueue.size());

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--1872161100", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1872161100", "asset-control-scheduler-agent-1164721449"));
        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        // CONTEXT--1872161100 now complete
        workingStatuses.put("CONTEXT--1872161100", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1724676333",
         *       "contexts" : [ {
         *         "name" : "CONTEXT--2017369407",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--928291134"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--928291133"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291134",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291134"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291133",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291133"
         *         } ]
         */

        // CONTEXT-1724676333 completes when the next contexts complete
        assertEquals(InstanceStatus.WAITING, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1892741766",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent-185916817",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1010295672"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1568132585"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--532050073",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1515829064"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-185916817"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-131944233",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-200769144"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--532050073"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-2074200534",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--2047526486"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-131944233"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent-959606163",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1657966748"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-2074200534"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--144197246",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1487547688"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-581199512"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-1951960644"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--605707438"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--716901243"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--375330452"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-959606163"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "asset-control-scheduler-agent--1352189442",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--598161030"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--144197246"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "185916817",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-185916817"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1892741766_ScheduledJob_17:00:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1010295672",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1010295672"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1568132585",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1568132585"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-532050073",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--532050073"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1515829064",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1515829064"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "131944233",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-131944233"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "200769144",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-200769144"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "2074200534",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-2074200534"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-2047526486",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--2047526486"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "959606163",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-959606163"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1657966748",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1657966748"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-144197246",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--144197246"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1487547688",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1487547688"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "581199512",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-581199512"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "1951960644",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-1951960644"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-605707438",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--605707438"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-716901243",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--716901243"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-375330452",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--375330452"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1352189442",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1352189442"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-598161030",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--598161030"
         *       } ]
         */

        /**
         *       "name" : "CONTEXT--1281498017",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "asset-control-scheduler-agent--958075417",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "asset-control-scheduler-agent-185916817"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--532050073"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-131944233"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-2074200534"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent-959606163"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--144197246"
         *           }, {
         *             "identifier" : "asset-control-scheduler-agent--1352189442"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-958075417",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--958075417"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1281498017_ScheduledJob_17:00:00",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1281498017_ScheduledJob_17:00:00"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "185916817",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-185916817"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-532050073",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--532050073"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "131944233",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-131944233"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "2074200534",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-2074200534"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "959606163",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent-959606163"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-144197246",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--144197246"
         *       }, {
         *         "agentName" : "asset-control-scheduler-agent",
         *         "jobName" : "-1352189442",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "asset-control-scheduler-agent--1352189442"
         *       } ]
         *     } ]
         */

        // SPECIAL NOTE interleaving CONTEXT-1892741766 and CONTEXT--1281498017 here
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.RUNNING);
        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1892741766_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1892741766_ScheduledJob_17:00:00"));

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT--1281498017"), agentName, "-1281498017_ScheduledJob_17:00:00", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1281498017", "asset-control-scheduler-agent--1281498017_ScheduledJob_17:00:00"));

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1568132585", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1568132585"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1010295672", true);
        assertEquals(1, events.size());
        assertEquals("185916817", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1010295672"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "185916817", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-185916817"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1515829064", true);
        assertEquals(1, events.size());
        assertEquals("-532050073", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1515829064"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-532050073", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--532050073"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "200769144", true);
        assertEquals(1, events.size());
        assertEquals("131944233", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-200769144"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "131944233", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-131944233"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-2047526486", true);
        assertEquals(1, events.size());
        assertEquals("2074200534", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--2047526486"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "2074200534", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-2074200534"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1657966748", true);
        assertEquals(1, events.size());
        assertEquals("959606163", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1657966748"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "959606163", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-959606163"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-375330452", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--375330452"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-716901243", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--716901243"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-605707438", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--605707438"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "1951960644", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-1951960644"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "581199512", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent-581199512"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-1487547688", true);
        assertEquals(1, events.size());
        assertEquals("-144197246", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1487547688"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-144197246", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--144197246"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766"), agentName, "-598161030", true);
        assertEquals(1, events.size());
        assertEquals("-1352189442", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--598161030"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT-1892741766", "CONTEXT--1281498017"), agentName, "-1352189442", true);
        assertEquals(1, events.size());
        assertEquals("-958075417", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT-1892741766", "asset-control-scheduler-agent--1352189442"));

        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.RUNNING);
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachineWithChildContextId(contextMachine, "CONTEXT-369160711", List.of("CONTEXT--1281498017"), agentName, "-958075417", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--1281498017", "asset-control-scheduler-agent--958075417"));

        workingStatuses.put("CONTEXT--1281498017", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-1892741766", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        /**
         *       "name" : "CONTEXT-1724676333",
         *       "contexts" : [ {
         *         "name" : "CONTEXT--2017369407",
         *         "jobDependencies" : [ {
         *           "jobIdentifier" : "asset-control-scheduler-agent-1164721449",
         *           "logicalGrouping" : {
         *             "logicalGrouping" : {
         *               "or" : [ {
         *                 "identifier" : "asset-control-scheduler-agent--928291134"
         *               }, {
         *                 "identifier" : "asset-control-scheduler-agent--928291133"
         *               } ]
         *             },
         *             "and" : [ {
         *               "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *             } ]
         *           }
         *         } ],
         *         "scheduledJobs" : [ {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1164721449",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1164721449"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "1724676333_ScheduledJob_17:00:00",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291134",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291134"
         *         }, {
         *           "agentName" : "asset-control-scheduler-agent",
         *           "jobName" : "-928291133",
         *           "startupControlType" : "AUTOMATIC",
         *           "identifier" : "asset-control-scheduler-agent--928291133"
         *         } ]
         */

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "-928291134", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent--928291134"));
        workingStatuses.put("CONTEXT--2017369407", InstanceStatus.RUNNING);
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "1724676333_ScheduledJob_17:00:00", true);
        assertEquals(1, events.size());
        assertEquals("1164721449", events.get(0).getJobName());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1724676333_ScheduledJob_17:00:00"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "-928291133", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent--928291133"));
        assertContextStatuses(contextMachine, workingStatuses);

        events = this.sendScheduledEventToContextMachine(contextMachine, "CONTEXT-369160711", "CONTEXT--2017369407", agentName, "1164721449", true);
        assertEquals(0, events.size());
        assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("CONTEXT--2017369407", "asset-control-scheduler-agent-1164721449"));

        // CONTEXT--2017369407 now complete
        // CONTEXT-1724676333 now complete
        // CONTEXT-769949213 now complete
        // CONTEXT-369160711 now complete
        workingStatuses.put("CONTEXT--2017369407", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-1724676333", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-769949213", InstanceStatus.COMPLETE);
        workingStatuses.put("CONTEXT-369160711", InstanceStatus.COMPLETE);
        assertContextStatuses(contextMachine, workingStatuses);

        // make sure all statuses in the map completed
        workingStatuses.entrySet().forEach(s -> assertEquals(InstanceStatus.COMPLETE, s.getValue()));

        // make sure there are no locks still locked
        validateAllLocksCleared();
    }

    private Map<String, InstanceStatus> createInitialStatuses() {
        Map<String, InstanceStatus> map = new HashMap<>();
        map.put("CONTEXT-369160711", InstanceStatus.WAITING);
        map.put("CONTEXT-769949213", InstanceStatus.WAITING);
        map.put("CONTEXT-140537370", InstanceStatus.WAITING);
        map.put("CONTEXT-1167353422", InstanceStatus.WAITING);
        map.put("CONTEXT-1724676333", InstanceStatus.WAITING);
        map.put("CONTEXT--2017369407", InstanceStatus.WAITING);
        map.put("CONTEXT--1405621029", InstanceStatus.WAITING);
        map.put("CONTEXT--1872161100", InstanceStatus.WAITING);
        map.put("CONTEXT-1034431901", InstanceStatus.WAITING);
        map.put("CONTEXT-1589044962", InstanceStatus.WAITING);
        map.put("CONTEXT-1208521119", InstanceStatus.WAITING);
        map.put("CONTEXT-1967431808", InstanceStatus.WAITING);
        map.put("CONTEXT--2125239559", InstanceStatus.WAITING);
        map.put("CONTEXT--1596119798", InstanceStatus.WAITING);
        map.put("CONTEXT-1892741766", InstanceStatus.WAITING);
        map.put("CONTEXT--1281498017", InstanceStatus.WAITING);
        return map;
    }

    private void assertContextStatuses(ContextMachine contextMachine, Map<String, InstanceStatus> map) {
        for (String key : map.keySet()) {
            assertEquals("key: " + key, map.get(key), contextMachine.getContextStatus(key));
        }
    }

    private List<SchedulerJobInitiationEvent> sendScheduledEventToContextMachineWithChildContextId(ContextMachine contextMachine, String contextId, List<String> childContextIds
        , String agentName, String jobName, boolean eventSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance(contextId, childContextIds, jobName, agentName, eventSuccessful);

        return contextMachine.eventReceived(eventInstance);
    }

    private List<SchedulerJobInitiationEvent> sendScheduledEventToContextMachine(ContextMachine contextMachine, String contextId, String childContextId
        , String agentName, String jobName, boolean eventSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance(contextId, childContextId, jobName, agentName, eventSuccessful);

        return contextMachine.eventReceived(eventInstance);
    }

}
