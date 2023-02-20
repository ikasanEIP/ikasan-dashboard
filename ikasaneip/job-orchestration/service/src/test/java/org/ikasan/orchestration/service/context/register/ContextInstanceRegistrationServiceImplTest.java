package org.ikasan.orchestration.service.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.GlobalEventJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.utils.*;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.now;
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
    ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;

    @Mock
    SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;

    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    private ContextInstanceSchedulerService contextInstanceSchedulerService;
    @Mock
    private TimeService timeService;
    @Mock
    private ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster;
    private ContextInstanceRegistrationServiceImpl contextInstanceRegistrationService;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private String contextName;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);

        // Stub this Implementation due to two difference SchedulerJobInstance can be returned
        schedulerJobInstanceService = new StubSchedulerJobInstanceServiceTestImpl();

        contextInstanceRegistrationService = new ContextInstanceRegistrationServiceImpl (
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
            schedulerJobStateChangeEventBroadcaster,
            jobLockCacheInitialisationService,
            contextInstanceSchedulerService,
            timeService,
            contextInstanceSavedEventBroadcaster);
        ContextMachineCache.instance().resetAllCache();
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @After
    public void tearDown() {
        ContextMachineCache.instance().resetAllCache();
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
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
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
    public void register_with_agents_outside_cron_blackout_window_so_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-cron-blackout-window-outside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);

        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);


        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
    public void register_with_agents_outside_datetime_window_so_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);

        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);


        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
    public void register_with_agents_outside_datetime_window_with_timezone_so_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        String timezone = "Asia/Singapore";
        ZonedDateTime zdtNowInSingapore = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setTimezone(timezone);

        // Pretend we are in Singapore
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInSingapore.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of( zdtNowInSingapore.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInSingapore.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000
            && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
    public void register_with_agents_inside_blackout_window_so_should_not_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-cron-blackout-window-inside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_agents_inside_datetime_blackout_window_so_should_not_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-inside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_agents_inside_datetime_with_timezone_blackout_window_so_should_not_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-inside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        String timezone = "Asia/Singapore";
        ZonedDateTime zdtNowInSingapore = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setTimezone(timezone);

        // Pretend we are in Singapore
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInSingapore.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of( zdtNowInSingapore.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                    zdtNowInSingapore.plus(Duration.ofMinutes(200)).toInstant().toEpochMilli()));

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_disabled_agent() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        record.setDisabled(true);
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");


        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setDisabled(true);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
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
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 1, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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

        assertTrue(schedulerJobInstance.isSkip());
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
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
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
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
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
    public void deregsiter_contextmachine_that_not_in_cache() {
        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName);

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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void deregister_should_save_instance_as_ended() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, null, null, JobLockCacheImpl.instance(), null,
            null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        ContextMachineCache.instance().put(contextMachine);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));

        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName);

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void deregister_ignore_as_instance_marked_as_manual_end() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setRunContextUntilManuallyEnded(true);
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, null, null, JobLockCacheImpl.instance(), null,
            null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        ContextMachineCache.instance().put(contextMachine);

        // execute
        contextInstanceRegistrationService.deRegisterById(contextInstance.getId());

        // verify
        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertFalse(ContextMachineCache.instance().cacheIsEmpty());
        assertEquals(InstanceStatus.WAITING, ContextMachineCache.instance()
            .getByContextInstanceId(contextInstance.getId()).getContext().getStatus());
    }

    @Test
    public void deregister_manually() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setRunContextUntilManuallyEnded(true);
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null, null, null, null, JobLockCacheImpl.instance(), null,
            null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        ContextMachineCache.instance().put(contextMachine);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));

        // execute
        contextInstanceRegistrationService.deregisterManually(contextInstance.getId());

        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        // verify
        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void register_with_agents_with_a_global_event() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);


        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
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
            jobLockCacheService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNotNull(contextMachine);

        HashMap<String, GlobalEventJobInstanceImpl> globalMap =
            (HashMap<String, GlobalEventJobInstanceImpl>) ReflectionTestUtils.getField(contextMachine, "globalEventJobInstanceMap");
        Assert.assertEquals(globalMap.size(), 1);

        HashMap<String, InternalEventDrivenJobInstanceImpl> internalMap =
            (HashMap<String, InternalEventDrivenJobInstanceImpl>) ReflectionTestUtils.getField(contextMachine, "internalEventDrivenJobInstances");
        Assert.assertEquals(internalMap.size(), 3);

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
}