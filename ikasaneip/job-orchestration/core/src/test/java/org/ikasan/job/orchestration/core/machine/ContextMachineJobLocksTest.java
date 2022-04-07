package org.ikasan.job.orchestration.core.machine;

import static org.ikasan.job.orchestration.core.machine.ContextMachineTestHelper.createInternalJobsMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.JobLockCacheServiceTestImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ContextMachineJobLocksTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private String queueDir = "./target";
    private ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_one_lock() throws IOException, InvalidContextTemplateException {
        ContextTemplate context1 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));
        ContextInstance instance1 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-1.json"));

        ContextTemplate context2 = contextService.getContextTemplate(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));
        ContextInstance instance2 = contextService.getContextInstance(loadDataFile("/data/locks/context-with-same-job-locks-2.json"));

        Map<String, InternalEventDrivenJob> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJob> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        contextTemplateValidator.validate(context1);
        contextTemplateValidator.validate(context2);

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        Assert.assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName2", events1.get(0).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        // job2 has the lock so completion of job 5 does not raise any events
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName3", events1.get(0).getJobName());
        // job3 has the lock so completion of job 6 does not raise any events
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all locks released so events for job4 and job8 can continue
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName4", events1.get(0).getJobName());
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events have been raised so not more events to raise
        Assert.assertEquals(0, events1.size());
        Assert.assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
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

        Map<String, InternalEventDrivenJob> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJob> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        contextTemplateValidator.validate(context1);
        contextTemplateValidator.validate(context2);

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        Assert.assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        Assert.assertEquals(2, events1.size());
        Assert.assertEquals("jobName2", events1.get(0).getJobName());
        Assert.assertEquals("jobName3", events1.get(1).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        // jobs 2 and 3 get first dibs of events not job4 and job5
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // job 3 still has a lock and in waiting state no events raised
        Assert.assertEquals(0, events1.size());
        // job 6 fired and job 7 can now get the lock hence event raised for job7
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName7", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(1, events1.size());
        //job4 event raised
        Assert.assertEquals("jobName4", events1.get(0).getJobName());
        Assert.assertEquals(1, events2.size());
        //job8 event raised
        Assert.assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // no more events to raise
        Assert.assertEquals(0, events1.size());
        Assert.assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
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

        Map<String, InternalEventDrivenJob> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJob> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        contextTemplateValidator.validate(context1);
        contextTemplateValidator.validate(context2);

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        Assert.assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        Assert.assertEquals(2, events1.size());
        Assert.assertEquals("jobName2", events1.get(0).getJobName());
        Assert.assertEquals("jobName3", events1.get(1).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        Assert.assertEquals(2, events2.size());
        Assert.assertEquals("jobName6", events2.get(0).getJobName());
        Assert.assertEquals("jobName7", events2.get(1).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events already raised
        Assert.assertEquals(0, events1.size());
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName4", events1.get(0).getJobName());
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events raised
        Assert.assertEquals(0, events1.size());
        Assert.assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
    }

    @Test
    public void test_context_machine_full_locks_in_two_different_contexts_different_locks() throws IOException, InvalidContextTemplateException {
        String context1Json = loadDataFile("/data/locks/context-with-different-job-locks-1.json");
        ContextTemplate context1 = contextService.getContextTemplate(context1Json);
        ContextInstance instance1 = contextService.getContextInstance(context1Json);

        String context2Json = loadDataFile("/data/locks/context-with-different-job-locks-2.json");
        ContextTemplate context2 = contextService.getContextTemplate(context2Json);
        ContextInstance instance2 = contextService.getContextInstance(context2Json);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobs1 = createInternalJobsMap(context1);
        Map<String, InternalEventDrivenJob> internalEventDrivenJobs2 = createInternalJobsMap(context2);

        contextTemplateValidator.validate(context1);
        contextTemplateValidator.validate(context2);

        ContextMachine contextMachine1 = new ContextMachine(context1, instance1, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs1, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());
        ContextMachine contextMachine2 = new ContextMachine(context2, instance2, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs2, queueDir, new HashMap<>(), new JobLockCacheServiceTestImpl());

        InstanceStatus instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.WAITING, instanceStatus);

        InstanceStatus jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        ContextualisedScheduledProcessEventImpl eventInstance1 = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance1.setJobStarting(true);
        ContextualisedScheduledProcessEventImpl eventInstance2 = scheduledProcessEventInstance("jobName5", "agentName5", false);
        eventInstance2.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events1 = contextMachine1.eventReceived(eventInstance1);
        Assert.assertEquals(0, events1.size());
        List<SchedulerJobInitiationEvent> events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(0, events2.size());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.RUNNING, jobStatus);

        events1 = contextMachine1.eventReceived(scheduledProcessEventInstance("jobName1", "agentName1", true));
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName2", events1.get(0).getJobName());
        events2 = contextMachine2.eventReceived(scheduledProcessEventInstance("jobName5", "agentName5", true));
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName6", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName1-jobName1");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", false);
        eventInstance1.setJobStarting(true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", false);
        eventInstance2.setJobStarting(true);

        eventInstance1 = scheduledProcessEventInstance("jobName2", "agentName2", true);
        eventInstance2 = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName3", events1.get(0).getJobName());
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName7", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.WAITING, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName3", "agentName3", true);
        eventInstance2 = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals("jobName4", events1.get(0).getJobName());
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals("jobName8", events2.get(0).getJobName());

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.RUNNING, instanceStatus);

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName7-jobName7");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        eventInstance1 = scheduledProcessEventInstance("jobName4", "agentName4", true);
        eventInstance2 = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events1 = contextMachine1.eventReceived(eventInstance1);
        events2 = contextMachine2.eventReceived(eventInstance2);
        // all events raised
        Assert.assertEquals(0, events1.size());
        Assert.assertEquals(0, events2.size());

        jobStatus = contextMachine1.getJobStatus("Context-Locks-1", "agentName4-jobName4");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);
        jobStatus = contextMachine2.getJobStatus("Context-Locks-2", "agentName8-jobName8");
        Assert.assertEquals(InstanceStatus.COMPLETE, jobStatus);

        instanceStatus = contextMachine1.getContextStatus("Context-Locks-1");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
        instanceStatus = contextMachine2.getContextStatus("Context-Locks-2");
        Assert.assertEquals(InstanceStatus.COMPLETE, instanceStatus);
    }
}
