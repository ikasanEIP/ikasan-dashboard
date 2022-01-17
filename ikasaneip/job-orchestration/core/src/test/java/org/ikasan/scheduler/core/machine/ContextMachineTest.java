package org.ikasan.scheduler.core.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.scheduler.core.model.context.ContextParameterImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.core.model.job.InternalEventDrivenJobImpl;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ContextMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ObjectMapper objectMapper = new ObjectMapper();

    private HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
    private String queueDir = "./target";

    @Before
    public void init() {
        internalEventDrivenJobs.clear();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName6-jobName6", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName8-jobName8", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName9-jobName9", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName10-jobName10", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName11-jobName11", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName12-jobName12", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName13-jobName13", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName14-jobName14", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName15-jobName15", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName16-jobName16", new InternalEventDrivenJobImpl());
    }


    @Test
    public void test_context_machine_full_nested_context_success() throws IOException, JSONException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-running-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job3-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job2-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job4-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job5-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job6-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName7",
            "agentName7", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(2, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job7-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName8",
            "agentName8", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job9-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName10",
            "agentName10", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job10-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName11",
            "agentName11", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job11-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        status = contextMachine.getContextStatus("Context1");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName12",
            "agentName12", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job12-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName13",
            "agentName13", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job13-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName14",
            "agentName14", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job14-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName15",
            "agentName15", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job15-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName16",
            "agentName16", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job16-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context1");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_context_machine_with_job_locks() throws IOException, JSONException {
        Context context = this.contextService.getContext(loadDataFile("/data/logic/context-with-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/context-with-job-locks.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        SchedulerJobInitiationEvent event = events.get(0);

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        event = events.get(0);

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        event = events.get(0);

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event.getJobName(),
            event.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());


        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_context_machine_full_via_big_queue_nested_context_success() throws IOException, JSONException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);
        contextMachine.init();

        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            System.out.println("1 "+event.getJobName());
        });

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        contextMachine.eventReceived(objectMapper.writeValueAsString(eventInstance));

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        contextMachine.eventReceived(objectMapper.writeValueAsString(eventInstance));

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);
        contextMachine.eventReceived(objectMapper.writeValueAsString(eventInstance));

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);
        contextMachine.eventReceived(objectMapper.writeValueAsString(eventInstance));

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        contextMachine.eventReceived(objectMapper.writeValueAsString(eventInstance));

        Thread.sleep(1000);

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job5-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        contextMachine.teardown();
    }

    @Test
    public void test_get_context_status() throws IOException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_get_context_status_error() throws IOException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstanceImpl contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", false);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.ERROR, status);
        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.ERROR, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        status = contextMachine.getContextStatus("Context1");
        Assert.assertEquals(InstanceStatus.ERROR, status);
    }

    @Test
    public void test_simple_context_chained_jobs_with_context_parameters() throws IOException {
        Context context = this.contextService.getContext(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-chained-jobs-with-context-parameters.json"));

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job5 = new InternalEventDrivenJobImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5", job5);
        InternalEventDrivenJobImpl job6 = new InternalEventDrivenJobImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6", job6);
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job8 = new InternalEventDrivenJobImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8", job8);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), internalEventDrivenJobs, this.queueDir);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
        Assert.assertEquals(2, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());
        Assert.assertEquals(3, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());
        Assert.assertEquals(4, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_get_status_non_existent_context() throws IOException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);
        InstanceStatus status = contextMachine.getContextStatus("NonExistentContext");

        Assert.assertNull(status);
    }

    @Test
    public void test_get_context() throws IOException {
        Context context = this.contextService.getContext(loadDataFile("/data/context.json"));
        ContextInstance instance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, instance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir);
        ContextInstance contextInstance = contextMachine.getContext("Context3");
        Assert.assertEquals("Context3", contextInstance.getName());

        contextInstance = contextMachine.getContext("NonExistentContext");

        Assert.assertNull(contextInstance);
    }
}
