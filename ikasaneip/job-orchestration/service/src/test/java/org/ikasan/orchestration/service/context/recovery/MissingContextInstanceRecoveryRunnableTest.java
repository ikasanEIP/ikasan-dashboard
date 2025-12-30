package org.ikasan.orchestration.service.context.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.JobLockCacheInitialisationServiceImpl;
import org.ikasan.orchestration.service.utils.GlobalEventJobTestSearchResults;
import org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults;
import org.ikasan.orchestration.service.utils.StubSchedulerJobInstanceServiceTestImpl;
import org.ikasan.orchestration.service.utils.TestUtils;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
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
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.ikasan.orchestration.service.utils.TestUtils.AGENT_URL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MissingContextInstanceRecoveryRunnableTest {
    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private JobInitiationService jobInitiationService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextParametersUpdateService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;

    @Mock
    SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;

    @Mock
    JobLockCacheInitialisationServiceImpl jobLockCacheInitialisationService;

    @Mock
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    @Mock
    private TimeService timeService;

    @Mock
    private JobUtilsService jobUtilsService;
    @Mock
    private JobProvisionService jobProvisionService;
    @Mock
    private SchedulerJobService schedulerJobService;
    @Mock
    private SearchResults searchResults;
    @Mock
    private ScheduledContextInstanceRecord scheduledContextInstanceRecord;
    @Mock
    private ContextInstance contextInstance;

    private MissingContextInstanceRecoveryRunnable backFiller;

    private String contextName;

    private ScheduledContextRecord record;
    private ContextTemplateImpl context;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Before
    public void setUp() {
        ContextMachineCache.instance().resetAllCache();

        // Stub this Implementation due to two difference SchedulerJobInstance can be returned
        schedulerJobInstanceService = new StubSchedulerJobInstanceServiceTestImpl();

        contextName = RandomStringUtils.randomAlphabetic(22);
        record = new ScheduledContextRecordImpl();
        context = new ContextTemplateImpl();
        record.setContextName(contextName);
        context.setTimeWindowStart("0 0 0 2 3 ? 1999");
        context.setName(contextName);
        record.setContext(context);

        backFiller = new MissingContextInstanceRecoveryRunnable("bigQueue/Dir",
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            record,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            jobLockCacheInitialisationService,
            contextInstanceSchedulerService,
            timeService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @After
    public void tearDown() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void should_create_instance_and_populate_params_and_save_instance_with_agents() throws Exception {
        // set up
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

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void should_create_instance_and_populate_params_and_save_instance_with_agents_sydney_timezone() throws Exception {
        this.context.setTimezone("Australia/Sydney");
        // 5 hours TTL
        this.context.setContextTtlMilliseconds(1000 * 60 * 60 * 5);
        this.record.setContext(context);
        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<String> endCron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCronTimezone = ArgumentCaptor.forClass(String.class);
        verify(contextInstanceSchedulerService).registerEndJobAndTrigger(anyString(), endCron.capture(), endCronTimezone.capture(), anyString());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            contextInstanceSchedulerService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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

        Assert.assertEquals(920311200000L, contextMachine.getContext().getProjectedEndTime());

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(contextMachine.getContext().getProjectedEndTime())
            , ZoneId.of("Australia/Sydney"));
        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Australia/Sydney")));

        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Australia/Sydney")));
        Assert.assertEquals("0 0 5 2 3 ? 1999", endCron.getValue());
        Assert.assertEquals("Australia/Sydney", endCronTimezone.getValue());
    }

    @Test
    public void should_create_instance_and_populate_params_and_save_instance_with_agents_singapore_timezone() throws Exception {
        this.context.setTimezone("Asia/Singapore");
        // 5 hours TTL
        this.context.setContextTtlMilliseconds(1000 * 60 * 60 * 5);
        this.record.setContext(context);
        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<String> endCron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCronTimezone = ArgumentCaptor.forClass(String.class);
        verify(contextInstanceSchedulerService).registerEndJobAndTrigger(anyString(), endCron.capture(), endCronTimezone.capture(), anyString());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.contextInstanceSchedulerService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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

        Assert.assertEquals(920322000000L, contextMachine.getContext().getProjectedEndTime());

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(contextMachine.getContext().getProjectedEndTime())
            , ZoneId.of("Asia/Singapore"));
        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Asia/Singapore")));
        Assert.assertEquals("0 0 5 2 3 ? 1999", endCron.getValue());
        Assert.assertEquals("Asia/Singapore", endCronTimezone.getValue());
    }

    @Test
    public void should_create_instance_for_plan_that_end_upon_completion_but_created_null_context_instances_result() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(null);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void should_create_instance_for_plan_that_end_upon_completion_but_created_empty_context_instances_result() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(this.searchResults);
        when(this.searchResults.getTotalNumberOfResults()).thenReturn(0L);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void plan_that_end_upon_completion_but_created_context_instances_result_outside_time_window() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(this.searchResults);
        when(this.searchResults.getTotalNumberOfResults()).thenReturn(1L);
        when(this.searchResults.getResultList()).thenReturn(List.of(this.scheduledContextInstanceRecord));
        when(this.scheduledContextInstanceRecord.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.getStartTime()).thenReturn(0L);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void plan_that_end_upon_completion_but_created_context_instances_result_outside_time_window_with_cron_expression() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.context.setTimeWindowStart("0 0 0 * * ?");
        this.context.setContextTtlMilliseconds(1L);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(this.searchResults);
        when(this.searchResults.getTotalNumberOfResults()).thenReturn(1L);
        when(this.searchResults.getResultList()).thenReturn(List.of(this.scheduledContextInstanceRecord));
        when(this.scheduledContextInstanceRecord.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.getStartTime()).thenReturn(System.currentTimeMillis());

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void plan_that_end_upon_completion_but_created_context_instances_result_run_in_past() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.context.setTimeWindowStart("0 0 0 * * ?");
        this.context.setContextTtlMilliseconds(1L);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(this.searchResults);
        when(this.searchResults.getTotalNumberOfResults()).thenReturn(1L);
        when(this.searchResults.getResultList()).thenReturn(List.of(this.scheduledContextInstanceRecord));
        when(this.scheduledContextInstanceRecord.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.getStartTime()).thenReturn(0L);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void plan_that_end_upon_completion_but_NOT_create_context_instances_result_inside_time_window() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.context.setTimeWindowStart("0 0 0 * * ?");
        this.context.setContextTtlMilliseconds(24L * 50L * 60L * 1000L);
        this.context.setTimezone(ZoneId.systemDefault().getId());
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(this.searchResults);
        when(this.searchResults.getTotalNumberOfResults()).thenReturn(1L);
        when(this.searchResults.getResultList()).thenReturn(List.of(this.scheduledContextInstanceRecord));
        when(this.scheduledContextInstanceRecord.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.getStartTime()).thenReturn(System.currentTimeMillis());

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), anyString(), anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNull(ContextMachineCache.instance().getFirstByContextName(contextName));
    }

    @Test
    public void plan_that_end_upon_completion_but_NOT_create_context_instance_as_inside_blackout_window_cron() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.context.setTimeWindowStart("0 0 0 * * ?");
        this.context.setContextTtlMilliseconds(1L);
        String cron = "* * * * * ? 1900-2900";
        List<String> cronExpressions = new ArrayList<>();
        cronExpressions.add(cron);
        this.context.setBlackoutWindowCronExpressions(cronExpressions);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(this.timeService.getDateNow()).thenReturn(new Date());

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        verify(this.timeService).getDateNow();

        verifyNoMoreInteractions(scheduledContextInstanceService,
            timeService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNull(ContextMachineCache.instance().getFirstByContextName(contextName));
    }

    @Test
    public void plan_that_end_upon_completion_but_NOT_create_context_instance_as_inside_blackout_window_date_range() throws Exception {
        this.context.setEndJobPlanUponCompletion(true);
        this.context.setTimeWindowStart("0 0 0 * * ?");
        this.context.setContextTtlMilliseconds(1L);
        this.context.setBlackoutWindowDateTimeRanges(Map.of(0L, 4100112000000L));
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(this.timeService.getDateNow()).thenReturn(new Date());

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        verify(this.timeService).getDateNow();

        verifyNoMoreInteractions(scheduledContextInstanceService,
            timeService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNull(ContextMachineCache.instance().getFirstByContextName(contextName));
    }

    @Test
    public void should_create_instance_and_populate_params_and_save_instance_with_agents_requires_agent_synchronisation() throws Exception {
        // Set flags so that jobs will be synchronised with the agent.
        this.context.setDelayAgentSynchronisationUntilNextInstance(true);
        this.context.setRequiresAgentSynchronisation(true);
        this.record.setContext(context);

        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(this.schedulerJobService.findByContext(anyString(), anyInt(), anyInt()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        when(scheduledContextService.findById(contextName)).thenReturn(record);

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        // execute
        backFiller.run();

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextParametersUpdateService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(scheduledContextService, times(1)).findById(contextName);
        verify(scheduledContextService, times(1)).save(any());
        verify(schedulerJobService, times(1)).findByContext(anyString(), anyInt(), anyInt());
        verify(jobProvisionService, times(1)).provisionJobs(any(), anyString());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
    public void should_create_instance_and_populate_params_and_save_instance_with_no_agents() {
        // set up
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(0);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());

        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));

        // execute
        backFiller.run();

        // verify
        verify(scheduledContextInstanceService, times(2)).save(any());
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            scheduledContextInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName(contextName));

        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
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
}