package org.ikasan.job.orchestration.core.machine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
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
import java.util.concurrent.atomic.AtomicReference;

public class ContextMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
    private String queueDir = "./target";

    private ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();

    @Before
    public void init() {
        internalEventDrivenJobs.clear();
        internalEventDrivenJobs.put("agentName1-jobName1", this.newInternalEventDrivenJob("agentName1-jobName1"));
        internalEventDrivenJobs.put("agentName2-jobName2", this.newInternalEventDrivenJob("agentName2-jobName2"));
        internalEventDrivenJobs.put("agentName3-jobName3", this.newInternalEventDrivenJob("agentName3-jobName3"));
        internalEventDrivenJobs.put("agentName4-jobName4", this.newInternalEventDrivenJob("agentName4-jobName4"));
        internalEventDrivenJobs.put("agentName5-jobName5", this.newInternalEventDrivenJob("agentName5-jobName5"));
        internalEventDrivenJobs.put("agentName6-jobName6", this.newInternalEventDrivenJob("agentName6-jobName6"));
        internalEventDrivenJobs.put("agentName7-jobName7", this.newInternalEventDrivenJob("agentName7-jobName7"));
        internalEventDrivenJobs.put("agentName8-jobName8", this.newInternalEventDrivenJob("agentName8-jobName8"));
        internalEventDrivenJobs.put("agentName9-jobName9", this.newInternalEventDrivenJob("agentName9-jobName9"));
        internalEventDrivenJobs.put("agentName10-jobName10", this.newInternalEventDrivenJob("agentName10-jobName10"));
        internalEventDrivenJobs.put("agentName11-jobName11", this.newInternalEventDrivenJob("agentName11-jobName11"));
        internalEventDrivenJobs.put("agentName12-jobName12", this.newInternalEventDrivenJob("agentName12-jobName12"));
        internalEventDrivenJobs.put("agentName13-jobName13", this.newInternalEventDrivenJob("agentName13-jobName13"));
        internalEventDrivenJobs.put("agentName14-jobName14", this.newInternalEventDrivenJob("agentName14-jobName14"));
        internalEventDrivenJobs.put("agentName15-jobName15", this.newInternalEventDrivenJob("agentName15-jobName15"));
        internalEventDrivenJobs.put("agentName16-jobName16", this.newInternalEventDrivenJob("agentName16-jobName16"));
        internalEventDrivenJobs.put("scheduler-agent-STPMUR.GLOBAL_BATCH_DONE", this.newInternalEventDrivenJob("scheduler-agent-STPMUR.GLOBAL_BATCH_DONE"));
        internalEventDrivenJobs.put("scheduler-agent-STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=kill)"
            , this.newInternalEventDrivenJob("scheduler-agent-STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=kill)"));
        internalEventDrivenJobs.put("scheduler-agent-LOG_MAINTENANCE"
            , this.newInternalEventDrivenJob("scheduler-agent-LOG_MAINTENANCE"));
        internalEventDrivenJobs.put("scheduler-agent-UPDATE_UNIQUEIDs"
            , this.newInternalEventDrivenJob("scheduler-agent-UPDATE_UNIQUEIDs"));
        internalEventDrivenJobs.put("scheduler-agent-STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=run)"
            , this.newInternalEventDrivenJob("scheduler-agent-STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=run)"));

        internalEventDrivenJobs.put("scheduler-agent-STOP_ALL_WORKFLOWS"
            , this.newInternalEventDrivenJob("scheduler-agent-STOP_ALL_WORKFLOWS"));

        internalEventDrivenJobs.put("scheduler-agent-ETF_BSKT_PURGE"
            , this.newInternalEventDrivenJob("scheduler-agent-ETF_BSKT_PURGE"));

        internalEventDrivenJobs.put("scheduler-agent-BSP_CLEAN_PPGT"
            , this.newInternalEventDrivenJob("scheduler-agent-BSP_CLEAN_PPGT"));

    }

    @Test
    public void test_complex_sample_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/SAMPLE_CONTEXT/context/SAMPLE_CONTEXT.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/SAMPLE_CONTEXT/context/SAMPLE_CONTEXT.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance
            , new ScheduledContextInstanceServiceTestImpl(), internalEventDrivenJobs, this.queueDir, new HashMap<>());

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("STPMUR.GLOBAL_BATCH_DONE",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=kill)",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("LOG_MAINTENANCE",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("UPDATE_UNIQUEIDs",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("STARTSTOP_ALL_MUREX(STPMUR.STARTSTOP_ALL_MUREX.RUN_KILL=run)",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("BSP_CLEAN_FBS",
            "scheduler-agent", true);
        contextMachine.eventReceived(eventInstance);

        System.out.println(this.objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(contextMachine.getContextInstanceStatus()));
    }

    private InternalEventDrivenJob newInternalEventDrivenJob(String jobIdentifier) {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setIdentifier(jobIdentifier);

        return job;
    }


    @Test
    public void test_context_machine_full_nested_context_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

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

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("bad-job-identifier");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("bad-job-identifier");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_complete() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.COMPLETE);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_running() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RUNNING);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_on_hold() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ON_HOLD);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_in_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ERROR);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_skipped() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.SKIPPED);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_complete() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.COMPLETE);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_running() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RUNNING);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_released() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_in_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ERROR);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_skipped() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.SKIPPED);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");
    }

    @Test
    public void test_context_machine_full_nested_context_job_release_success_job_already_on_hold() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ON_HOLD);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1");

        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_release_bad_job_when_others_on_hold_exception() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName5-jobName5");
        contextMachine.holdJob("agentName16-jobName16");

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-running-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        contextMachine.releaseJob("bad-job-name");
    }

    @Test
    public void test_context_machine_full_nested_context_job_held_success() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        contextMachine.init();
        contextMachine.holdJob("agentName5-jobName5");
        contextMachine.holdJob("agentName16-jobName16");

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-running-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job1-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job3-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job2-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job4-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        Assert.assertEquals(1, contextInstance.getHeldJobs().size());

        AtomicReference<String> jobName = new AtomicReference<>();
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            jobName.set(event.getJobName());
        });

        contextMachine.releaseJob("agentName5-jobName5");

        Assert.assertEquals(0, contextInstance.getHeldJobs().size());

        Thread.sleep(1000);

        Assert.assertEquals("jobName5", jobName.get());

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job5-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job6-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName7",
            "agentName7", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(2, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job7-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName8",
            "agentName8", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job9-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName10",
            "agentName10", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job10-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName11",
            "agentName11", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job11-success-context-status-on-hold.json")
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

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job12-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName13",
            "agentName13", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job13-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName14",
            "agentName14", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job14-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName15",
            "agentName15", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job15-success-context-status-on-hold.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        AtomicReference<String> jobName2 = new AtomicReference<>();
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
            jobName2.set(event.getJobName());
        });

        contextMachine.releaseJob("agentName16-jobName16");

        Assert.assertEquals(0, contextInstance.getHeldJobs().size());

        Thread.sleep(1000);

        Assert.assertEquals("jobName16", jobName2.get());

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
    public void test_context_machine_with_job_locks() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-job-locks.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

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
    public void test_context_machine_with_job_locks_containing_four_jobs() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-four-jobs-in-job-locks.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

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
    public void test_context_machine_with_job_locks_containing_four_jobs_in_two_separate_job_locks() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(2, events.size());

        SchedulerJobInitiationEvent event1 = events.get(0);
        SchedulerJobInitiationEvent event2 = events.get(1);

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        event1 = events.get(0);

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event2.getJobName(),
            event2.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event2.getJobName(),
            event2.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        event1 = events.get(0);

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(event1.getJobName(),
            event1.getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());


        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_context_machine_full_nested_context_with_job_locks_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/nested-contexts-with-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/nested-contexts-with-locks.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

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
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName8",
            "agentName8", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

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
    public void test_context_machine_full_nested_context_with_job_locks_at_different_levels_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/nested-contexts-with-locks-at-different-levels.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/nested-contexts-with-locks-at-different-levels.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);

        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName4", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName6", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job6-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName7",
            "agentName7", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName8",
            "agentName8", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

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
    public void test_context_machine_full_via_big_queue_nested_context_success() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
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
    public void test_get_context_status() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
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
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
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
    public void test_simple_context_chained_jobs_with_context_parameters() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-chained-jobs-with-context-parameters.json"));

        this.contextTemplateValidator.validate(context);

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

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), internalEventDrivenJobs, this.queueDir, new HashMap<>());

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
    public void test_get_status_non_existent_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        InstanceStatus status = contextMachine.getContextStatus("NonExistentContext");

        Assert.assertNull(status);
    }

    @Test
    public void test_get_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance instance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, instance, new ScheduledContextInstanceServiceTestImpl()
            , this.internalEventDrivenJobs, this.queueDir, new HashMap<>());
        ContextInstance contextInstance = contextMachine.getContext("Context3");
        Assert.assertEquals("Context3", contextInstance.getName());

        contextInstance = contextMachine.getContext("NonExistentContext");

        Assert.assertNull(contextInstance);
    }
}
