package org.ikasan.job.orchestration.core.machine;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.core.ScheduledContextInstanceServiceTestImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ContextMachineWitStartAndTerminalJobsTest extends AbstractTest {

    protected ContextService contextService = new ContextService();
    protected ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
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
    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
    }

    @Test
    public void test_context_with_start_and_terminal_jobs_success() throws IOException {
        ObjectMapper objectMapperTest = ObjectMapperFactory.newInstance();
        objectMapperTest.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // modify the context descriptor to add GRP1 for the environment group
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap = this.loadInternalEventDrivenJobInstanceMap
            (contextInstance, "./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal",
            "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal");

        Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap = loadContextTerminalJobInstanceMap(context, contextInstance);

        Map<String, ContextStartJobInstance> contextStartJobInstanceMap = loadContextStartJobInstanceMap(context, contextInstance);

        ContextMachine contextMachine  = new ContextMachine(context, contextInstance, new ScheduledContextInstanceServiceTestImpl(), new HashMap<>(), new HashMap<>()
            , internalEventDrivenJobInstanceMap, contextStartJobInstanceMap, contextTerminalJobInstanceMap, new HashMap<>(), this.queueDir, new HashMap<>(), moduleMetadataService, JobLockCacheImpl.instance()
            , contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService
            , this.jobLockCacheInitialisationService, contextInstancePublicationService, this.jobUtilsService);
        contextMachine.init();
        ReflectionTestUtils.setField(contextMachine, "inboundQueue", inboundQueue);

        ContextMachineCache.instance().put(contextMachine);

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.WAITING);

        ContextualisedScheduledProcessEventImpl eventInstance = scheduledProcessEventInstance("TEST_IK_JOB_2",
            "scheduler-agent", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("TEST_IK_JOB_2",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.WAITING);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.WAITING);

        Assert.assertEquals("TEST_IK_JOB_1", events.get(0).getJobName());
        String processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", false);
        eventInstance.setJobStarting(true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 1 Terminal", events.get(0).getJobName());
        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);
        eventInstance.setInternalEventDrivenJob(null);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_AM_1 Step 1 Start", events.get(0).getJobName());
        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);
        eventInstance.setInternalEventDrivenJob(null);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(3, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.WAITING);

        Assert.assertEquals("TEST_IK_JOB_3", events.get(0).getJobName());
        Assert.assertEquals("TEST_IK_JOB_4", events.get(1).getJobName());
        Assert.assertEquals("TEST_IK_JOB_5", events.get(2).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(1).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(2).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_AM_1 Step 1 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);
        eventInstance.setInternalEventDrivenJob(null);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_AM_2 Step 1 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);
        eventInstance.setInternalEventDrivenJob(null);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(3, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_JOB_6", events.get(0).getJobName());
        Assert.assertEquals("TEST_IK_JOB_7", events.get(1).getJobName());
        Assert.assertEquals("TEST_IK_JOB_8", events.get(2).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(1).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(2).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_AM_2 Step 1 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);
        eventInstance.setInternalEventDrivenJob(null);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.WAITING);

        Assert.assertEquals("TEST_IK_GLOB Step 3 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_JOB_9", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_LOCK_1 Step 1 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_10", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_LOCK_1 Step 1 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_LOCK_1 Step 2 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(4, events.size());

        Assert.assertEquals("TEST_IK_JOB_14", events.get(0).getJobName());
        Assert.assertEquals("TEST_IK_JOB_13", events.get(1).getJobName());
        Assert.assertEquals("TEST_IK_JOB_12", events.get(2).getJobName());
        Assert.assertEquals("TEST_IK_JOB_11", events.get(3).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(1).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(2).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(3).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);

        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_LOCK_1 Step 2 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 3 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);


        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.WAITING);

        Assert.assertEquals("TEST_IK_EVENT1 Step 1 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_15", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_EVENT2 Step 1 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_17", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_EVENT2 Step 1 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_EVENT2 Step 2 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_18", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_EVENT2 Step 2 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_16", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_EVENT1 Step 1 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 5", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_GLOB Step 5 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_JOB_23", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 5 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());


        Assert.assertEquals("TEST_IK_GLOB Step 6 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 5", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 6", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_JOB_26_HANG", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 6 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 7 Start", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 5", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 6", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 7", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_JOB_20_FAIL", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 7 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 8 Start", events.get(0).getJobName());

        eventInstance = scheduledProcessEventInstance("TEST_IK_JOB_19_ScheduledJob_15:40:00",
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        eventInstance = scheduledProcessEventInstance("TEST_IK_GLOB Step 8 Start",
            JobConstants.CONTEXT_START_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(3, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.RUNNING);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 5", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 6", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 7", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 8", InstanceStatus.RUNNING);

        Assert.assertEquals("TEST_IK_JOB_21_MIN", events.get(0).getJobName());
        Assert.assertEquals("TEST_IK_JOB_22_MAX", events.get(1).getJobName());
        Assert.assertEquals("TEST_IK_JOB_19", events.get(2).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(1).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        Assert.assertEquals(0, contextMachine.eventReceived(eventInstance).size());

        processEventJobName = events.get(2).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            "scheduler-agent", true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(1, events.size());

        Assert.assertEquals("TEST_IK_GLOB Step 8 Terminal", events.get(0).getJobName());

        processEventJobName = events.get(0).getJobName();

        eventInstance = scheduledProcessEventInstance(processEventJobName,
            JobConstants.CONTEXT_TERMINAL_JOB, true);

        events = contextMachine.eventReceived(eventInstance);
        Assert.assertEquals(0, events.size());

        this.assertContextStatus(contextMachine, "TEST_IK_GLOB", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 2", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_1 Step 1",   InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_AM_2 Step 1", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 3", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 4", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 5", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 6", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 7", InstanceStatus.COMPLETE);
        this.assertContextStatus(contextMachine, "TEST_IK_GLOB Step 8", InstanceStatus.COMPLETE);
    }

//    public Map<String, ContextTerminalJobInstance> loadContextTerminalJobInstanceMap(ContextInstance contextInstance, String directory, String jobsBase) throws IOException {
//        List<ContextTerminalJobInstance> files = Files.list(Path.of(directory)).map(path -> {
//            ContextTerminalJobInstance contextTerminalJobInstance = null;
//            try {
//                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
//                contextTerminalJobInstance = objectMapper.readValue(jobJson, ContextTerminalJobInstanceImpl.class);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            return contextTerminalJobInstance;
//        }).collect(Collectors.toList());
//
//        Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap =  files.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));
//
//        List<ContextTerminalJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();
//
//        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
//            ContextTerminalJobInstance instance = contextTerminalJobInstanceMap.get(schedulerJobInstance.getIdentifier());
//
//            if(instance != null) {
//                ContextTerminalJobInstance contextualisedInstance = (ContextTerminalJobInstance) SerializationUtils.clone(instance);
//                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
//                contextualisedInstance.setContextInstanceId(contextInstance.getId());
//
//                contextualisedSchedulerJobInstances.add(contextualisedInstance);
//            }
//        });
//
//        return contextualisedSchedulerJobInstances.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
//    }
//
//    public Map<String, ContextStartJobInstance> loadContextStartJobInstanceMap(ContextInstance contextInstance, String directory, String jobsBase) throws IOException {
//        List<ContextStartJobInstance> files = Files.list(Path.of(directory)).map(path -> {
//            ContextStartJobInstance contextStartJobInstance = null;
//            try {
//                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
//                contextStartJobInstance = objectMapper.readValue(jobJson, ContextStartJobInstanceImpl.class);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            return contextStartJobInstance;
//        }).collect(Collectors.toList());
//
//        Map<String, ContextStartJobInstance> contextStartJobInstanceMap =  files.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));
//
//        List<ContextStartJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();
//
//        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
//            ContextStartJobInstance instance = contextStartJobInstanceMap.get(schedulerJobInstance.getIdentifier());
//
//            if(instance != null) {
//                ContextStartJobInstance contextualisedInstance = (ContextStartJobInstance) SerializationUtils.clone(instance);
//                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
//                contextualisedInstance.setContextInstanceId(contextInstance.getId());
//
//                contextualisedSchedulerJobInstances.add(contextualisedInstance);
//            }
//        });
//
//        return contextualisedSchedulerJobInstances.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
//    }
//
//    public Map<String, InternalEventDrivenJobInstance> loadInternalEventDrivenJobInstanceMap(ContextInstance contextInstance, String directory, String jobsBase) throws IOException {
//        List<InternalEventDrivenJobInstance> files = Files.list(Path.of(directory)).map(path -> {
//            InternalEventDrivenJobInstance internalEventDrivenJob = null;
//            try {
//                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
//                internalEventDrivenJob = objectMapper.readValue(jobJson, InternalEventDrivenJobInstanceImpl.class);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            return internalEventDrivenJob;
//        }).collect(Collectors.toList());
//
//        Map<String, InternalEventDrivenJobInstance> schedulerJobInstanceMap =  files.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));
//
//        List<InternalEventDrivenJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();
//
//        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
//            InternalEventDrivenJobInstance instance = schedulerJobInstanceMap.get(schedulerJobInstance.getIdentifier());
//
//            if(instance != null) {
//                InternalEventDrivenJobInstance contextualisedInstance = (InternalEventDrivenJobInstance) SerializationUtils.clone(instance);
//                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
//                contextualisedInstance.setContextInstanceId(contextInstance.getId());
//
//                contextualisedSchedulerJobInstances.add(contextualisedInstance);
//            }
//        });
//
//        return contextualisedSchedulerJobInstances.stream()
//            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
//    }
}
