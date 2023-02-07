package org.ikasan.orchestration.service.context.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.JobLockCacheInitialisationServiceImpl;
import org.ikasan.orchestration.service.utils.*;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
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

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.time.ZonedDateTime.now;
import static org.ikasan.orchestration.service.utils.ScheduledContextRecordTestSearchResults.CONTEXT_NAME;
import static org.ikasan.spec.scheduled.instance.model.InstanceStatus.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRecoveryServiceImplTest {
    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private JobInitiationService jobInitiationService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Mock
    ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;

    @Mock
    SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;

    @Mock
    JobLockCacheInitialisationServiceImpl jobLockCacheInitialisationService;

    @Mock
    private ExecutorService executor;

    @Mock
    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    @Mock
    private TimeService timeService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private ContextInstanceRecoveryServiceImpl contextInstanceRecoveryServiceImpl;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Before
    public void setUp() {
        ContextMachineCache.instance().resetAllCache();
        JobLockCacheImpl.instance().reset();

        // Stub this Implementation due to two difference SchedulerJobInstance can be returned
        schedulerJobInstanceService = new StubSchedulerJobInstanceServiceTestImpl();
        
        contextInstanceRecoveryServiceImpl = new ContextInstanceRecoveryServiceImpl(
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
            timeService
        );

        ReflectionTestUtils.setField(contextInstanceRecoveryServiceImpl, "executor", executor);
    }

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void should_do_nothing_if_outside_of_operating_window_no_contexts() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(0, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(timeService).getDateNow();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_call_executor_to_create_context_inside_operating_window_no_instance() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jobLockInstance, "jobLockCacheData"));
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(executor).execute(any(MissingContextInstanceRecoveryRunnable.class));
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_not_call_executor_to_create_context_inside_operating_window_no_instance_but_context_disabled() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jobLockInstance, "jobLockCacheData"));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
        );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_do_nothing_context_machine_outside_of_operating_window() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(timeService).getDateNow();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_do_nothing_context_machine_outside_of_operating_window_no_instance() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\" : \"CONTEXT-1436221681\"", "\"name\" : \"" + CONTEXT_NAME + "\"");
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(CONTEXT_NAME);

        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(3, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(3, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalJobResults.getResultList());
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache((JobLockCacheData) ReflectionTestUtils.getField(jobLockInstance, "jobLockCacheData"));
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(3)).populateContextParameters();
        verify(contextParametersInstanceService, times(3)).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(3)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(9)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService
            );

        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName1"));
        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName2"));
        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName3"));

        assertEquals(3, ContextMachineCache.instance().contextNames().size());

        verifyContextMachine("ContextName1");
        verifyContextMachine("ContextName2");
        verifyContextMachine("ContextName3");
    }

    @Test
    public void should_create_context_machine_no_agents() {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(0);
        schedulerJobInstanceService.save(internalJobResults.getResultList());
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(0);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class));
        verify(timeService).getDateNow();

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals("ContextName1", actualContextInstanceRecord.getContextName());
        assertEquals(WAITING.name(), actualContextInstanceRecord.getStatus());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService
            );

        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName("ContextName1");
        assertNotNull(contextMachine);
        assertEquals(1, ContextMachineCache.instance().contextNames().size());
        verifyContextMachine("ContextName1");
    }

    @Test
    public void does_nothing_if_no_context_instance_records_or_context_records() {
        // set up
        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);
        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(0, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            executor,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    private void verifyContextMachine(String contextName) {
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

    private List<InstanceStatus> getStatusesToLookFor() {
        return List.of(WAITING, RUNNING, ERROR);
    }
}