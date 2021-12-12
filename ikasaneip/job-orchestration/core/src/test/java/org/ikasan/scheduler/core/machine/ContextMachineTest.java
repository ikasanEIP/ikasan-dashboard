package org.ikasan.scheduler.core.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ContextualisedScheduledProcessEventInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.List;

public class ContextMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test_context_machine_full_nested_context_success() throws IOException, JSONException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());

        ContextualisedScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
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
    public void test_context_machine_full_via_big_queue_nested_context_success() throws IOException, JSONException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        contextMachine.init();

        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            System.out.println("1 "+event.getJobName());
        });

        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            System.out.println("2 "+event.getJobName());
        });

        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            System.out.println("3 "+event.getJobName());
        });

        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            System.out.println("4 "+event.getJobName());
        });

        ContextualisedScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
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

//        eventInstance = scheduledProcessEventInstance("jobName1",
//            "agentName1", true);
//        InstanceStatus status = contextMachine.getContextStatus("Context3");
//        Assert.assertEquals(InstanceStatus.RUNNING, status);
//        status = contextMachine.getContextStatus("Context4");
//        Assert.assertEquals(InstanceStatus.WAITING, status);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job3-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName2",
//            "agentName2", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job2-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName4",
//            "agentName4", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job4-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName5",
//            "agentName5", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(1, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job5-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName6",
//            "agentName6", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job6-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName7",
//            "agentName7", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(2, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job7-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName8",
//            "agentName8", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName9",
//            "agentName9", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job9-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        status = contextMachine.getContextStatus("Context3");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context4");
//        Assert.assertEquals(InstanceStatus.RUNNING, status);
//
//        eventInstance = scheduledProcessEventInstance("jobName10",
//            "agentName10", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(1, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job10-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName11",
//            "agentName11", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job11-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        status = contextMachine.getContextStatus("Context2");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context3");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context4");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context5");
//        Assert.assertEquals(InstanceStatus.WAITING, status);
//        status = contextMachine.getContextStatus("Context1");
//        Assert.assertEquals(InstanceStatus.RUNNING, status);
//
//        eventInstance = scheduledProcessEventInstance("jobName12",
//            "agentName12", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        status = contextMachine.getContextStatus("Context5");
//        Assert.assertEquals(InstanceStatus.RUNNING, status);
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job12-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName13",
//            "agentName13", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(1, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job13-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName14",
//            "agentName14", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job14-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName15",
//            "agentName15", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(1, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job15-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        eventInstance = scheduledProcessEventInstance("jobName16",
//            "agentName16", true);
//
//        events = contextMachine.eventReceived(eventInstance);
//        Assert.assertEquals(0, events.size());
//
//        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job16-success-context-status.json")
//            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
//
//        status = contextMachine.getContextStatus("Context2");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context3");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context4");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context5");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
//        status = contextMachine.getContextStatus("Context1");
//        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_get_context_status() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName1",
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
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName1",
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
    public void test_get_status_non_existent_context() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        InstanceStatus status = contextMachine.getContextStatus("NonExistentContext");

        Assert.assertNull(status);
    }

    @Test
    public void test_get_context() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, new ScheduledContextInstanceServiceTestImpl());
        ContextInstance contextInstance = contextMachine.getContext("Context3");
        Assert.assertEquals("Context3", contextInstance.getName());

        contextInstance = contextMachine.getContext("NonExistentContext");

        Assert.assertNull(contextInstance);
    }


    @Test
    public void test() throws JsonProcessingException {
        ContextualisedScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventInstance));
    }
}
