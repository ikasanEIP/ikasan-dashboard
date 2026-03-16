package org.ikasan.job.orchestration.core.machine;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.job.orchestration.JobLockCacheServiceTestImpl;
import org.ikasan.job.orchestration.builder.context.JobLockBuilder;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.util.JobThreadFactory;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.ikasan.job.orchestration.core.machine.ContextMachineTestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextMachineTest extends AbstractTest {

    protected ContextService contextService = new ContextService();
    protected ObjectMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();
    protected String queueDir = "./target";

    protected ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();

    @Mock
    protected ScheduledContextService scheduledContextService;

    @Mock
    protected SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    protected JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    protected ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Mock
    protected SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    @Mock
    protected ModuleMetaDataService moduleMetadataService;

    @Mock
    protected BigQueueImpl inboundQueue;

    @Mock
    JobUtilsService jobUtilsService;

    @Mock
    ContextInstance mockContextInstance;

    @Mock
    ScheduledContextInstanceService scheduledContextInstanceService;

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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus())
            , JSONCompareMode.LENIENT);

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

    @Test
    public void test_context_machine_full_nested_context_job_in_context_error_reset_skipped_job_plan_ends_completed() throws IOException, JSONException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new GlobalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus())
            , JSONCompareMode.LENIENT);

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
            "agentName15", false);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        // reset the job!
        contextMachine.resetJob("agentName15-jobName15", "Context5");
        // skip the job
        contextMachine.skipJob("agentName15-jobName15", "Context5",  true);

        eventInstance = scheduledProcessEventInstance("jobName16",
            "agentName16", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

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
    public void test_context_machine_full_nested_context_final_job_in_context_error_reset_skipped_job_plan_ends_completed() throws IOException, JSONException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new GlobalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus())
            , JSONCompareMode.LENIENT);

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
            "agentName16", false);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        // reset the job!
        contextMachine.resetJob("agentName16-jobName16", "Context5");
        // skip the job
        contextMachine.skipJob("agentName16-jobName16", "Context5",  true);

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
    public void test_context_machine_ignore_quartz_scheduled_job() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        // remove agentName3-jobName3 from the internal event jobs
        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.remove("agentName3-jobName3");

        // add agentName3-jobName3 to the quartz event jobs
        QuartzScheduleDrivenJobInstance quartzScheduleDrivenJobInstance = new QuartzScheduleDrivenJobInstanceImpl();
        quartzScheduleDrivenJobInstance.setJobName("jobName3");
        quartzScheduleDrivenJobInstance.setAgentName("agentName3");
        quartzScheduleDrivenJobInstance.setIdentifier("agentName3-jobName3");

        Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap
            = Map.of("agentName3-jobName3", quartzScheduleDrivenJobInstance);


        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), quartzScheduleDrivenJobInstanceMap
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        // Disable the quartz driven jobs for the context
        contextMachine.disableQuartzBasedJobs();

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        eventInstance.setJobGroup("the job group");

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);

        // Assert no events raised as the quartz jobs are disabled.
        Assert.assertEquals(0, events.size());

        // Now enable the quartz jobs
        contextMachine.enableQuartzBasedJobs();

        events = contextMachine.eventReceived(eventInstance);

        // Assert that there is a event raised as the quartz jobs are now enabled
        Assert.assertEquals(1, events.size());
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_held_exception_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.holdJob("bad-job-identifier", "bad_context_name");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.holdJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_complete() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.COMPLETE);
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test
    public void test_context_machine_full_nested_context_job_release_exception_job_already_running() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RUNNING);
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");

        Assert.assertEquals(InstanceStatus.RUNNING, contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").getStatus());
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_released() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_in_error() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.ERROR);
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_full_nested_context_job_release_exception_job_already_skipped() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.SKIPPED);
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.releaseJob("agentName1-jobName1", "Context3");

        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3")
            .getScheduledJobsMap().get("agentName1-jobName1").setStatus(InstanceStatus.RELEASED);
    }

    @Test
    public void test_context_machine_hold_release_success_local_event_job() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new LocalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/context-with-local-jobs.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/context-with-local-jobs.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Hold the local event job
        contextMachine.holdJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        // the local event job that has been placed on hold resides in 2 contexts and is expected to be held in both.
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-start is held"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
            .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-start has a status of ON_HOLD"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.ON_HOLD));
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-end is held"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-end has a status of ON_HOLD"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.ON_HOLD));

        // Now release the local event job
        contextMachine.releaseJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        // the local event job that has been placed on hold resides in 2 contexts and is expected to be released in both and
        // in a WAITING state.
        Assert.assertFalse("LOCAL_EVENT_JOB-local-hold in context local-event-start is NOT held"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-start has a status of WAITING"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.WAITING));
        Assert.assertFalse("LOCAL_EVENT_JOB-local-hold in context local-event-end is NOT held"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-end has a status of WAITING"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.WAITING));
    }

    @Test
    public void test_context_machine_hold_job_catalyst_event_release_success_local_event_job() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new LocalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/context-with-local-jobs.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/context-with-local-jobs.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = creatLocalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), localEventJobInstanceMap, new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Hold the local event job
        contextMachine.holdJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        AtomicReference<String> jobName = new AtomicReference<>();
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> jobName.set(event.getJobName()));

        // the local event job that has been placed on hold resides in 2 contexts and is expected to be held in both.
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-start is held"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-start has a status of ON_HOLD"
            , ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.ON_HOLD));
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-end is held"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Assert.assertTrue("LOCAL_EVENT_JOB-local-hold in context local-event-end has a status of ON_HOLD"
            , ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.ON_HOLD));

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("fw-local",
            "scheduler-agent", true);
        eventInstance.setChildContextNames(List.of("local-event-start"));
        eventInstance.setJobStarting(false);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        // Now release the local event job
        contextMachine.releaseJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        // the local event job that has been placed on hold resides in 2 contexts and is expected to be released in both and
        // in a COMPLETE state due to the upstream event having run.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() ->
            !ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() ->
            ContextHelper.getChildContextInstance("local-event-start", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.COMPLETE));
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() ->
            !ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").isHeld());
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() ->
            ContextHelper.getChildContextInstance("local-event-end", contextMachine.getContext())
                .getScheduledJobsMap().get("LOCAL_EVENT_JOB-local-hold").getStatus().equals(InstanceStatus.COMPLETE));


        // Make sure the downstream job is initialised which indicates an event was raised to tell it to run!
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() ->
            jobName.get().equals("fw-hold"));
    }

    @Test
    public void test_context_machine_full_nested_context_skip_jobs_in_child_context() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJobs("Context3", true);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_jobs_in_child_context() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJobs("Context3", true);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        contextMachine.skipJobs("Context3", false);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_skip_and_enable_jobs_prepared_context_remains_prepared() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.setStatus(InstanceStatus.PREPARED);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJobs("Context3", true);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        contextMachine.skipJobs("Context3", false);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        Assert.assertTrue("Instance status is PREPARED!" ,contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED));
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_jobs_that_are_already_complete_in_child_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.COMPLETE);
        });

        contextMachine.skipJobs("Context3", true);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        contextMachine.skipJobs("Context3", false);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_enable_jobs_that_are_already_skipped_complete_in_child_context() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.SKIPPED_COMPLETE);
        });

        contextMachine.skipJobs("Context3", true);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.SKIPPED_COMPLETE, job.getStatus());
        });

        contextMachine.skipJobs("Context3", false);

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });
    }

    @Test
    public void test_context_machine_job_skipped_not_run_downstream_if_preceding_jobs_not_complete() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/test-skipped-job.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/test-skipped-job.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("test-cluster2", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("cluster-child", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("skipped-job-bug", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJob("scheduler-agent-sched-cmd1", "skipped-job-bug", true);

        this.assertJobStatus(contextMachine, "skipped-job-bug",  "scheduler-agent-sched-cmd1", InstanceStatus.SKIPPED);

        ContextualisedScheduledProcessEvent eventInstance
            = scheduledProcessEventInstance("sched-ski1", "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("schedskip2", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance
            = scheduledProcessEventInstance(events.get(0).getJobName(), events.get(0).getAgentName(), true);
        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        this.assertJobStatus(contextMachine, "skipped-job-bug",  "scheduler-agent-sched-cmd1", InstanceStatus.SKIPPED_COMPLETE);

        eventInstance
            = scheduledProcessEventInstance("sched-cmd3", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("sched-skip4", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance
            = scheduledProcessEventInstance(events.get(0).getJobName(), events.get(0).getAgentName(), true);
        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        this.assertJobStatus(contextMachine, "skipped-job-bug",  "scheduler-agent-sched-cmd2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "skipped-job-bug", InstanceStatus.COMPLETE);
    }

    @Test
    public void test_context_machine_job_skipped_not_run_downstream_if_preceding_jobs_already_complete() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/test-skipped-job.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/test-skipped-job.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("test-cluster2", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("cluster-child", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("skipped-job-bug", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextualisedScheduledProcessEvent eventInstance
            = scheduledProcessEventInstance("sched-ski1", "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("sched-cmd3", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("sched-skip4", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());


        contextMachine.skipJob("scheduler-agent-sched-cmd1", "skipped-job-bug", true);

        eventInstance
            = scheduledProcessEventInstance("schedskip2", "scheduler-agent", true);

        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance
            = scheduledProcessEventInstance(events.get(0).getJobName(), events.get(0).getAgentName(), true);
        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance
            = scheduledProcessEventInstance(events.get(0).getJobName(), events.get(0).getAgentName(), true);
        events =  contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        this.assertJobStatus(contextMachine, "skipped-job-bug",  "scheduler-agent-sched-cmd1", InstanceStatus.SKIPPED_COMPLETE);
        this.assertJobStatus(contextMachine, "skipped-job-bug",  "scheduler-agent-sched-cmd2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "skipped-job-bug", InstanceStatus.COMPLETE);
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertTrue(job.getStatus().equals(InstanceStatus.SKIPPED)
                || job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE));
        });

        contextMachine.skipJobs("Context2", false);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.WAITING, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_that_are_already_complete_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.COMPLETE);
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.COMPLETE);
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.COMPLETE);
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        contextMachine.skipJobs("Context2", false);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_that_are_already_running_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.RUNNING);
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.RUNNING);
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.RUNNING);
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });

        contextMachine.skipJobs("Context2", false);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.RUNNING, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_that_are_already_error_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ERROR);
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ERROR);
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ERROR);
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });

        contextMachine.skipJobs("Context2", false);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ERROR, job.getStatus());
        });
    }

    @Test
    public void test_context_machine_full_nested_context_skip_and_enable_that_are_already_held_jobs_in_child_context_that_has_nested_children_contexts() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextInstance context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ON_HOLD);
        });

        ContextInstance context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ON_HOLD);
        });

        ContextInstance context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            job.setStatus(InstanceStatus.ON_HOLD);
        });

        contextMachine.skipJobs("Context2", true);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });

        contextMachine.skipJobs("Context2", false);

        context4 = (ContextInstance) ContextHelper.getChildContext("Context4", contextMachine.getContext());
        context4.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });

        context3 = (ContextInstance) ContextHelper.getChildContext("Context3", contextMachine.getContext());
        context3.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });

        context2 = (ContextInstance) ContextHelper.getChildContext("Context2", contextMachine.getContext());
        context2.getScheduledJobs().forEach(job -> {
            Assert.assertEquals(InstanceStatus.ON_HOLD, job.getStatus());
        });
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_release_bad_job_when_others_on_hold_exception() throws IOException, JSONException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());


        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-same-job-multiple-contexts.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-same-job-multiple-contexts.json"));

        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.values().forEach(job -> job.setTargetResidingContextOnly(true));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService
            , this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
    public void test_context_machine_exception_missing_jobs() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ObjectMapper objectMapper = new ObjectMapper();

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-missing-jobs.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-missing-jobs.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        Thread.sleep(1000);

        String outboundQueueName = "outbound-" + contextInstance.getId() + "-queue";
        String inboundQueueName = "inbound-" + contextInstance.getId() + "-queue";

        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));

        IBigQueue bigQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "inboundQueue");

        // Assert that the big message causing the ContextMachineException has been dequeued.
        Assert.assertEquals(0, bigQueue.size());

        contextMachine.teardown();

        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
    }

    @Test
    public void test_complex_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/CONTEXT-36916071.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-36916071.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);


        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
    public void test_get_context_status_error_acknowledged() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        InstanceStatus status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.WAITING, status);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);

        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName2",
            "agentName2", false);

        // delegate to the context machine which will also broadcast the status update
        contextMachine.acknowledgeSchedulerJobError(internalEventDrivenJobs.get("agentName2-jobName2-Context3"));

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
    }

    @Test
    public void test_error_then_resubmit_to_raise_downstream_events() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        eventInstance.getInternalEventDrivenJob().setChildContextName("Context3");
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        eventInstance.getInternalEventDrivenJob().setChildContextName("Context3");
        eventInstance.setRaisedDueToFailureResubmission(true);
        contextMachine.eventReceived(eventInstance);
    }

    @Test
    public void test_query_machine_to_determine_which_jobs_can_run() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        eventInstance.getInternalEventDrivenJob().setChildContextName("Context3");
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
    public void test_query_machine_to_determine_which_jobs_can_run_when_job_starts_job_in_another_child_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-with-job-initiating-event-in-next-child-context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-with-job-initiating-event-in-next-child-context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance(), contextParametersInstanceService
            , this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        Assert.assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("Context3", "agentName2-jobName2"));

        eventInstance = scheduledProcessEventInstance("jobName4",
            "agentName4", true);
        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
        Assert.assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("Context3", "agentName5-jobName5"));
        Assert.assertEquals(InstanceStatus.COMPLETE, contextMachine.getJobStatus("Context3", "agentName4-jobName4"));

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", false);
        contextMachine.eventReceived(eventInstance);

        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.ERROR, status);
        status = contextMachine.getContextStatus("Context4");
        Assert.assertEquals(InstanceStatus.WAITING, status);
        Assert.assertEquals(InstanceStatus.ERROR, contextMachine.getJobStatus("Context3", "agentName6-jobName6"));
        Assert.assertEquals(InstanceStatus.ERROR, contextMachine.getJobStatus("Context4", "agentName6-jobName6"));


        // We resubmit the job events but with raisedDueToFailureResubmission == true
        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", false);
        eventInstance.setRaisedDueToFailureResubmission(true);
        eventInstance.getInternalEventDrivenJob().setChildContextName("Context3");
        List<SchedulerJobInitiationEvent> events = contextMachine.getEventsThatCanRun(eventInstance);

        // We get the downstream jobs to execute as expected.
        Assert.assertNotNull(events);
        Assert.assertEquals(2, events.size());
        Assert.assertEquals("jobName7", events.get(0).getJobName());
        Assert.assertEquals("jobName8", events.get(1).getJobName());

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
        this.assertJobStatus(contextMachine, "Context3", "agentName6-jobName6", InstanceStatus.ERROR);
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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        InstanceStatus status = contextMachine.getContextStatus("NonExistentContext");

        Assert.assertNull(status);
    }

    @Test
    public void test_get_context() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance instance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, instance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        this.assertContextStatus(contextMachine, "CONTEXT--1745612430", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "CONTEXT--1409548854", InstanceStatus.WAITING);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        Assert.assertEquals(1, this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "PARALLEL_SAMPLE", null
                ,"scheduler-agent", "STPMUR.GLOBAL_BATCH_DONE", true).size());

        this.assertContextStatus(contextMachine, "PARALLEL_SAMPLE", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "MUREX_RESTART", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "WEEKLY_PURGES", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "ETF_BSKT_PURGE", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "WAREHOUSE_REBUILD", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "NEWRUN_RBPL", InstanceStatus.WAITING);
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
        this.assertContextStatus(contextMachine, "LIQ_MUREX_REC", InstanceStatus.WAITING);

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
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

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

    @Test
    public void test_context_with_final_parallel_jobs_that_has_error_job_reset_confirm_context_complete() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/job-plan-with-parallel-jobs-in-end.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/job-plan-with-parallel-jobs-in-end.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        JobLockCacheImpl jobLockCache = JobLockCacheImpl.instance();
        jobLockCache.setJobLockCacheService(new JobLockCacheServiceTestImpl());
        jobLockCache.addLocks(context.getAllNestedJobLocks(), context.getEnvironmentGroup());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, jobLockCache
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        List<SchedulerJobInitiationEvent> initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "scheduled", true);
        Assert.assertEquals(2, initiationEvents.size());

        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);

        Assert.assertEquals("demo-3", initiationEvents.get(0).getJobName());
        Assert.assertEquals("demo1", initiationEvents.get(1).getJobName());

        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo1", true);

        Assert.assertEquals(0, initiationEvents.size());

        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo-3", true);

        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);

        Assert.assertEquals(1, initiationEvents.size());

        Assert.assertEquals("demo-4", initiationEvents.get(0).getJobName());

        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo-4", true);

        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);

        Assert.assertEquals(1, initiationEvents.size());

        Assert.assertEquals("demo-6", initiationEvents.get(0).getJobName());

        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo-6", true);

        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-6", InstanceStatus.COMPLETE);

        Assert.assertEquals(2, initiationEvents.size());

        Assert.assertEquals("demo2", initiationEvents.get(0).getJobName());
        Assert.assertEquals("demo-7", initiationEvents.get(1).getJobName());

        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo2", true);

        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-6", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo2", InstanceStatus.COMPLETE);

        Assert.assertEquals(0, initiationEvents.size());

        // demo-7 job has an error
        initiationEvents = this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo-7", false);

        // Confirm statuses are reflected correctly
        this.assertContextStatus(contextMachine, "test", InstanceStatus.ERROR);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-6", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo2", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-7", InstanceStatus.ERROR);

        Assert.assertEquals(0, initiationEvents.size());

        // Reset the failed job.
        contextMachine.resetJob("scheduler-agent-demo-7", "test");

        // Confirm that the job plan is now in a running state and the errored job is now waiting.
        this.assertContextStatus(contextMachine, "test", InstanceStatus.RUNNING);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-6", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo2", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-7", InstanceStatus.WAITING);

        // Now successfully run the final job in the plans.
        this.sendScheduledEventToContextMachineWithChildContextId
            (contextMachine, "test", null
                ,"scheduler-agent", "" +
                    "demo-7", true);

        // Confirm the job plan is complete and all jobs within it.
        this.assertContextStatus(contextMachine, "test", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-scheduled", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo1", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-3", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-4", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-6", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo2", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "test", "scheduler-agent-demo-7", InstanceStatus.COMPLETE);
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_reset_job_exception_due_to_job_not_complete_or_error_or_waiting() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.getContextsMap().get("Context2").getContextsMap().get("Context3").getScheduledJobs()
            .forEach(schedulerJobInstance -> schedulerJobInstance.setStatus(InstanceStatus.RUNNING));
        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.resetJob("agentName5-jobName5", "Context3");
    }

    @Test(expected = ContextMachineException.class)
    public void test_context_machine_reset_job_exception_job_not_in_context() throws IOException, JSONException, InterruptedException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        contextMachine.resetJob("bad-job-name", "Context3");
    }

    @Test
    public void test_context_machine_reset_command_execution_job_success() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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

    @Test
    public void test_context_machine_hold_job_and_reset_preceding_command_execution_job_success() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new InternalEventDrivenJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextHelper.enrichJobs(contextInstance);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        contextMachine.holdJob("agentName6-jobName6", "Context3");

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

        Assert.assertEquals("Should receive no job initiation events, as job 6 on hold", 0, contextMachine.eventReceived(eventInstance).size());

        // Confirm the job is in a COMPLETE state
        this.assertJobStatus(contextMachine, "Context3", "agentName5-jobName5", InstanceStatus.COMPLETE);
        // Confirm the job is in a ON_HOLD state
        this.assertJobStatus(contextMachine, "Context3", "agentName6-jobName6", InstanceStatus.ON_HOLD);
        // And the context it resides in is in a COMPLETE state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        // Now reset the job
        contextMachine.resetJob("agentName5-jobName5", "Context3");

        contextMachine.releaseJob("agentName6-jobName6", "Context3");

        // Confirm the job itself goes into WAITING state
        this.assertJobStatus(contextMachine, "Context3", "agentName5-jobName5", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine, "Context3", "agentName6-jobName6", InstanceStatus.WAITING);
        // And the context into a RUNNING state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);

        eventInstance = scheduledProcessEventInstance("jobName5",
            "agentName5", true);

        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        contextMachine.eventReceived(eventInstance);

        this.assertJobStatus(contextMachine, "Context3", "agentName5-jobName5", InstanceStatus.COMPLETE);
        this.assertJobStatus(contextMachine, "Context3", "agentName6-jobName6", InstanceStatus.COMPLETE);
        // And the context into a RUNNING state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);
    }

    @Test
    public void test_context_machine_held_and_released_local_event_job() throws IOException, InvalidContextTemplateException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new LocalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/context-with-local-jobs.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/context-with-local-jobs.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = creatLocalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), localEventJobInstanceMap, new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Hold the local event job
        contextMachine.holdJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        this.assertJobStatus(contextMachine, "local-event-start", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.ON_HOLD);
        this.assertJobStatus(contextMachine, "local-event-end", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.ON_HOLD);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("fw-local",
            "scheduler-agent", true);
        eventInstance.setChildContextNames(List.of("local-event-start"));

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        contextMachine.releaseJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        this.assertJobStatus(contextMachine, "local-event-start", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine, "local-event-end", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.WAITING);

        // As local event has sent an event onto the outbound big queue.
        Assert.assertEquals(1, contextMachine.getOutboundQueue().size());

        // Double check the message on the outbound queue is correct and is local-hold.
        byte[] localEvent = contextMachine.getOutboundQueue().peek();

        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage = objectMapperTest.readValue(localEvent, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("local-hold", schedulerJobInitiationEvent.getJobName());
        Assert.assertEquals(JobConstants.LOCAL_EVENT_JOB, schedulerJobInitiationEvent.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(null);

        // Process the released job.
        eventInstance = scheduledProcessEventInstance("local-hold",
            JobConstants.LOCAL_EVENT_JOB, true);
        eventInstance.setChildContextNames(List.of("local-event-start", "local-event-end"));

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        // Confirm the local jobs are complete
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            this.assertJobStatus(contextMachine, "local-event-start", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.COMPLETE);
            this.assertJobStatus(contextMachine, "local-event-end", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.COMPLETE);
        });
    }

    @Test
    public void test_context_machine_reset_preceding_job_to_held_local_event_job() throws IOException, InvalidContextTemplateException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new LocalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/context-with-local-jobs.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/context-with-local-jobs.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = creatLocalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), localEventJobInstanceMap, new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Hold the local event job
        contextMachine.holdJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        this.assertJobStatus(contextMachine, "local-event-start", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.ON_HOLD);
        this.assertJobStatus(contextMachine, "local-event-end", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.ON_HOLD);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("fw-local",
            "scheduler-agent", true);
        eventInstance.setChildContextNames(List.of("local-event-start"));

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        contextMachine.resetJob("scheduler-agent-fw-local", "local-event-start");
        contextMachine.releaseJob("LOCAL_EVENT_JOB-local-hold", "local-event-start");

        this.assertJobStatus(contextMachine, "local-event-start", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.WAITING);
        this.assertJobStatus(contextMachine, "local-event-end", "LOCAL_EVENT_JOB-local-hold", InstanceStatus.WAITING);

        // Nothing sent to big queue as the job previous to the local event was reset.
        Assert.assertEquals(0, contextMachine.getOutboundQueue().size());
    }

    @Test
    public void test_context_machine_reset_bridging_job_success() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-with-bridging-job.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-with-bridging-job.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        Map<String, BridgingJobInstance> bridgingJobInstanceMap = loadBridgingJobInstanceMap(context, contextInstance);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), bridgingJobInstanceMap, this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
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
            "BRIDGING_JOB", true);

        contextMachine.eventReceived(eventInstance);

        eventInstance = scheduledProcessEventInstance("jobName6",
            "agentName6", true);

        contextMachine.eventReceived(eventInstance);

        // Confirm the job is in a COMPLETE state
        this.assertJobStatus(contextMachine, "Context3", "BRIDGING_JOB-jobName5", InstanceStatus.COMPLETE);
        // And the context it resides in is in a COMPLETE state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.COMPLETE, status);

        // Now reset the job
        contextMachine.resetJob("BRIDGING_JOB-jobName5", "Context3");

        // Confirm the job itself goes into WAITING state
        this.assertJobStatus(contextMachine, "Context3", "BRIDGING_JOB-jobName5", InstanceStatus.WAITING);
        // And the context into a RUNNING state
        status = contextMachine.getContextStatus("Context3");
        Assert.assertEquals(InstanceStatus.RUNNING, status);
    }

    @Test
    public void test_repeating_job_success_not_target_residing_context_only() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/repeating-job.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/repeating-job.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.values().forEach(job -> {
            job.setJobRepeatable(true);
            job.setTargetResidingContextOnly(false);
        });

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-1.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        // Now repeat the cron job a few times
        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_repeating_job_success_target_residing_context_only() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/repeating-job.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/repeating-job.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.values().forEach(job -> job.setJobRepeatable(true));

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-1.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        // Now repeat the cron job a few times
        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-status-2.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_repeating_jobs_with_bridging_job_connecting_success_not_target_residing_context_only() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/contexts/repeating-job-with-bridging.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/contexts/repeating-job-with-bridging.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.values().forEach(job -> {
            job.setJobRepeatable(true);
            job.setTargetResidingContextOnly(false);
        });

        Map<String, BridgingJobInstance> bridgingJobInstanceMap = creatBridgingMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), bridgingJobInstanceMap, this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("jobName5",
            "BRIDGING_JOB", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("BE_MHI_EMIR_PFO_2", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO_2",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());


        // Now repeat the cron job a few times
        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        // Bridging job repeats
        eventInstance = scheduledProcessEventInstance("jobName5",
            "BRIDGING_JOB", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("BE_MHI_EMIR_PFO_2", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO_2",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-with-bridging-job-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        // Bridging job repeats
        eventInstance = scheduledProcessEventInstance("jobName5",
            "BRIDGING_JOB", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("BE_MHI_EMIR_PFO_2", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO_2",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-with-bridging-job-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);

        eventInstance = scheduledProcessEventInstance("SIMPLE_ME_ScheduledJob_06:01:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        // Bridging job repeats
        eventInstance = scheduledProcessEventInstance("jobName5",
            "BRIDGING_JOB", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("BE_MHI_EMIR_PFO_2", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("BE_MHI_EMIR_PFO_2",
            "scheduler-agent", true, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        JSONAssert.assertEquals(loadDataFile("/data/machine/result/repeating-job-with-bridging-job-status.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()), JSONCompareMode.LENIENT);
    }

    /**
     * This test evaluates a simple dependency where by jobName2 is a Global Event within a Context Machine
     * This is to test that jobName1 see and is able to raise a global event, jobName2, and then when the global event
     * is raised to successful that it can kick off jobName3 which is a internal job.
     *      agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     */
    @Test
    public void test_simple_context_chained_jobs_with_global_events() throws IOException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));

        this.contextTemplateValidator.validate(context);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(JobConstants.GLOBAL_EVENT, events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", JobConstants.GLOBAL_EVENT, true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());
        
        // Now check that all Jobs within the context instance is Complete
        contextInstance.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });
    }

    /**
     * This test evaluates a simple dependency where by jobName2 is a Global Event within a Context Machine
     * This is to test that jobName1 see and is able to raise a global event, jobName2, and then when the global event
     * is raised to successful that it can kick off jobName3 which is a internal job.
     *      agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     */
    @Test
    public void test_simple_context_chained_jobs_with_global_events_skip_job() throws IOException, InvalidContextTemplateException {
        when(this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(any(), any(), any()))
            .thenReturn(this.schedulerJobInstanceRecord);
        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance())
            .thenReturn(new GlobalEventJobInstanceImpl());

        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));

        this.contextTemplateValidator.validate(context);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalJob1.setIdentifier("agentName1-jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalJob3.setIdentifier("agentName3-jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalJob2.setIdentifier("agentName2-jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        contextMachine.skipJob("GLOBAL_EVENT-jobName2", "Context1", true);
        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(JobConstants.GLOBAL_EVENT, events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());
        Assert.assertEquals(true, events.get(0).isSkipped());

        eventInstance
            = scheduledProcessEventInstance("jobName2", JobConstants.GLOBAL_EVENT, true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(0, events.size());

        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("agentName1-jobName1").getStatus());
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("agentName3-jobName3").getStatus());
        Assert.assertEquals(InstanceStatus.SKIPPED_COMPLETE, contextInstance.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
    }

    /**
     * This test is based on the test - test_global_events_through_context_machine_via_big_queue_single_context
     * This test differs as we now have two context running.
     * The two Context are defined as 
     * 
     * Context1
     *      agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     * 
     * Context1-Two-Jobs
     *      agentNameTwoJobs-jobName2 --> agentNameTwoJobs-jobName3
     * 
     * jobName2 are defined as Global Events.
     * 
     * Expectation is that Context1 will raised the a global event when agentName1-jobName1 is successful.
     * This globalEvent is jobName2 and a successful message will be sent via the ContextMachine to Context1 and Context1-TwoJobs.
     * 
     * Context1 should then raised agentName3-jobName3 to run
     * Context1-TwoJobs should then raised agentNameTwoJobs-jobName3 to run.
     *
     * @throws IOException
     * @throws InvalidContextTemplateException
     */
    @Test
    public void test_global_events_through_context_machine_via_big_queue_two_context_running() throws IOException, InvalidContextTemplateException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        //Context1 the main initiator
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));

        // Context1-Two-Jobs - raised by a global event.
        ContextTemplate context2 = this.contextService.getContextTemplate(loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json"));
        ContextInstance contextInstance2 = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json"));
        
        this.contextTemplateValidator.validate(context);
        this.contextTemplateValidator.validate(context2);

        /* START setup for Context1 */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());
        /* END setup for Context1 */

        /* START setup for Context1-Two-Jobs */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJobC2 = new InternalEventDrivenJobInstanceImpl();
        internalJobC2.setAgentName("agentNameTwoJobs");
        internalJobC2.setJobName("jobName3");
        internalEventDrivenJobs2.put("agentNameTwoJobs-jobName3-Context1-Two-Jobs", internalJobC2);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances2 = new HashMap<>();
        GlobalEventJobInstanceImpl globalJobC2 = new GlobalEventJobInstanceImpl();
        globalJobC2.setAgentName("agentNameTwoJobs");
        globalJobC2.setJobName("jobName2");
        globalEventJobInstances2.put("GLOBAL_EVENT-jobName2-Context1-Two-Jobs", globalJobC2);

        ContextMachine contextMachine2 = new ContextMachine(context2, contextInstance2, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances2, new HashMap<>()
            , internalEventDrivenJobs2, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine2.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine2);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine2.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());
        /* END setup for Context1-Two-Jobs */
        
        /**** START TESTING Context1 ****/
        
        // Agent sends jobName1 as success. jobName1 is a Internal job
        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));
        
        // Test to make sure that jobName2 is now success. This means that jobName1 event has triggered jobName2 to run.
        // jobName2 is a Global Event, this event will not be sent to the agent, the Context machine will orchestrate which is why
        // quickly after the event for jobName1 is sent, jobName2 event should be sent on the BigQueue and the ContextMachine will process
        // it to success. Wait for 5 seconds for it to change.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> { 
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
        });

        // As jobName2 has sent an event onto the outbound big queue. This should be jobName3
        Assert.assertEquals(contextMachine.getOutboundQueue().size(), 1);
        
        // Double check the message on the outbound queue is correct and is jobName3.
        byte[] jobName3Event = contextMachine.getOutboundQueue().peek();
        
        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage = objectMapperTest.readValue(jobName3Event, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("jobName3", schedulerJobInitiationEvent.getJobName());
        Assert.assertEquals("agentName3", schedulerJobInitiationEvent.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(null);
        
        // Agent sends jobName3 as success. jobName3 is a Internal job
        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));
        
        // Now wait until jobName 3 is Success
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("agentName3-jobName3").getStatus());
        });

        // Now check that all Jobs within the context instance is Complete
        contextInstance.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });

        // Make sure the context is now fully complete
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getStatus());
        /**** END TESTING Context1 ****/

        /**** START TESTING Context1-Two-Jobs ****/
        // As we were testing Context1, Context1-Two-Jobs was running in the background, therefore jobName2 should be in COMPLETED status
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> { 
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
        });

        // As jobName2 has sent an event onto the outbound big queue. This should be jobName3
        Assert.assertEquals(contextMachine2.getOutboundQueue().size(), 1);

        // Double check the message on the outbound queue is correct and is jobName3.
        byte[] jobName3Event2 = contextMachine2.getOutboundQueue().peek();

        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage2 = objectMapperTest.readValue(jobName3Event2, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString2 = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage2.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent2
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString2, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("jobName3", schedulerJobInitiationEvent2.getJobName());
        Assert.assertEquals("agentNameTwoJobs", schedulerJobInitiationEvent2.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine2.setSchedulerJobInitiationEventRaisedListener(null);


        // Agent sends jobName3 as success. jobName3 is a Internal job
        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentNameTwoJobs", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine2.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Now wait until jobName 3 is Success
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getScheduledJobsMap().get("agentNameTwoJobs-jobName3").getStatus());
        });

        // Now check that all Jobs within the context instance is Complete
        contextInstance2.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });

        // Make sure the context is now fully complete
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getStatus());
        /**** END TESTING Context1-Two-Jobs ****/
        
        String outboundQueueName = "outbound-" + contextInstance.getId() + "-queue";
        String inboundQueueName = "inbound-" + contextInstance.getId() + "-queue";
        String outboundQueueName2 = "outbound-" + contextInstance2.getId() + "-queue";
        String inboundQueueName2 = "inbound-" + contextInstance2.getId() + "-queue";

        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName2)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName2)));

        ContextMachineCache.instance().remove(contextMachine);
        ContextMachineCache.instance().remove(contextMachine2);
        contextMachine.teardown();
        contextMachine2.teardown();

        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName2)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName2)));
    }

    /**
     * This test evaluates a simple dependency where by jobName2 is a Global Event within a Context Machine
     * This is to test that jobName1 see and is able to raise a global event, jobName2, and then when the global event
     * is raised to successful that it can kick off jobName3 which is a internal job.
     * This test is to be run through the Context Machine. JobName1 and JobName3 success events will be simulated by creating and injecting events (as the agent does this)
     * However jobName2 will be done by the ContextMachine
     *     agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     * @throws IOException
     * @throws InvalidContextTemplateException
     */
    @Test
    public void test_global_events_through_context_machine_via_big_queue_single_context() throws IOException, InvalidContextTemplateException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json"));

        this.contextTemplateValidator.validate(context);

        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());

        // Agent sends jobName1 as success. jobName1 is a Internal job
        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Test to make sure that jobName2 is now success. This means that jobName1 event has triggered jobName2 to run.
        // jobName2 is a Global Event, this event will not be sent to the agent, the Context machine will orchestrate which is why
        // quickly after the event for jobName1 is sent, jobName2 event should be sent on the BigQueue and the ContextMachine will process
        // it to success. Wait for 5 seconds for it to change.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
        });

        // As jobName2 has sent an event onto the outbound big queue. This should be jobName3
        Assert.assertEquals(contextMachine.getOutboundQueue().size(), 1);

        // Double check the message on the outbound queue is correct and is jobName3.
        byte[] jobName3Event = contextMachine.getOutboundQueue().peek();

        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage = objectMapperTest.readValue(jobName3Event, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("jobName3", schedulerJobInitiationEvent.getJobName());
        Assert.assertEquals("agentName3", schedulerJobInitiationEvent.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(null);

        // Agent sends jobName3 as success. jobName3 is a Internal job
        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Now wait until jobName 3 is Success
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("agentName3-jobName3").getStatus());
        });

        // Now check that all Jobs within the context instance is Complete
        contextInstance.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });

        // Make sure the context is now fully complete
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getStatus());

        String outboundQueueName = "outbound-" + contextInstance.getId() + "-queue";
        String inboundQueueName = "inbound-" + contextInstance.getId() + "-queue";

        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));

        ContextMachineCache.instance().remove(contextMachine);
        contextMachine.teardown();

        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
    }

    /**
     * This test is based on the test - test_global_events_through_context_machine_via_big_queue_three_context_running_send_to_two
     * However we have three contexts running which has global events, however the context machine will only send it to two of the contexts
     * based on the environmentGroup defined on the context
     *
     * The three Context are defined as 
     *
     * Context1 (environmentGroup=GRP1)
     *      agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     *
     * Context1-Two-Jobs (environmentGroup=GRP1)
     *      agentNameTwoJobs-jobName2 --> agentNameTwoJobs-jobName3
     *
     * Context2-Two-Jobs (environmentGroup=null)
     *      agentNameTwoJobsNull-jobName2 --> agentNameTwoJobsNull-jobName3
     * 
     * jobName2 are defined as Global Events.
     *
     * Expectation is that Context1 will raised the a global event when agentName1-jobName1 is successful.
     * This globalEvent is jobName2 and a successful message will be sent via the ContextMachine to Context1 and Context1-TwoJobs.
     * This will not be sent to Context2-TwoJobs
     *
     * Context1 should then raised agentName3-jobName3 to run
     * Context1-TwoJobs should then raised agentNameTwoJobs-jobName3 to run.
     * Context2-TwoJobs should do nothing as it isn't part of the environmentGroup GRP1
     *
     * @throws IOException
     * @throws InvalidContextTemplateException
     */
    @Test
    public void test_global_events_through_context_machine_via_big_queue_three_context_running_send_to_two() throws IOException, InvalidContextTemplateException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson = loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json");

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson2 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json");

        // modify the context descriptor to change the context name and agent name as we reusing the same descriptor.
        String contextJson3 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("Context1-Two-Jobs","Context2-Two-Jobs")
            .replaceAll("agentNameTwoJobs", "agentNameTwoJobsNull");
        
        //Context1 the main initiator - environmentGroup = GRP1
        ContextTemplate context = this.contextService.getContextTemplate(contextJson);
        context.setEnvironmentGroup("GRP1");
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);
        contextInstance.setEnvironmentGroup("GRP1");

        // Context1-Two-Jobs - environmentGroup = GRP1
        ContextTemplate context2 = this.contextService.getContextTemplate(contextJson2);
        context2.setEnvironmentGroup("GRP1");
        ContextInstance contextInstance2 = this.contextService.getContextInstance(contextJson2);
        contextInstance2.setEnvironmentGroup("GRP1");

        // Context2-Two-Jobs - environmentGroup = null
        ContextTemplate context3 = this.contextService.getContextTemplate(contextJson3);
        context3.setEnvironmentGroup("environment");
        ContextInstance contextInstance3 = this.contextService.getContextInstance(contextJson3);
        contextInstance3.setEnvironmentGroup("environment");

        this.contextTemplateValidator.validate(context);
        this.contextTemplateValidator.validate(context2);
        this.contextTemplateValidator.validate(context3);

        /* START setup for Context1 */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());
        /* END setup for Context1 */

        /* START setup for Context1-Two-Jobs */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs2 = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJobC2 = new InternalEventDrivenJobInstanceImpl();
        internalJobC2.setAgentName("agentNameTwoJobs");
        internalJobC2.setJobName("jobName3");
        internalEventDrivenJobs2.put("agentNameTwoJobs-jobName3-Context1-Two-Jobs", internalJobC2);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances2 = new HashMap<>();
        GlobalEventJobInstanceImpl globalJobC2 = new GlobalEventJobInstanceImpl();
        globalJobC2.setAgentName("agentNameTwoJobs");
        globalJobC2.setJobName("jobName2");
        globalEventJobInstances2.put("GLOBAL_EVENT-jobName2-Context1-Two-Jobs", globalJobC2);

        ContextMachine contextMachine2 = new ContextMachine(context2, contextInstance2, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances2, new HashMap<>()
            , internalEventDrivenJobs2, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine2.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine2);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine2.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());
        /* END setup for Context1-Two-Jobs */

        /* START setup for Context2-Two-Jobs */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs3 = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJobC3 = new InternalEventDrivenJobInstanceImpl();
        internalJobC3.setAgentName("agentNameTwoJobsNull");
        internalJobC3.setJobName("jobName3");
        internalEventDrivenJobs3.put("agentNameTwoJobsNull-jobName3-Context2-Two-Jobs", internalJobC3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances3 = new HashMap<>();
        GlobalEventJobInstanceImpl globalJobC3 = new GlobalEventJobInstanceImpl();
        globalJobC3.setAgentName("agentNameTwoJobsNull");
        globalJobC3.setJobName("jobName2");
        globalEventJobInstances3.put("GLOBAL_EVENT-jobName2-Context2-Two-Jobs", globalJobC3);

        ContextMachine contextMachine3 = new ContextMachine(context3, contextInstance3, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances3, new HashMap<>()
            , internalEventDrivenJobs3, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine3.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().put(contextMachine3);

        // The below is used to stop the outbound bigQueue from consuming, this is so we can interrogate the queue within the test
        contextMachine3.setSchedulerJobInitiationEventRaisedListener(new SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue());
        /* END setup for Context2-Two-Jobs */

        /**** START TESTING Context1 ****/

        // Agent sends jobName1 as success. jobName1 is a Internal job
        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName1",
            "agentName1", true);
        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Test to make sure that jobName2 is now success. This means that jobName1 event has triggered jobName2 to run.
        // jobName2 is a Global Event, this event will not be sent to the agent, the Context machine will orchestrate which is why
        // quickly after the event for jobName1 is sent, jobName2 event should be sent on the BigQueue and the ContextMachine will process
        // it to success. Wait for 5 seconds for it to change.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
        });

        // As jobName2 has sent an event onto the outbound big queue. This should be jobName3
        Assert.assertEquals(contextMachine.getOutboundQueue().size(), 1);

        // Double check the message on the outbound queue is correct and is jobName3.
        byte[] jobName3Event = contextMachine.getOutboundQueue().peek();

        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage = objectMapperTest.readValue(jobName3Event, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("jobName3", schedulerJobInitiationEvent.getJobName());
        Assert.assertEquals("agentName3", schedulerJobInitiationEvent.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(null);

        // Agent sends jobName3 as success. jobName3 is a Internal job
        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Now wait until jobName 3 is Success
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getScheduledJobsMap().get("agentName3-jobName3").getStatus());
        });

        // Now check that all Jobs within the context instance is Complete
        contextInstance.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });

        // Make sure the context is now fully complete
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance.getStatus());
        /**** END TESTING Context1 ****/

        /**** START TESTING Context1-Two-Jobs ****/
        // As we were testing Context1, Context1-Two-Jobs was running in the background, therefore jobName2 should be in COMPLETED status
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());
        });

        // As jobName2 has sent an event onto the outbound big queue. This should be jobName3
        Assert.assertEquals(contextMachine2.getOutboundQueue().size(), 1);

        // Double check the message on the outbound queue is correct and is jobName3.
        byte[] jobName3Event2 = contextMachine2.getOutboundQueue().peek();

        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage2 = objectMapperTest.readValue(jobName3Event2, BigQueueMessageImpl.class);
        String outgoingBigQueueMessageAsString2 = new String(objectMapperTest.writeValueAsBytes(outgoingBigQueueMessage2.getMessage()));
        SchedulerJobInitiationEvent schedulerJobInitiationEvent2
            = objectMapperTest.readValue(outgoingBigQueueMessageAsString2, SchedulerJobInitiationEventImpl.class);
        Assert.assertEquals("jobName3", schedulerJobInitiationEvent2.getJobName());
        Assert.assertEquals("agentNameTwoJobs", schedulerJobInitiationEvent2.getAgentName());

        // As we have the messages on the outbound big queue, re-enable the outbound queue.
        contextMachine2.setSchedulerJobInitiationEventRaisedListener(null);
        
        // Agent sends jobName3 as success. jobName3 is a Internal job
        eventInstance = scheduledProcessEventInstance("jobName3",
            "agentNameTwoJobs", true);
        bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapperTest.writeValueAsString(eventInstance)).build();
        contextMachine2.eventReceived(objectMapperTest.writeValueAsString(bigQueueMessage));

        // Now wait until jobName 3 is Success
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getScheduledJobsMap().get("agentNameTwoJobs-jobName3").getStatus());
        });

        // Now check that all Jobs within the context instance is Complete
        contextInstance2.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.COMPLETE, jobs.getStatus());
        });

        // Make sure the context is now fully complete
        Assert.assertEquals(InstanceStatus.COMPLETE, contextInstance2.getStatus());
        /**** END TESTING Context1-Two-Jobs ****/

        /**** START TESTING Context2-Two-Jobs ****/
        // As we were testing Context1 and Context1-Two-Jobs, Context2-Two-Jobs was running in the background, therefore jobName2 should still be in 
        // WAITING status as no event should be sent to it because Context2-Two-Jobs is not part of the environmentGroup GRP1.
        Assert.assertEquals(InstanceStatus.WAITING, contextInstance3.getScheduledJobsMap().get("GLOBAL_EVENT-jobName2").getStatus());

        // As jobName2 has not sent an event onto the outbound big queue. make sure it is still 0
        Assert.assertEquals(contextMachine3.getOutboundQueue().size(), 0);
        
        // re-enable the outbound queue for Context2-Two-Jobs
        contextMachine3.setSchedulerJobInitiationEventRaisedListener(null);

        // Now check that all Jobs within the context instance is all waiting
        contextInstance3.getScheduledJobs().forEach(jobs -> {
            Assert.assertEquals(InstanceStatus.WAITING, jobs.getStatus());
        });

        // Make sure the context is still WAITING which means that nothing within the context has kicked off which is good.
        Assert.assertEquals(InstanceStatus.WAITING, contextInstance3.getStatus());
        /**** END TESTING Context2-Two-Jobs ****/
        
        String outboundQueueName = "outbound-" + contextInstance.getId() + "-queue";
        String inboundQueueName = "inbound-" + contextInstance.getId() + "-queue";
        String outboundQueueName2 = "outbound-" + contextInstance2.getId() + "-queue";
        String inboundQueueName2 = "inbound-" + contextInstance2.getId() + "-queue";
        String outboundQueueName3 = "outbound-" + contextInstance3.getId() + "-queue";
        String inboundQueueName3 = "inbound-" + contextInstance3.getId() + "-queue";

        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName2)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName2)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName3)));
        assertTrue(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName3)));

        ContextMachineCache.instance().remove(contextMachine);
        ContextMachineCache.instance().remove(contextMachine2);
        ContextMachineCache.instance().remove(contextMachine3);
        contextMachine.teardown();
        contextMachine2.teardown();
        contextMachine3.teardown();

        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName2)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName2)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + outboundQueueName3)));
        assertFalse(Files.exists(Path.of(this.queueDir + File.separator + inboundQueueName3)));
    }

    @Test
    public void test_broadcast_global_events_success() throws IOException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson = loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson2 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to change the context name and agent name as we reusing the same descriptor.
        String contextJson3 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("Context1-Two-Jobs","Context2-Two-Jobs").replaceAll("agentNameTwoJobs", "agentNameTwoJobsNull");

        //Context1 the main initiator - environmentGroup = GRP1
        ContextTemplate context1 = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance1 = this.contextService.getContextInstance(contextJson);

        // Context1-Two-Jobs - environmentGroup = GRP1
        ContextTemplate context2 = this.contextService.getContextTemplate(contextJson2);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(contextJson2);

        // Context2-Two-Jobs - environmentGroup = null
        ContextTemplate context3 = this.contextService.getContextTemplate(contextJson3);
        ContextInstance contextInstance3 = this.contextService.getContextInstance(contextJson3);

        /* START setup for Context1 */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine1  = new ContextMachine(context1, contextInstance1, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine1.init();
        ReflectionTestUtils.setField(contextMachine1, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine1);

        ContextMachine contextMachine2  = new ContextMachine(context2, contextInstance2, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine2.init();
        ReflectionTestUtils.setField(contextMachine2, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine2);

        ContextMachine contextMachine3  = new ContextMachine(context3, contextInstance3, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine3.init();
        ReflectionTestUtils.setField(contextMachine3, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine3);

        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("jobName2");
        schedulerJobInitiationEvent.setContextInstanceId(contextMachine1.getContext().getId());

        contextMachine1.broadcastGlobalEvents(schedulerJobInitiationEvent, true, false);

        verify(inboundQueue, times(3)).enqueue(any());
        verifyNoMoreInteractions(inboundQueue);
    }

    @Test
    public void test_broadcast_global_events_with_exception_in_one_plan_success() throws IOException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson = loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson2 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to change the context name and agent name as we reusing the same descriptor.
        String contextJson3 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("Context1-Two-Jobs","Context2-Two-Jobs").replaceAll("agentNameTwoJobs", "agentNameTwoJobsNull");

        //Context1 the main initiator - environmentGroup = GRP1
        ContextTemplate context1 = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance1 = this.contextService.getContextInstance(contextJson);

        // Context1-Two-Jobs - environmentGroup = GRP1
        ContextTemplate context2 = this.contextService.getContextTemplate(contextJson2);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(contextJson2);

        // Context2-Two-Jobs - environmentGroup = null
        ContextTemplate context3 = this.contextService.getContextTemplate(contextJson3);
        ContextInstance contextInstance3 = this.contextService.getContextInstance(contextJson3);

        /* START setup for Context1 */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine1  = new ContextMachine(context1, contextInstance1, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine1.init();
        BigQueueImpl inboundQueue1 = mock(BigQueueImpl.class);
        ReflectionTestUtils.setField(contextMachine1, "inboundQueue", inboundQueue1);

        ContextMachineCache.instance().put(contextMachine1);

        ContextMachine contextMachine2  = new ContextMachine(context2, contextInstance2, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine2.init();
        BigQueueImpl inboundQueue2 = mock(BigQueueImpl.class);
        ReflectionTestUtils.setField(contextMachine2, "inboundQueue", inboundQueue2);

        ContextMachineCache.instance().put(contextMachine2);

        ContextMachine contextMachine3  = new ContextMachine(context3, contextInstance3, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance(), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine3.init();
        BigQueueImpl inboundQueue3 = mock(BigQueueImpl.class);
        ReflectionTestUtils.setField(contextMachine3, "inboundQueue", inboundQueue3);

        ContextMachineCache.instance().put(contextMachine3);

        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("jobName2");
        schedulerJobInitiationEvent.setContextInstanceId(contextMachine1.getContext().getId());

        doThrow(new RuntimeException("error enqueuing message!")).when(inboundQueue2).enqueue(any());

        contextMachine1.broadcastGlobalEvents(schedulerJobInitiationEvent, true, false);

        verify(inboundQueue1, times(1)).enqueue(any());
        // we have an error thrown when enqueuing on inboundQueue2
        verify(inboundQueue2, times(1)).enqueue(any());
        // even though there was an error we still enqueue onto inboundQueue3
        verify(inboundQueue3, times(1)).enqueue(any());
        verifyNoMoreInteractions(inboundQueue1, inboundQueue2, inboundQueue3);
    }

    @Test
    public void test_broadcast_global_events_skipped_success() throws IOException {
        ObjectMapper objectMapperTest = ConcurrentObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson = loadDataFile("/data/logic/simple-context-and-chained-single-dependency.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson2 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("\"environmentGroup\" : null", "\"environmentGroup\" : \"GRP1\"");

        // modify the context descriptor to change the context name and agent name as we reusing the same descriptor.
        String contextJson3 = loadDataFile("/data/logic/simple-context-and-chained-single-dependency-2-jobs.json")
            .replaceAll("Context1-Two-Jobs","Context2-Two-Jobs").replaceAll("agentNameTwoJobs", "agentNameTwoJobsNull");

        //Context1 the main initiator - environmentGroup = GRP1
        ContextTemplate context1 = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance1 = this.contextService.getContextInstance(contextJson);

        // Context1-Two-Jobs - environmentGroup = GRP1
        ContextTemplate context2 = this.contextService.getContextTemplate(contextJson2);
        ContextInstance contextInstance2 = this.contextService.getContextInstance(contextJson2);

        // Context2-Two-Jobs - environmentGroup = null
        ContextTemplate context3 = this.contextService.getContextTemplate(contextJson3);
        ContextInstance contextInstance3 = this.contextService.getContextInstance(contextJson3);

        /* START setup for Context1 */
        // Setup the jobs for testing
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        InternalEventDrivenJobInstanceImpl internalJob1 = new InternalEventDrivenJobInstanceImpl();
        internalJob1.setAgentName("agentName1");
        internalJob1.setJobName("jobName1");
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", internalJob1);

        InternalEventDrivenJobInstanceImpl internalJob3 = new InternalEventDrivenJobInstanceImpl();
        internalJob3.setAgentName("agentName3");
        internalJob3.setJobName("jobName3");
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalJob3);

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        GlobalEventJobInstanceImpl globalJob2 = new GlobalEventJobInstanceImpl();
        globalJob2.setAgentName("agentName2");
        globalJob2.setJobName("jobName2");
        globalEventJobInstances.put("GLOBAL_EVENT-jobName2-Context1", globalJob2);

        ContextMachine contextMachine1  = new ContextMachine(context1, contextInstance1, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine1.init();
        ReflectionTestUtils.setField(contextMachine1, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine1);

        ContextMachine contextMachine2  = new ContextMachine(context2, contextInstance2, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine2.init();
        ReflectionTestUtils.setField(contextMachine2, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine2);

        ContextMachine contextMachine3  = new ContextMachine(context3, contextInstance3, new ScheduledContextInstanceServiceTestImpl(), globalEventJobInstances, new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine3.init();
        ReflectionTestUtils.setField(contextMachine3, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine3);

        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setJobName("jobName2");
        schedulerJobInitiationEvent.setAgentName(JobConstants.GLOBAL_EVENT);
        schedulerJobInitiationEvent.setContextInstanceId(contextMachine1.getContext().getId());
        schedulerJobInitiationEvent.setSkipped(true);

        contextMachine1.broadcastGlobalEvents(schedulerJobInitiationEvent, true, false);

        verify(inboundQueue, times(1)).enqueue(any());
        verifyNoMoreInteractions(inboundQueue);
    }

    @Test
    public void test_context_machine_concurrent_modifications() throws IOException, InvalidContextTemplateException, InterruptedException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        // Start a thread that is heavily exercising the ContextMachine
        ExecutorService contextExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("test-context-event-factory"));
        ExecutorService contextModifierExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("test-context-modifier-factory"));

        try {
            AtomicReference<Boolean> concurrentModificationExceptionEncountered = new AtomicReference<>(false);
            AtomicReference<Boolean> generalExceptionEncountered = new AtomicReference<>(false);
            contextExecutor.execute(() -> {
                try {
                    while (true) {
                        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
                            "agentName3", false);
                        eventInstance.setJobStarting(true);

                        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
                        Assert.assertEquals(0, events.size());

                        eventInstance = scheduledProcessEventInstance("jobName3",
                            "agentName3", true);

                        contextMachine.eventReceived(eventInstance);
                    }
                } catch (ConcurrentModificationException concurrentModificationException) {
                    concurrentModificationException.printStackTrace();
                    concurrentModificationExceptionEncountered.set(true);
                } catch (Exception e) {
                    generalExceptionEncountered.set(true);
                }
            });

            // Now start another thread that changes the internal structure of the context instance. In reality this would not
            // happen as a job plan instance is immutable, however we are doing here to try to expose a potential ConcurrentModificationException.
            contextModifierExecutor.execute(() -> {
                while(true) {
                    contextMachine.getContext().getContexts().add(new ContextInstanceImpl());
                    contextMachine.getContext().getContexts().get(0).getContexts().add(new ContextInstanceImpl());
                    contextMachine.getContext().getContexts().get(0).getContexts().get(0).getContexts().add(new ContextInstanceImpl());
                    contextMachine.getContext().getContexts().get(0).getContexts().get(0).getContexts()
                        .remove(contextMachine.getContext().getContexts().get(0).getContexts().get(0).getContexts().size()-1);
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        // this get thrown when we tidy up our threads so simply ignore.
                    }
                }
            });

            // We'll give ourselves 30 seconds
            Thread.sleep(30000);

            Assert.assertFalse("No concurrent modification exceptions should be encountered!", concurrentModificationExceptionEncountered.get());
            Assert.assertFalse("No general exceptions should be encountered!", generalExceptionEncountered.get());
        }
        finally {
            // clean up our threads
            contextExecutor.shutdownNow();
            contextModifierExecutor.shutdownNow();
        }
    }

    @Test
    public void test_release_queued_jobs() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);
        internalEventDrivenJobs.values().forEach(job -> {
            job.setContextName(contextInstance.getName());
            job.setContextInstanceId("contextInstanceId");
        });

        ContextHelper.setJobStatusAll(contextInstance, internalEventDrivenJobs, InstanceStatus.LOCK_QUEUED);

        when(mockContextInstance.getAllSchedulerJobInstances()).thenReturn(new ArrayList<>(internalEventDrivenJobs.values()));
        when(mockContextInstance.getId()).thenReturn("contextInstanceId");
        when(mockContextInstance.getEnvironmentGroup()).thenReturn("environment");

        this.contextTemplateValidator.validate(context);

        ContextMachine contextMachine = new ContextMachine(context, mockContextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        // Set up the job locks used in the test
        ArrayList<InternalEventDrivenJob> jobs = new ArrayList<>(internalEventDrivenJobs.values());
        JobLockCacheImpl.instance().addLocks(List.of(this.makeJobLock("JOB_LOCK", jobs)), context.getEnvironmentGroup());

        // We set all but one job in a queued state
        for (int i=1; i<jobs.size(); i++) {
            InternalEventDrivenJobInstance internalEventDrivenJob = (InternalEventDrivenJobInstance) jobs.get(i);
            internalEventDrivenJob.setStatus(InstanceStatus.LOCK_QUEUED);

            SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
            schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);
            schedulerJobInitiationEvent.setContextInstanceId(internalEventDrivenJob.getContextInstanceId());
            JobLockCacheImpl.instance().addQueuedSchedulerJobInitiationEvent
                    (internalEventDrivenJob.getIdentifier(), internalEventDrivenJob.getContextName()
                        , schedulerJobInitiationEvent, context.getEnvironmentGroup());
        }

        // And one running job that will take the lock
        InternalEventDrivenJobInstance job = internalEventDrivenJobs.values().stream().findFirst().get();
        job.setStatus(InstanceStatus.RUNNING);

        JobLockCacheImpl.instance().lock(job.getIdentifier(), job.getContextName(), context.getEnvironmentGroup());

        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(JobLockCacheImpl.instance(), "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

        // Now assert that
        Assert.assertEquals(16, jobLockCacheData.getJobLocksByIdentifier().size());
        Assert.assertEquals(1, jobLockCacheData.getJobLocksByLockName().size());
        Assert.assertEquals(15, jobLockCacheData.getJobLocksByLockName().values().stream().findFirst()
            .get().getSchedulerJobInitiationEventWaitQueue().size());
        Assert.assertTrue(JobLockCacheImpl.instance().hasLock(job.getIdentifier(), job.getContextName(), context.getEnvironmentGroup()));

        contextMachine.releaseQueuedJobs();

        Assert.assertEquals(16, jobLockCacheData.getJobLocksByIdentifier().size());
        Assert.assertEquals(1, jobLockCacheData.getJobLocksByLockName().size());

        // After releasing all queued jobs, there are no longer any queued jobs or jobs holding jocks
        Assert.assertEquals(0, jobLockCacheData.getJobLocksByLockName().values().stream().findFirst()
            .get().getSchedulerJobInitiationEventWaitQueue().size());
        Assert.assertFalse(JobLockCacheImpl.instance().hasLock(job.getIdentifier(), job.getContextName(), context.getEnvironmentGroup()));

        verify(mockContextInstance, times(16)).getId();
        verify(mockContextInstance, times(2)).getScheduledJobs();
        verify(mockContextInstance, times(2)).getContexts();
        verify(mockContextInstance, times(33)).getEnvironmentGroup();
        verify(mockContextInstance, times(1)).getAllSchedulerJobInstances();

        verifyNoMoreInteractions(mockContextInstance);
    }

    @Test
    public void test_black_listed_message_retries_exceeded_and_message_placed_onto_DLQ() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.setId(UUID.randomUUID().toString());

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        doNothing()
            .doThrow(new RuntimeException("error!"))
            .when(this.scheduledContextInstanceService)
            .save(any());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().put(contextMachine);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        ConcurrentHashMap<String, Integer> bigQueueMessageBlacklist = (ConcurrentHashMap<String, Integer>)ReflectionTestUtils
            .getField(contextMachine, "bigQueueMessageBlacklist");

        IBigQueue deadLetterQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "deadLetterQueue");

        // Assert that the offending message has been placed on the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(1, deadLetterQueue.size()));

        IBigQueue inboundQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "inboundQueue");

        // Assert that the offending message has been dequeued from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));

        // Confirm that the message that was blacklisted is no longer in the blacklist.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> bigQueueMessageBlacklist.size() == 0);
    }

    @Test
    public void test_black_listed_message_retries_exceeded_and_message_placed_onto_DLQ_and_resubmitted() throws IOException, JSONException, InvalidContextTemplateException, BigQueueNotFoundException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.setId(UUID.randomUUID().toString());

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        doNothing()
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doNothing()
            .when(this.scheduledContextInstanceService)
            .save(any());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().put(contextMachine);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        ConcurrentHashMap<String, Integer> bigQueueMessageBlacklist = (ConcurrentHashMap<String, Integer>)ReflectionTestUtils
            .getField(contextMachine, "bigQueueMessageBlacklist");

        IBigQueue deadLetterQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "deadLetterQueue");

        // Assert that the offending message has been placed on the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(1, deadLetterQueue.size()));

        IBigQueue inboundQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "inboundQueue");

        // Assert that the offending message has been dequeued from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));

        // Confirm that the message that was blacklisted is no longer in the blacklist.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> bigQueueMessageBlacklist.size() == 0);

        bigQueueMessage = objectMapper.readValue(deadLetterQueue.peek(), BigQueueMessageImpl.class);

        Assert.assertNotNull(bigQueueMessage);

        // Resubmit the message from the DLQ!
        Assert.assertTrue(contextMachine.resubmitMessageFromDeadLetterQueue(bigQueueMessage.getMessageId()));

        // Assert that the offending message has been removed from the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, deadLetterQueue.size()));

        // Assert that the offending message has been processed from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));
    }

    @Test
    public void test_black_listed_message_retries_exceeded_and_message_placed_onto_DLQ_and_resubmitted_bad_message_id() throws IOException, JSONException, InvalidContextTemplateException, BigQueueNotFoundException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.setId(UUID.randomUUID().toString());

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        doNothing()
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doNothing()
            .when(this.scheduledContextInstanceService)
            .save(any());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().put(contextMachine);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        ConcurrentHashMap<String, Integer> bigQueueMessageBlacklist = (ConcurrentHashMap<String, Integer>)ReflectionTestUtils
            .getField(contextMachine, "bigQueueMessageBlacklist");

        IBigQueue deadLetterQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "deadLetterQueue");

        // Assert that the offending message has been placed on the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(1, deadLetterQueue.size()));

        IBigQueue inboundQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "inboundQueue");

        // Assert that the offending message has been dequeued from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));

        // Confirm that the message that was blacklisted is no longer in the blacklist.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> bigQueueMessageBlacklist.size() == 0);

        bigQueueMessage = objectMapper.readValue(deadLetterQueue.peek(), BigQueueMessageImpl.class);

        Assert.assertNotNull(bigQueueMessage);

        // Resubmit the message from the DLQ!
        Assert.assertFalse(contextMachine.resubmitMessageFromDeadLetterQueue("bad message id"));

        // Assert that the offending message has is still on the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(1, deadLetterQueue.size()));

        // Assert that the offending message has been processed from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));
    }

    @Test
    public void test_black_listed_message_retries_and_succeeds_nothing_on_DLQ() throws IOException, JSONException, InvalidContextTemplateException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));
        contextInstance.setId(UUID.randomUUID().toString());

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        this.contextTemplateValidator.validate(context);

        doNothing()
            .doThrow(new RuntimeException("error!"))
            .doThrow(new RuntimeException("error!"))
            .doNothing()
            .when(this.scheduledContextInstanceService)
            .save(any());

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();

        // Add the context machine to the cache
        ContextMachineCache.instance().resetAllCache();
        ContextMachineCache.instance().put(contextMachine);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", false);
        eventInstance.setJobStarting(true);

        BigQueueMessage bigQueueMessage = new BigQueueMessageBuilder().withMessage(objectMapper.writeValueAsString(eventInstance)).build();
        contextMachine.eventReceived(objectMapper.writeValueAsString(bigQueueMessage));

        ConcurrentHashMap<String, Integer> bigQueueMessageBlacklist = (ConcurrentHashMap<String, Integer>)ReflectionTestUtils
            .getField(contextMachine, "bigQueueMessageBlacklist");

        IBigQueue deadLetterQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "deadLetterQueue");

        // Assert that the offending message has NOT been placed on the DLQ.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, deadLetterQueue.size()));

        IBigQueue inboundQueue = (IBigQueue) ReflectionTestUtils.getField(contextMachine, "inboundQueue");

        // Assert that the offending message has been dequeued from the inbound queue.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> Assert.assertEquals(0, inboundQueue.size()));

        // Confirm that the message that was blacklisted is not longer in the blacklist.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> bigQueueMessageBlacklist.size() == 0);
    }

    protected JobLock makeJobLock(String jobLockName, ArrayList<InternalEventDrivenJob> internalEventDrivenJobs) {
        JobLockBuilder jobLockBuilder = new JobLockBuilder();
        jobLockBuilder.withLockName(jobLockName);
        jobLockBuilder.withLockCount(1);
        for (InternalEventDrivenJob internalEventDrivenJob: internalEventDrivenJobs) {
            SchedulerJobLockParticipant job = makeSchedulerJobLockParticipant(internalEventDrivenJob);
            job.setContextName(UUID.randomUUID().toString());
            jobLockBuilder.withJob(job.getContextName(), job);
        }
        return jobLockBuilder.build().get(0);
    }

    /**
     * Create a SchedulerJobLockParticipant object with the provided job lock name and internal event driven job.
     *
     * @param internalEventDrivenJob the internal event driven job to associate with the participant
     * @return a SchedulerJobLockParticipant object initialized with the provided values
     */
    protected SchedulerJobLockParticipant makeSchedulerJobLockParticipant(InternalEventDrivenJob internalEventDrivenJob) {
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setAgentName("AgentName");
        job.setJobName(internalEventDrivenJob.getJobName());
        job.setIdentifier(internalEventDrivenJob.getIdentifier());
        job.setJobDescription("Job Description");
        job.setLockCount(1);
        return job;
    }

    protected List<SchedulerJobInitiationEvent> sendScheduledEventToContextMachineWithChildContextId(ContextMachine contextMachine, String contextId, List<String> childContextIds
    , String agentName, String jobName, boolean eventSuccessful) {
    ContextualisedScheduledProcessEventImpl eventInstance
        = scheduledProcessEventInstance(contextId, childContextIds, jobName, agentName, eventSuccessful);

    return contextMachine.eventReceived(eventInstance);
    }

    protected List<SchedulerJobInitiationEvent> sendScheduledEventToContextMachine(ContextMachine contextMachine, String contextId, String childContextId
        , String agentName, String jobName, boolean eventSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance(contextId, childContextId, jobName, agentName, eventSuccessful);

        return contextMachine.eventReceived(eventInstance);
    }

    protected void assertContextStatus(ContextMachine contextMachine, String context, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getContextStatus(context);
        Assert.assertEquals(expected, status);
    }

    protected void assertJobStatus(ContextMachine contextMachine, String context, String jobName, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getJobStatus(context, jobName);
        Assert.assertEquals(expected, status);
    }

    /**
     * Test concurrent read operations for thread safety
     */
    @Test
    public void test_concurrent_status_reads() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicBoolean hasException = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Read operations should be thread-safe
                        contextMachine.getContextStatus("Context1");
                        contextMachine.getJobStatus("Context1", "agentName1-jobName1");
                        contextMachine.getContext();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hasException.set(true);
                }
            });
        }

        executor.shutdown();
        assertTrue("Concurrent operations should complete", executor.awaitTermination(30, TimeUnit.SECONDS));
        assertFalse("No exceptions should occur during concurrent reads", hasException.get());
    }

    /**
     * Test concurrent getContext calls
     */
    @Test
    public void test_concurrent_get_context() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicBoolean hasException = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        ContextInstance ctx = contextMachine.getContext();
                        assertFalse(ctx == null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hasException.set(true);
                }
            });
        }

        executor.shutdown();
        assertTrue("Concurrent operations should complete", executor.awaitTermination(30, TimeUnit.SECONDS));
        assertFalse("No exceptions should occur during concurrent getContext", hasException.get());
    }


    /**
     * Test error handling when hold job called with invalid job identifier
     */
    @Test(expected = ContextMachineException.class)
    public void test_hold_job_invalid_identifier_throws_exception() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        contextMachine.holdJob("non-existent-job", "Context1");
    }

    /**
     * Test error handling when release job called with invalid job identifier
     */
    @Test(expected = ContextMachineException.class)
    public void test_release_job_invalid_identifier_throws_exception() throws Exception {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsMap(context);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobs, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);

        contextMachine.releaseJob("non-existent-job", "Context1");
    }


    // The below is used to disable the outbound threads from consuming the data so that interrogating what is on the BigQueue is possible
    protected class SchedulerJobInitiationEventRaisedListenerToDisableOutboundBigQueue implements SchedulerJobInitiationEventRaisedListener {
        @Override
        public void onSchedulerJobInitiationEventRaised(SchedulerJobInitiationEvent event) {
            throw new RuntimeException("Stops the Outbound BigQueue from Consuming the data within the ContextMachine");
        }
    }
}
