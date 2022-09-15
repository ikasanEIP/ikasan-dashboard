package org.ikasan.orchestration.service.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.utils.CustomBackFillerMatcher;
import org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults;
import org.ikasan.orchestration.service.utils.TestUtils;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults.AGENT_NAME;
import static org.ikasan.orchestration.service.utils.TestUtils.AGENT_URL;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRegistrationServiceImplTest {

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private JobInitiationService jobInitiationService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;

    @Mock
    SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;


    private ContextInstanceRegistrationServiceImpl contextInstanceRegistrationService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private String contextName;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);

        contextInstanceRegistrationService = new ContextInstanceRegistrationServiceImpl(
            "bigQueue/dir",
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        TestUtils.resetContextMachineCache();
        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @After
    public void tearDown() {
        TestUtils.resetContextMachineCache();
    }

    @Test(expected = RuntimeException.class)
    public void register_null_ScheduledContextRecord() {
        // set up
        when(scheduledContextService.findById(contextName)).thenReturn(null);

        // execute
        contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @Test
    public void register_with_agents_not_found_module_metadata() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(null);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        // execute
        contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(schedulerJobInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(moduleMetadataService).findById(AGENT_NAME + "1");
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));
        verify(jobLockCacheService).get();
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        assertNotNull(contextMachine);

        SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
            = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
        assertNotNull(schedulerJobInitiationEventRaisedListener);

        List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
            = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
        assertNotNull(contextInstanceStateChangeEventListeners);
        assertEquals(1, contextInstanceStateChangeEventListeners.size());

        JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
        assertNotNull(jobLogicMachine);
        List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
            = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
        assertNotNull(schedulerJobInstanceStateChangeEventListeners);
        assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
    }

    @Test
    public void register_with_agents() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(TestUtils.createModuleMetaData("1"));
        when(moduleMetadataService.findById(AGENT_NAME + "2")).thenReturn(TestUtils.createModuleMetaData("2"));
        when(moduleMetadataService.findById(AGENT_NAME + "3")).thenReturn(TestUtils.createModuleMetaData("3"));

        List<ContextParameterInstance> params = TestUtils.createParams();
        when(contextParametersInstanceService.getAllContextParameters(contextName)).thenReturn(params);

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        // execute
        contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(schedulerJobInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(moduleMetadataService).findById(AGENT_NAME + "1");
        verify(moduleMetadataService).findById(AGENT_NAME + "2");
        verify(moduleMetadataService).findById(AGENT_NAME + "3");
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));
        verify(jobLockCacheService).get();
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        assertNotNull(contextMachine);

        SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
            = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
        assertNotNull(schedulerJobInitiationEventRaisedListener);

        List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
            = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
        assertNotNull(contextInstanceStateChangeEventListeners);
        assertEquals(1, contextInstanceStateChangeEventListeners.size());

        JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
        assertNotNull(jobLogicMachine);
        List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
            = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
        assertNotNull(schedulerJobInstanceStateChangeEventListeners);
        assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
    }

    @Test
    public void register_with_agents_with_skipped_jobs() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1, true, false, "scheduler-agent-1799613995");
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "scheduler-agent-1799613995")).thenReturn(TestUtils.createModuleMetaData("1"));

        List<ContextParameterInstance> params = TestUtils.createParams();
        when(contextParametersInstanceService.getAllContextParameters(contextName)).thenReturn(params);

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        // execute
        contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(schedulerJobInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(moduleMetadataService).findById(AGENT_NAME + "scheduler-agent-1799613995");
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1")
            , argThat(new CustomBackFillerMatcher(contextInstance, contextName)));
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));
        verify(jobLockCacheService).get();
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        assertNotNull(contextMachine);

        SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
            = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
        assertNotNull(schedulerJobInitiationEventRaisedListener);

        List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
            = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
        assertNotNull(contextInstanceStateChangeEventListeners);
        assertEquals(1, contextInstanceStateChangeEventListeners.size());

        JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
        assertNotNull(jobLogicMachine);
        List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
            = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
        assertNotNull(schedulerJobInstanceStateChangeEventListeners);
        assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());

        ContextInstance child = ContextHelper.getChildContextInstance("CONTEXT-1616645609", contextMachine.getContext());
        SchedulerJobInstance schedulerJobInstance = child.getScheduledJobsMap().get("scheduler-agent-1799613995");
        assertNotNull(schedulerJobInstance);

        assertEquals(true, schedulerJobInstance.isSkip());
        assertEquals(InstanceStatus.SKIPPED, schedulerJobInstance.getStatus());
    }

    @Test
    public void register_no_agents() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(0);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        // execute
        contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(schedulerJobInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));
        verify(jobLockCacheService, times(1)).get();
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).getAllContextParameters(contextName);
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp()
            <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        assertNotNull(contextMachine);

        SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
            = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
        assertNotNull(schedulerJobInitiationEventRaisedListener);

        List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
            = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
        assertNotNull(contextInstanceStateChangeEventListeners);
        assertEquals(1, contextInstanceStateChangeEventListeners.size());

        JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
        assertNotNull(jobLogicMachine);
        List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
            = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
        assertNotNull(schedulerJobInstanceStateChangeEventListeners);
        assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
    }

    @Test(expected = RuntimeException.class)
    public void deregsiter_null_contextmachine_should() {
        // execute
        contextInstanceRegistrationService.deRegister(contextName);

        // verify
        verifyNoMoreInteractions(scheduledContextInstanceService);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }

    @Test
    public void deregsiter_should_save_instance_as_ended() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, JobLockCacheImpl.instance(), null,
            null, this.schedulerJobInstanceService);

        ContextMachineCache.instance().put(contextMachine);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(TestUtils.createModuleMetaData("1"));

        // execute
        contextInstanceRegistrationService.deRegister(contextName);

        // verify
        verify(schedulerJobInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(moduleMetadataService).findById(AGENT_NAME + "1");
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertNull(ContextMachineCache.instance().getByContextName(contextName));
    }
}