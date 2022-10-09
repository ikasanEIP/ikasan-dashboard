package org.ikasan.job.orchestration.core.machine;

import static org.ikasan.job.orchestration.core.machine.ContextMachineTestHelper.createInternalJobsMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.job.orchestration.JobLockCacheServiceTestImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(MockitoJUnitRunner.class)
public class ContextMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
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
    public void test_context_machine_full_nested_context_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job8-success-context-status-no-lock.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job9-success-context-status-no-lock.json")
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

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("bad-job-identifier", "bad_context_name");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("bad-job-identifier", "bad_context_name");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_complete() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.COMPLETE);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_running() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RUNNING);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_on_hold() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ON_HOLD);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_in_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ERROR);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_job_already_skipped() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.SKIPPED);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_complete() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.COMPLETE);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_running() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RUNNING);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_released() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_in_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ERROR);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_skipped() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.SKIPPED);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test
    public void test_context_machine_full_nested_context_job_release_success_job_already_on_hold() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ON_HOLD);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");

        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_release_bad_job_when_others_on_hold_exception() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName5-jobName5", "Context3");
        contextMachine.holdJob("agentName16-jobName16", "Context3");

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

        contextMachine.releaseJob("bad-job-name", "Context3");
    }

    @Test
    public void test_context_machine_full_nested_context_job_held_success() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName5-jobName5", "Context3");
        contextMachine.holdJob("agentName16-jobName16", "Context5");

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
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> jobName.set(event.getJobName()));

        contextMachine.releaseJob("agentName5-jobName5", "Context3");

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
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> jobName2.set(event.getJobName()));

        contextMachine.releaseJob("agentName16-jobName16", "Context5");

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
    public void test_context_machine_full_nested_context_job_held_success_same_job_multiple_contexts() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-same-job-multiple-contexts.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-same-job-multiple-contexts.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.holdJob("agentName5-jobName5", "Context3");
        contextMachine.holdJob("agentName16-jobName16", "Context5");

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        Assert.assertEquals(1, contextInstance.getHeldJobs().size());

        AtomicReference<String> jobName = new AtomicReference<>();
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> jobName.set(event.getJobName()));

        contextMachine.releaseJob("agentName5-jobName5", "Context3");

        Assert.assertEquals(0, contextInstance.getHeldJobs().size());

        Thread.sleep(1000);

        Assert.assertEquals("jobName5", jobName.get());

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        eventInstance.setChildContextNames(List.of("Context3"));

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);
        eventInstance.setChildContextNames(List.of("Context3"));

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName7",
            "agentName7", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(2, events.size());

        eventInstance = scheduledProcessEventInstance("jobName8",
            "agentName8", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName9",
            "agentName9", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName10",
            "agentName10", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName11",
            "agentName11", true);
        eventInstance.setChildContextNames(List.of("Context4"));

        contextMachine.holdJob("agentName6-jobName6", "Context4");

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
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

        eventInstance = scheduledProcessEventInstance("jobName13",
            "agentName13", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName14",
            "agentName14", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName15",
            "agentName15", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        AtomicReference<String> jobName2 = new AtomicReference<>();
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> jobName2.set(event.getJobName()));

        contextMachine.releaseJob("agentName16-jobName16", "Context5");

        // agentName6-jobName6 in Context4 is still held
        Assert.assertEquals(1, contextInstance.getHeldJobs().size());

        Thread.sleep(1000);

        Assert.assertEquals("jobName16", jobName2.get());

        eventInstance = scheduledProcessEventInstance("jobName16",
            "agentName16", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
        status = contextMachine.getContextStatus("Context1");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        // RELEASE THE HELD JOB.
        contextMachine.releaseJob("agentName6-jobName6", "Context4");

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);
        eventInstance.setChildContextNames(List.of("Context4"));

        contextMachine.eventReceived(eventInstance);

        // Give it some time to process
        Thread.sleep(1000);

        // No more held jobs
        Assert.assertEquals(0, contextInstance.getHeldJobs().size());

        // Now confirm that the job is complete.
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
    public void test_context_machine_with_job_locks() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-job-locks.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_with_job_locks_containing_four_jobs() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-four-jobs-in-job-locks.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);
        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_with_job_locks_containing_four_jobs_in_two_separate_job_locks() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);
        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_nested_context_with_job_locks_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/nested-contexts-with-locks.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/nested-contexts-with-locks.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_nested_context_with_job_locks_at_different_levels_success() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/locks/nested-contexts-with-locks-at-different-levels.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/locks/nested-contexts-with-locks-at-different-levels.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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
        // jobName3 released because it is the first queued job
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        // jobName4 released because it is the first queued job
        Assert.assertEquals("jobName4", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        // jobName5 released because it is the first queued job
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        // jobName6 released because it is the first queued job
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

        validateAllLocksCleared();
    }

    @Test
    public void test_context_machine_full_via_big_queue_nested_context_success() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ObjectMapper objectMapper = new ObjectMapper();
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        Thread.sleep(1000);

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/job5-success-context-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        String outboundQueueName = "outbound-" + contextInstance.getId() + "-queue";
        String inboundQueueName = "inbound-" + contextInstance.getId() + "-queue";

        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));

        contextMachine.teardown();

        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
    }

    @Test
    public void test_complex_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/CONTEXT-36916071.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-36916071.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);


        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        InstanceStatus status = contextMachine.getContextStatus("CONTEXT-1892741766");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("1892741766_ScheduledJob_17:00:00",
            "scheduler-agent", true);

        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("CONTEXT-1892741766");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("1010295672",
            "scheduler-agent", true);

        contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.WAITING);

        eventInstance = scheduledProcessEventInstance("1568132585",
            "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.WAITING);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstanceStarting(events.get(0).getJobName(),
            events.get(0).getAgentName(), true);

        contextMachine.eventReceived(eventInstance);

        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.RUNNING);

        eventInstance = scheduledProcessEventInstance(events.get(0).getJobName(),
            events.get(0).getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--1515829064", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.WAITING);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("-1515829064",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--1515829064", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.WAITING);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstanceStarting(events.get(0).getJobName(),
            events.get(0).getAgentName(), true);

        contextMachine.eventReceived(eventInstance);

        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.RUNNING);

        eventInstance = scheduledProcessEventInstance(events.get(0).getJobName(),
            events.get(0).getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--1515829064", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-200769144", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-131944233", InstanceStatus.WAITING);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("200769144",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--1515829064", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-200769144", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-131944233", InstanceStatus.WAITING);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance(events.get(0).getJobName(),
            events.get(0).getAgentName(), true);

        events = contextMachine.eventReceived(eventInstance);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1010295672", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1568132585", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-1892741766_ScheduledJob_17:00:00", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-185916817", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--1515829064", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--532050073", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-200769144", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-131944233", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent--2047526486", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine,"CONTEXT-1892741766", "scheduler-agent-2074200534", InstanceStatus.WAITING);
        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_get_context_status() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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
    public void test_get_job_status() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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

        status = contextMachine.getJobStatus("Context3", "agentName2-jobName2");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getJobStatus("Context3", "agentName3-jobName3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getJobStatus("Context3", "agentName4-jobName4");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getJobStatus("Context3", "agentName5-jobName5");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getJobStatus("Context3", "agentName6-jobName6");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_get_context_status_error() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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
    public void test_error_then_resubmit_to_raise_downstream_events() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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

        // We resubmit the job events but with raisedDueToFailureResubmission == true
        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", false);
        eventInstance.setRaisedDueToFailureResubmission(true);
        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);

        // We get the downstream jobs to execute as expected.
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        // Make sure that the contexts remain in the correct states.
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

        // Make sure the failing job is still in error!
        this.assertJobStatus(contextMachine, "Context3", "agentName2-jobName2", InstanceStatus.ERROR);
    }

    @Test(expected = ContextMachineException.class)
    public void test_exception_attempt_ro_resubmit_to_raise_downstream_events_but_job_is_in_complete_state() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context2");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        status = contextMachine.getContextStatus("Context5");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        status = contextMachine.getContextStatus("Context1");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        // We resubmit the job events but with raisedDueToFailureResubmission == true
        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", false);
        eventInstance.setRaisedDueToFailureResubmission(true);
        contextMachine.eventReceived(eventInstance);
    }

    @Test
    public void test_query_machine_to_determine_which_jobs_can_run() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
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

        // We resubmit the job events but with raisedDueToFailureResubmission == true
        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", false);
        eventInstance.setRaisedDueToFailureResubmission(true);
        List<SchedulerJobInitiationEvent> events = contextMachine.getEventsThatCanRun(eventInstance);

        // We get the downstream jobs to execute as expected.
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        // Make sure that the contexts remain in the correct states.
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

        // Make sure the failing job is still in error!
        this.assertJobStatus(contextMachine, "Context3", "agentName2-jobName2", InstanceStatus.ERROR);
    }

    @Test
    public void test_simple_context_chained_jobs_with_context_parameters() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-chained-jobs-with-context-parameters.json"));

        this.contextTemplateValidator.validate(context);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job5 = new InternalEventDrivenJobInstanceImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", job5);
        InternalEventDrivenJobInstanceImpl job6 = new InternalEventDrivenJobInstanceImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6-Context1", job6);
        internalEventDrivenJobs.put("agentName7-jobName7-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job8 = new InternalEventDrivenJobInstanceImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8-Context1", job8);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

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

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        InstanceStatus status = contextMachine.getContextStatus("NonExistentContext");

        Assert.assertNull(status);
    }

    @Test
    public void test_get_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance instance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, instance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        ContextInstance contextInstance = contextMachine.getContext("Context3");
        Assert.assertEquals("Context3", contextInstance.getName());

        contextInstance = contextMachine.getContext("NonExistentContext");

        Assert.assertNull(contextInstance);
    }

    /**
     * This test performs a complex batch based on file /data/contexts/CONTEXT-1436221681.json
     *
     * The format of the test is to firee ContextualisedScheduledProcessEventImpl at the ContextMachine
     * and monitor for JobInitiationEvents that are raised by the context machine.
     *
     * At key intervals within the context orchestration, the state of all the internal contexts
     * are validated in order to make sure that the appropriate state transitions are occurring
     * when events are raised.
     *
     * JSON snippets accompany areas of the code exercising that part of the file.
     *
     * @throws IOException
     */
    @Test
    public void test_complex_context_with_scheduled_and_file_jobs() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        /**
         * name" : "CONTEXT-1616645609",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent-1799613995",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *         } ]
         *       }
         *     }, {
         *       "jobIdentifier" : "scheduler-agent--1352045846",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *         } ]
         *       }
         *     } ],
         *     "scheduledJobs" : [ {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *     } ]
         *   }
         */
        // Execute the first scheduled job that fires based on a cron expression.
        Assert.assertEquals(1, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-1616645609"
                ,"scheduler-agent", "1799613995_ScheduledJob_06:00", true).size());

        // Now check that the appropriate contexts are in the state that we expect.
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        // Send the subsequent events in the initial context.
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-1616645609"
                , "scheduler-agent", "1799613995", true).size());

        Assert. assertEquals(1, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-1616645609"
                , "scheduler-agent", "-1352045846_ScheduledJob_06:00", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-1616645609"
                , "scheduler-agent", "-1352045846", true).size());

        // Now confirm that the initial context is complete but the parent still running as some of its children are waiting.
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);


        /**
         * {
         *     "name" : "CONTEXT-1848727981",
         *     "contexts" : [ {
         *       "name" : "CONTEXT--1209755884",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1164721449",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-515125013"
         *           }, {
         *             "identifier" : "scheduler-agent-515125014"
         *           }, {
         *             "identifier" : "scheduler-agent--2127221002"
         *           }, {
         *             "identifier" : "scheduler-agent--2127221001"
         *           }, {
         *             "identifier" : "scheduler-agent--1423328214"
         *           }, {
         *             "identifier" : "scheduler-agent--1423328213"
         *           }, {
         *             "identifier" : "scheduler-agent-1634692843"
         *           }, {
         *             "identifier" : "scheduler-agent-1634692844"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1164721449",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1164721449"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "515125013",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-515125013"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "515125014",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-515125014"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-2127221002",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--2127221002"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-2127221001",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--2127221001"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-1423328214",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--1423328214"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-1423328213",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--1423328213"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1634692843",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1634692843"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1634692844",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1634692844"
         *       } ]
         *     }
         */
        // Now waiting on a bunch of file received jobs
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "515125013", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "515125014", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "-2127221002", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "-2127221001", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "-1423328214", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "-1423328213", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "1634692843", true).size());

        List<SchedulerJobInitiationEvent> jobs = this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT--1209755884",
                "scheduler-agent", "1634692844", true);

        Assert.assertEquals(1, jobs.size());

        Assert.assertEquals(1, jobs.get(0).getChildContextNames().size());
        Assert.assertEquals("CONTEXT--1209755884", jobs.get(0).getChildContextNames().get(0));

        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        // Now the event that occurred due to all the file events
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--1209755884"
                , "scheduler-agent", "1164721449", true).size());

        // confirm that context 1209755884 is now complete
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT-774294372",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--505061472",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--940759996"
         *           }, {
         *             "identifier" : "scheduler-agent-1998299171"
         *           }, {
         *             "identifier" : "scheduler-agent-707845497"
         *           }, {
         *             "identifier" : "scheduler-agent--1874104712"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-505061472",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--505061472"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-940759996",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--940759996"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1998299171",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1998299171"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "707845497",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-707845497"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-1874104712",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--1874104712"
         *       } ]
         *     }
         */
        // Now waiting on a bunch more file received jobs
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine,  "CONTEXT-1436221681", "CONTEXT-774294372",
                "scheduler-agent", "-940759996", true).size());

        // confirm the context now running
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.RUNNING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-774294372",
                "scheduler-agent", "1998299171", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-774294372",
                "scheduler-agent", "707845497", true).size());

        // once all 4 file events are received an event is raised
        Assert.assertEquals(1, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-774294372",
                "scheduler-agent", "-1874104712", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-774294372", "CONTEXT--2036736597"),
                "scheduler-agent", "-505061472", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--2036736597",
                "scheduler-agent", "-502413013", true).size());

        // confirm that context 1209755884 is now complete
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *     "name" : "CONTEXT-1590773100",
         *     "contexts" : [ {
         *       "name" : "CONTEXT--129403053",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1164721449",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-1178974128"
         *           }, {
         *             "identifier" : "scheduler-agent-1178974129"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1164721449",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1164721449"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1178974128",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1178974128"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "1178974129",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-1178974129"
         *       } ]
         *     }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--129403053",
                "scheduler-agent", "1178974129", true).size());

        // once all 4 file events are received an event is raised
        Assert.assertEquals(1, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--129403053",
                "scheduler-agent", "1178974128", true).size());


        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT--129403053",
                "scheduler-agent", "1164721449", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT-1589183395",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-744167903",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--1692626050"
         *           } ]
         *         }
         *       }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachine
            (contextMachine, "CONTEXT-1436221681", "CONTEXT-1589183395",
                "scheduler-agent", "-1692626050", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1589183395", "CONTEXT-1195088490"),
                "scheduler-agent", "744167903", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT-1195088490",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--1479686678",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-744167903"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-1479686678",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--1479686678"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "744167903",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-744167903"
         *       } ]
         *     }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1195088490"),
                "scheduler-agent", "-1479686678", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *     "name" : "CONTEXT-1182789380",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent-97656185",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent--857357080"
         *         } ]
         *       }
         *     }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789380"),
                "scheduler-agent", "-857357080", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789380", "CONTEXT--663833459"),
                "scheduler-agent", "97656185", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT--663833459",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--131863702",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-97656185"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "scheduler-agent-239208485",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--131863702"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "scheduler-agent--742746991",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-239208485"
         *           } ]
         *         }
         *       } ],
         *       "scheduledJobs" : [ {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-131863702",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--131863702"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "97656185",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-97656185"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "239208485",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent-239208485"
         *       }, {
         *         "agentName" : "scheduler-agent",
         *         "jobName" : "-742746991",
         *         "startupControlType" : "AUTOMATIC",
         *         "identifier" : "scheduler-agent--742746991"
         *       } ]
         *     }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--663833459"),
                "scheduler-agent", "-131863702", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--663833459"),
                "scheduler-agent", "239208485", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--663833459"),
                "scheduler-agent", "-742746991", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *     "name" : "CONTEXT-1616674532",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent--764230802",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent--764230802_ScheduledJob_15:25:00"
         *         } ]
         *       }
         *     }, {
         *       "jobIdentifier" : "scheduler-agent-2090738162",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-2090738162_ScheduledJob_16:30:00"
         *         } ]
         *       }
         *     }, {
         *       "jobIdentifier" : "scheduler-agent-241430090",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-241430090_ScheduledJob_16:30:00"
         *         } ]
         *       }
         *     } ],
         *     "scheduledJobs" : [ {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-764230802",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--764230802"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-764230802_ScheduledJob_15:25:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--764230802_ScheduledJob_15:25:00"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "2090738162",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-2090738162"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "2090738162_ScheduledJob_16:30:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-2090738162_ScheduledJob_16:30:00"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "241430090",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-241430090"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "241430090_ScheduledJob_16:30:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-241430090_ScheduledJob_16:30:00"
         *     } ]
         *   }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "-764230802_ScheduledJob_15:25:00", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "-764230802", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "2090738162_ScheduledJob_16:30:00", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "2090738162", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "241430090_ScheduledJob_16:30:00", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1616674532"),
                "scheduler-agent", "241430090", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *     "name" : "CONTEXT-1182789416",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent-97656185",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-210659119"
         *         }, {
         *           "identifier" : "scheduler-agent-2014644399"
         *         }, {
         *           "identifier" : "scheduler-agent--1758465897"
         *         }, {
         *           "identifier" : "scheduler-agent--148873498"
         *         } ]
         *       }
         *     }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789416"),
                "scheduler-agent", "-148873498", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789416"),
                "scheduler-agent", "-1758465897", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789416"),
                "scheduler-agent", "2014644399", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789416"),
                "scheduler-agent", "210659119", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1182789416", "CONTEXT-613708632"),
                "scheduler-agent", "97656185", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT-613708632",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--131863702",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-97656185"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "scheduler-agent-1720807104",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--131863702"
         *           } ]
         *         }
         *       }, {
         *         "jobIdentifier" : "scheduler-agent-1836346836",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-1720807104"
         *           } ]
         *         }
         *       }
         */

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-613708632"),
                "scheduler-agent", "-131863702", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-613708632"),
                "scheduler-agent", "1720807104", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-613708632"),
                "scheduler-agent", "1836346836", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);


        /**
         * {
         *       "name" : "CONTEXT-521366615",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1164721449",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-290005873"
         *           }, {
         *             "identifier" : "scheduler-agent-290005874"
         *           }, {
         *             "identifier" : "scheduler-agent-1581813691"
         *           }, {
         *             "identifier" : "scheduler-agent-1581813692"
         *           }, {
         *             "identifier" : "scheduler-agent-748080832"
         *           }, {
         *             "identifier" : "scheduler-agent-748080833"
         *           }, {
         *             "identifier" : "scheduler-agent-340732842"
         *           }, {
         *             "identifier" : "scheduler-agent-340732843"
         *           } ]
         *         }
         *       }
         */

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "290005873", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "290005874", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "1581813691", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "1581813692", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "748080832", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "748080833", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "340732842", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "340732843", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);


        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-521366615"),
                "scheduler-agent", "1164721449", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *         "jobIdentifier" : "scheduler-agent-1226061027",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--1613104257"
         *           }, {
         *             "identifier" : "scheduler-agent--847515063"
         *           }, {
         *             "identifier" : "scheduler-agent--1157186056"
         *           }, {
         *             "identifier" : "scheduler-agent-99102350"
         *           } ]
         *         }
         *       }
         */

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1789550425"),
                "scheduler-agent", "99102350", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1789550425"),
                "scheduler-agent", "-1157186056", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1789550425"),
                "scheduler-agent", "-847515063", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1789550425"),
                "scheduler-agent", "-1613104257", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1789550425"),
                "scheduler-agent", "1226061027", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);
        
        /**
         * {
         *       "name" : "CONTEXT--305614098",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1228709486",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-1226061027"
         *           } ]
         *         }
         *       }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--305614098"),
                "scheduler-agent", "1226061027", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--305614098"),
                "scheduler-agent", "1228709486", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT--918631717",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1164721449",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--1349296895"
         *           }, {
         *             "identifier" : "scheduler-agent--1349296894"
         *           }, {
         *             "identifier" : "scheduler-agent--1028088288"
         *           }, {
         *             "identifier" : "scheduler-agent--1028088287"
         *           } ]
         *         }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--918631717"),
                "scheduler-agent", "-1349296895", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--918631717"),
                "scheduler-agent", "-1349296894", true).size());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--918631717"),
                "scheduler-agent", "-1028088288", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--918631717"),
                "scheduler-agent", "-1028088287", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--918631717"),
                "scheduler-agent", "1164721449", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT-1065418539",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--213937305",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-2072336655"
         *           }, {
         *             "identifier" : "scheduler-agent--1493805586"
         *           } ]
         *         }
         *       }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1065418539"),
                "scheduler-agent", "2072336655", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1065418539"),
                "scheduler-agent", "-1493805586", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT--1745612430",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--211288846",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--213937305"
         *           } ]
         *         }
         *       }
         */

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT-1065418539", "CONTEXT--1745612430"),
                "scheduler-agent", "-213937305", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1745612430"),
                "scheduler-agent", "-211288846", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *     "contexts" : [ {
         *       "name" : "CONTEXT--1250033421",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1164721449",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent--254494704"
         *           }, {
         *             "identifier" : "scheduler-agent--254494703"
         *           } ]
         *         }
         */
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1250033421"),
                "scheduler-agent", "-254494704", true).size());

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1250033421"),
                "scheduler-agent", "-254494703", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1250033421"),
                "scheduler-agent", "1164721449", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT--1543216829",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent-1651431039",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-1009789918"
         *           } ]
         *         }
         *       }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1543216829"),
                "scheduler-agent", "1009789918", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1543216829"),
                "scheduler-agent", "1651431039", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        /**
         * {
         *       "name" : "CONTEXT--1409548854",
         *       "jobDependencies" : [ {
         *         "jobIdentifier" : "scheduler-agent--98367158",
         *         "logicalGrouping" : {
         *           "and" : [ {
         *             "identifier" : "scheduler-agent-1651431039"
         *           } ]
         *         }
         *       }
         */
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1409548854"),
                "scheduler-agent", "1651431039", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.RUNNING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of("CONTEXT--1409548854"),
                "scheduler-agent", "-98367158", true).size());

        // confirm that context context states as expected
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1195088490", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.COMPLETE);

        /**
         * The orchestration is now complete!
         */

        validateAllLocksCleared();
    }

    @Test
    public void test_job_fires_with_empty_child_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        /**
         * name" : "CONTEXT-1616645609",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent-1799613995",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *         } ]
         *       }
         *     }, {
         *       "jobIdentifier" : "scheduler-agent--1352045846",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *         } ]
         *       }
         *     } ],
         *     "scheduledJobs" : [ {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *     } ]
         *   }
         */
        // Execute the first scheduled job that fires based on a cron expression.
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", List.of()
                ,"scheduler-agent", "1799613995_ScheduledJob_06:00", true).size());

        // Now check that the appropriate contexts are in the state that we expect.
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

        validateAllLocksCleared();
    }

    @Test
    public void test_job_fires_with_null_child_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        /**
         * name" : "CONTEXT-1616645609",
         *     "jobDependencies" : [ {
         *       "jobIdentifier" : "scheduler-agent-1799613995",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *         } ]
         *       }
         *     }, {
         *       "jobIdentifier" : "scheduler-agent--1352045846",
         *       "logicalGrouping" : {
         *         "and" : [ {
         *           "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *         } ]
         *       }
         *     } ],
         *     "scheduledJobs" : [ {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "1799613995_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent-1799613995_ScheduledJob_06:00"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846"
         *     }, {
         *       "agentName" : "scheduler-agent",
         *       "jobName" : "-1352045846_ScheduledJob_06:00",
         *       "startupControlType" : "AUTOMATIC",
         *       "identifier" : "scheduler-agent--1352045846_ScheduledJob_06:00"
         *     } ]
         *   }
         */
        // Execute the first scheduled job that fires based on a cron expression.
        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "CONTEXT-1436221681", null
                ,"scheduler-agent", "1799613995_ScheduledJob_06:00", true).size());

        // Now check that the appropriate contexts are in the state that we expect.
        this.assertContextStatus(contextMachine, "CONTEXT-1616645609", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1436221681", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CONTEXT-1848727981", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1209755884", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-774294372", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--2036736597", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1590773100", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--129403053", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1589183395", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--663833459", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1616674532", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1182789416", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-613708632", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--715116816", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-521366615", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1789550425", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--305614098", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-2139852148", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--918631717", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-1065418539", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT-195330380", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1250033421", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1543216829", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);
    }

    @Test
    public void test_context_with_parallel_jobs() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/SAMPLE_CONTEXT_PARALLEL_JOBS.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/SAMPLE_CONTEXT_PARALLEL_JOBS.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "STPMUR.GLOBAL_BATCH_DONE", true).size());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);


        Assert.assertEquals("LOG_MAINTENANCE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "STOP_ALL_MUREX", true).get(0).getJobName());
        Assert.assertEquals("UPDATE_UNIQUEIDs", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "LOG_MAINTENANCE", true).get(0).getJobName());
        Assert.assertEquals("START_ALL_MUREX", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "UPDATE_UNIQUEIDs", true).get(0).getJobName());
        Assert.assertEquals("STOP_ALL_WORKFLOWS", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "START_ALL_MUREX", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "STOP_ALL_WORKFLOWS", true);
        Assert.assertEquals("BSP_CLEAN_FBS", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("BSP_CLEAN_PPGT", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("ACG_HISTORY_PURGE", schedulerJobInitiationEvents.get(2).getJobName());
        Assert.assertEquals("PURGE_BULK_EVENTS", schedulerJobInitiationEvents.get(3).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "BSP_CLEAN_FBS", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "BSP_CLEAN_PPGT", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "ACG_HISTORY_PURGE", true).size());
        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_BULK_EVENTS", true);
        Assert.assertEquals("PURGE_JOB_LIST", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("PURGE_DM_AUDIT", schedulerJobInitiationEvents.get(1).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_JOB_LIST", true).size());

        Assert.assertEquals("MARKETDATA_PURGE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_DM_AUDIT", true).get(0).getJobName());

        Assert.assertEquals("PURGE_MXML_DATA", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "MARKETDATA_PURGE", true).get(0).getJobName());

        Assert.assertEquals("PURGE_BREACHOSP", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_MXML_DATA", true).get(0).getJobName());

        Assert.assertEquals("PURGE_LOSTDATA", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_BREACHOSP", true).get(0).getJobName());

        Assert.assertEquals("BF_MHI_PURGE_DT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "PURGE_LOSTDATA", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        Assert.assertEquals("BSP_BSK_PURGE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_MHI_PURGE_DT", true).get(0).getJobName());

        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BSP_BSK_PURGE", true);
        Assert.assertEquals("BF_BOND_POS", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("BF_EQUITY_POS", schedulerJobInitiationEvents.get(1).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_BOND_POS", true).size());
        Assert.assertEquals("TRADES_LOGICAL_PURGE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_EQUITY_POS", true).get(0).getJobName());
        Assert.assertEquals("LOG_PURGE_BOND_MAT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "TRADES_LOGICAL_PURGE", true).get(0).getJobName());
        Assert.assertEquals("LOG_PURGE_FUT_MAT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "LOG_PURGE_BOND_MAT", true).get(0).getJobName());
        Assert.assertEquals("START_ALL_WORKFLOWS", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "LOG_PURGE_FUT_MAT", true).get(0).getJobName());
        Assert.assertEquals("BSP_REM_PTF_NDE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "START_ALL_WORKFLOWS", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        Assert.assertEquals("WH_CLEANUP", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BSP_REM_PTF_NDE", true).get(0).getJobName());
        Assert.assertEquals("WH_STOP_ENGINES", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "WH_CLEANUP", true).get(0).getJobName());
        Assert.assertEquals("WH_REBUILD_PARALLEL", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "WH_STOP_ENGINES", true).get(0).getJobName());
        Assert.assertEquals("WH_MAINTENANCE_PAR", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "WH_REBUILD_PARALLEL", true).get(0).getJobName());
        Assert.assertEquals("WH_START_ENGINES", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "WH_MAINTENANCE_PAR", true).get(0).getJobName());
        Assert.assertEquals("BO_DLV_FIX_EOD", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "WH_START_ENGINES", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        Assert.assertEquals("STARTSTOP_LIVEBOOK", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BO_DLV_FIX_EOD", true).get(0).getJobName());
        Assert.assertEquals("START_LIVEBOOK_CORE", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "STARTSTOP_LIVEBOOK", true).get(0).getJobName());


        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "START_LIVEBOOK_CORE", true);
        Assert.assertEquals("CLEAN_RBPL_USD_T", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("CLEAN_RBPL_EUR_T", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("CLEAN_RBPL_GBP_T", schedulerJobInitiationEvents.get(2).getJobName());
        Assert.assertEquals("CLEAN_RBPL_XCCY_T", schedulerJobInitiationEvents.get(3).getJobName());
        Assert.assertEquals("CLEAN_RBPL_MGMT_T", schedulerJobInitiationEvents.get(4).getJobName());
        Assert.assertEquals("CLEAN_RBPL_FL_USD_T", schedulerJobInitiationEvents.get(5).getJobName());
        Assert.assertEquals("CLEAN_RBPL_FL_EUR_T", schedulerJobInitiationEvents.get(6).getJobName());
        Assert.assertEquals("CLEAN_RBPL_FL_GBP_T", schedulerJobInitiationEvents.get(7).getJobName());
        Assert.assertEquals("CLEAN_RBPL_FL_XCC_T", schedulerJobInitiationEvents.get(8).getJobName());
        Assert.assertEquals("CLEAN_RBPL_FL_MGM_T", schedulerJobInitiationEvents.get(9).getJobName());
        Assert.assertEquals("CLEAN_RBPL_USD_NY_T", schedulerJobInitiationEvents.get(10).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_USD_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_EUR_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_GBP_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_XCCY_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_MGMT_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_FL_USD_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_FL_EUR_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_FL_GBP_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_FL_XCC_T", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_FL_MGM_T", true).size());


        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "CLEAN_RBPL_USD_NY_T", true);
        Assert.assertEquals("NEWRUN_RBPL_EUR", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_FlashMGM", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_FlashUSD", schedulerJobInitiationEvents.get(2).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_FlashXCC", schedulerJobInitiationEvents.get(3).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_Flash_EU", schedulerJobInitiationEvents.get(4).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_Flash_GBP", schedulerJobInitiationEvents.get(5).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_GBP", schedulerJobInitiationEvents.get(6).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_MGMT", schedulerJobInitiationEvents.get(7).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_USD", schedulerJobInitiationEvents.get(8).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_USD_NY", schedulerJobInitiationEvents.get(9).getJobName());
        Assert.assertEquals("NEWRUN_RBPL_XCCY", schedulerJobInitiationEvents.get(10).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_EUR", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_FlashMGM", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_FlashUSD", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_FlashXCC", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_Flash_EU", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_Flash_GBP", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_GBP", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_MGMT", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_USD", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_USD_NY", true).size());

        Assert.assertEquals("START_LIVEBOOK_BOOKS", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "NEWRUN_RBPL_XCCY", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "START_LIVEBOOK_BOOKS", true);
        Assert.assertEquals("BF_SETTLED_POS", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("BF_MHI_LIQUIDATION", schedulerJobInitiationEvents.get(1).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.RUNNING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_SETTLED_POS", true).size());

        Assert.assertEquals("BF_POS_LIQUIDAT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_MHI_LIQUIDATION", true).get(0).getJobName());

        Assert.assertEquals("BE_POS_LIQUIDAT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BF_POS_LIQUIDAT", true).get(0).getJobName());

        Assert.assertEquals("MXHC", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "BE_POS_LIQUIDAT", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.COMPLETE);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null,"scheduler-agent", "MXHC", true).size());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LOGICAL_PURGES", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "WEEKEND_LIVEBOOKS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "CLEAN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.COMPLETE);
    }

    @Test
    public void test_simple_context_with_parallel_jobs() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/ME_SIMPLE.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/ME_SIMPLE.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), jobLockCache, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);

        List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "SIMPLE_ME_ScheduledJob_06:01:00", true);
        Assert.assertEquals(3, schedulerJobInitiationEvents.size());
        Assert.assertEquals("BF_MHBK_FXSLLDW", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("PRC_SCF_DETAILS", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("BE_IPV_FBS_MHBK", schedulerJobInitiationEvents.get(2).getJobName());



        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        Assert.assertEquals("BF_MHBK_FXSLUSD", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MHBK_FXSLLDW", true).get(0).getJobName());
        Assert.assertEquals("BE_MHEU_SCF_COM", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "PRC_SCF_DETAILS", true).get(0).getJobName());
        Assert.assertEquals("BE_MHEU_AWV_LNB", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_IPV_FBS_MHBK", true).get(0).getJobName());
        Assert.assertEquals("BE_MHEU_SALES", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHEU_AWV_LNB", true).get(0).getJobName());
        Assert.assertEquals("BE_MHI_MIFID_RP", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHEU_SALES", true).get(0).getJobName());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHI_MIFID_RP", true).size());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        Assert.assertEquals("BF_MHBK_FXDLTCC", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MHBK_FXSLUSD", true).get(0).getJobName());
        Assert.assertEquals("BE_MHEU_SCF_INT", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHEU_SCF_COM", true).get(0).getJobName());

        Assert.assertEquals("BE_MHBK_FXSLLDW", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MHBK_FXDLTCC", true).get(0).getJobName());
        Assert.assertEquals("BE_MHI_EMIR_PFO", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHEU_SCF_INT", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHBK_FXSLLDW", true).size());
        Assert.assertEquals("BSP_IPV_ALL_TR", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_MHI_EMIR_PFO", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BSP_IPV_ALL_TR", true);

        Assert.assertEquals(5, schedulerJobInitiationEvents.size());
        Assert.assertEquals("BF_MD_CRDCRV_ME", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("BF_MHI_SIMCDCRV", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("BF_SIM_REPO_ME", schedulerJobInitiationEvents.get(2).getJobName());
        Assert.assertEquals("BF_MD_RTCRV_ME", schedulerJobInitiationEvents.get(3).getJobName());
        Assert.assertEquals("BF_SIM_FWD_ME", schedulerJobInitiationEvents.get(4).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MD_CRDCRV_ME", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MHI_SIMCDCRV", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_SIM_REPO_ME", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MD_RTCRV_ME", true).size());
        Assert.assertEquals("BE_REPO_INT_ME", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_SIM_FWD_ME", true).get(0).getJobName());

        schedulerJobInitiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BE_REPO_INT_ME", true);

        Assert.assertEquals(5, schedulerJobInitiationEvents.size());
        Assert.assertEquals("BF_MHBK_PLDEIPV", schedulerJobInitiationEvents.get(0).getJobName());
        Assert.assertEquals("BF_RTCRV_MHBK", schedulerJobInitiationEvents.get(1).getJobName());
        Assert.assertEquals("BF_SIMFIPV_MHBK", schedulerJobInitiationEvents.get(2).getJobName());
        Assert.assertEquals("BF_SIM_FWD_MHBK", schedulerJobInitiationEvents.get(3).getJobName());
        Assert.assertEquals("BF_SIM_REP_MHBK", schedulerJobInitiationEvents.get(4).getJobName());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_MHBK_PLDEIPV", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_RTCRV_MHBK", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_SIMFIPV_MHBK", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_SIM_FWD_MHBK", true).size());
        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BF_SIM_REP_MHBK", true).size());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.WAITING);

        Assert.assertEquals("BSP_MHIPLD_TMEC", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "SIMPLE_ME_COPY_ScheduledJob_09:30:00", true).get(0).getJobName());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.RUNNING);

        Assert.assertEquals("BSP_MHITPA_TMEC", this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BSP_MHIPLD_TMEC", true).get(0).getJobName());

        Assert.assertEquals(0, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "SIMPLE_MONTH_END", null,"scheduler-agent", "BSP_MHITPA_TMEC", true).size());

        this.assertContextStatus(contextMachine, "SIMPLE_MONTH_END", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "FX_SELLDOWN_SPOT_BOOKING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "AWV_REPORTING", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_PRU_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_FEEDERS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_PREP", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "IPV_MHBK_EXTRACTIONS", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "SIMPLE_ME_COPY", InstanceStatus.COMPLETE);

    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_reset_job_exception_due_to_job_not_complete_or_error() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.resetJob("agentName5-jobName5", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_reset_job_exception_job_not_in_context() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();
        contextMachine.resetJob("bad-job-name", "Context3");
    }

    @Test
    public void test_context_machine_reset_job_success() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl()
            , internalEventDrivenJobs, this.queueDir, new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService);
        contextMachine.init();

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);

        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        contextMachine.eventReceived(eventInstance);

        // Confirm the job is in a COMPLETE state
        this.assertJobStatus(contextMachine, "Context3", "agentName5-jobName5", InstanceStatus.COMPLETE);
        // And the context it resides in is in a COMPLETE state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        // Now reset the job
        contextMachine.resetJob("agentName5-jobName5", "Context3");

        // Confirm the job itself goes into WAITING state
        this.assertJobStatus(contextMachine, "Context3", "agentName5-jobName5", InstanceStatus.WAITING);
        // And the context into a RUNNING state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
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

    private void assertContextStatus(ContextMachine contextMachine, String context, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getContextStatus(context);
        Assert.assertEquals(expected, status);
    }

    private void assertJobStatus(ContextMachine contextMachine, String context, String jobName, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getJobStatus(context, jobName);
        Assert.assertEquals(expected, status);
    }
}
