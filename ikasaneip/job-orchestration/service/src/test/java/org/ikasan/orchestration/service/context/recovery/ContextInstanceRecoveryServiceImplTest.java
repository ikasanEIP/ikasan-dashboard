package org.ikasan.orchestration.service.context.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.JobLockCacheInitialisationServiceImpl;
import org.ikasan.orchestration.service.utils.*;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
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
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.ZonedDateTime.now;
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
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Mock
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    @Mock
    JobUtilsService jobUtilsService;

    @Mock
    private TimeService timeService;
    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    @Mock
    private JobProvisionService jobProvisionService;
    @Mock
    private SchedulerJobService schedulerJobService;
    @Mock
    private ScheduledContextRecord scheduledContextRecord;

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
            timeService,
            contextInstanceRegistrationService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            true
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService,
            timeService
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void should_call_executor_to_create_context_inside_operating_window_no_instance() throws IOException {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared.setContextInstance(contextInstance);
        prepared.setContextName("ContextName1");
        prepared.setStartTime(System.currentTimeMillis() + 100000L);

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared), 0, 1));
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService).findAll();
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
            );

        // ensure prepared context in cache
        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName1"));
        assertEquals(InstanceStatus.PREPARED, ContextMachineCache.instance().getFirstByContextName("ContextName1").getContext().getStatus());
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
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
        );

        // ensure cache is empty
        assertNull(ContextMachineCache.instance().getFirstByContextName("ContextName1"));
    }

    @Test
    public void should_do_nothing_context_machine_outside_of_operating_window() throws IOException {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1), 1, 1));

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
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextInstanceService, times(2)).save(any()); // updating two context to InstanceStatus.ENDED
        verify(scheduledContextInstanceService, times(2)).save(any());
        verify(scheduledContextService).findAll();
        verify(timeService).getDateNow();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(contextInstancePublicationService, times(6)).remove(anyString(), any()); // due to updating 2 instances to Ended, will remove contextId from 3 agent as mocking 3 agents at part of the moduleMetadataService
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
            );

        // ensure prepared context in cache
        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName1"));
        assertEquals(InstanceStatus.PREPARED, ContextMachineCache.instance().getFirstByContextName("ContextName1").getContext().getStatus());
    }

    @Test
    public void should_do_nothing_context_machine_outside_of_operating_window_no_instance() throws IOException {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        // set up
        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(results);

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1), 1, 1));

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
        verify(scheduledContextInstanceService).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService).findAll();
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
            );

        // ensure prepared context in cache
        assertNotNull(ContextMachineCache.instance().getFirstByContextName("ContextName1"));
        assertEquals(InstanceStatus.PREPARED, ContextMachineCache.instance().getFirstByContextName("ContextName1").getContext().getStatus());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared2 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName2");
        contextInstance.setStatus(PREPARED);
        prepared2.setContextInstance(contextInstance);
        prepared2.setContextName("ContextName2");
        prepared2.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared3 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName3");
        contextInstance.setStatus(PREPARED);
        prepared3.setContextInstance(contextInstance);
        prepared3.setContextName("ContextName3");
        prepared3.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1, prepared2, prepared3), 1, 1));


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
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(12)).populateContextParameters();
        verify(contextParametersInstanceService, times(12)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(22)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(12)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(36)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
            );

        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName1").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName2").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName3").size());

        assertEquals(3, ContextMachineCache.instance().contextNames().size());
        assertEquals(6, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(3, preparedCount.get());
        assertEquals(3, waitingCount.get());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_requires_agent_synchronisation() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared2 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName2");
        contextInstance.setStatus(PREPARED);
        prepared2.setContextInstance(contextInstance);
        prepared2.setContextName("ContextName2");
        prepared2.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared3 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName3");
        contextInstance.setStatus(PREPARED);
        prepared3.setContextInstance(contextInstance);
        prepared3.setContextName("ContextName3");
        prepared3.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1, prepared2, prepared3), 1, 1));


        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(3, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);

        // Set the context to require a delayed synchronisation.
        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(3, false, false, true);
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
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(12)).populateContextParameters();
        verify(contextParametersInstanceService, times(12)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(22)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(12)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(36)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName1").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName2").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName3").size());

        assertEquals(3, ContextMachineCache.instance().contextNames().size());
        assertEquals(6, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(3, preparedCount.get());
        assertEquals(3, waitingCount.get());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_2nd_weekday_of_month() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context-nth-business-day-of-month.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared2 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName2");
        contextInstance.setStatus(PREPARED);
        prepared2.setContextInstance(contextInstance);
        prepared2.setContextName("ContextName2");
        prepared2.setStartTime(System.currentTimeMillis() + 100000L);

        ScheduledContextInstanceRecordImpl prepared3 = new ScheduledContextInstanceRecordImpl();
        contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName3");
        contextInstance.setStatus(PREPARED);
        prepared3.setContextInstance(contextInstance);
        prepared3.setContextName("ContextName3");
        prepared3.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1, prepared2, prepared3), 1, 1));


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
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(12)).populateContextParameters();
        verify(contextParametersInstanceService, times(12)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(22)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(12)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(36)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName1").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName2").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName3").size());

        assertEquals(3, ContextMachineCache.instance().contextNames().size());
        assertEquals(6, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(3, preparedCount.get());
        assertEquals(3, waitingCount.get());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_2nd_weekday_of_month_before_now_sydney() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context-nth-business-day-of-month.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() - 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        contextInstance.setTimeWindowStart("0 0 0 2 3 ? 1999");
        contextInstance.setTimezone("Australia/Sydney");
        contextInstance.setContextTtlMilliseconds(1000*60*60*5);
        contextInstance.setId("identifier");
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() - 100000L);


        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1), 1, 1));


        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(1, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalJobResults.getResultList());
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));
        when(this.scheduledContextService.findById(anyString())).thenReturn(this.scheduledContextRecord);
        ContextTemplate jobPlan = new ContextTemplateImpl();
        jobPlan.setName("jobPlan");
        jobPlan.setTimeWindowStart("0 0 0 2 3 ? 1999");
        jobPlan.setTimezone("Australia/Sydney");
        jobPlan.setContextTtlMilliseconds(1000*60*60*5);
        when(this.scheduledContextRecord.getContext()).thenReturn(jobPlan);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        Thread.sleep(2000);

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(1)).populateContextParameters();
        verify(contextParametersInstanceService, times(1)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(4)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(3)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(contextInstancePublicationService, times(6)).remove(anyString(), any());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService, times(2)).findById(anyString());
        verify(contextInstanceStateChangeEventBroadcaster, times(3)).broadcast(any());
        verify(timeService, times(4)).getLocalDateNow();

        ArgumentCaptor<String> endCron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCronTimezone = ArgumentCaptor.forClass(String.class);
        verify(contextInstanceSchedulerService, times(1)).registerEndJobAndTrigger(anyString(), endCron.capture(), endCronTimezone.capture(), anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            contextInstanceSchedulerService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(1, ContextMachineCache.instance().getAllByContextName("ContextName1").size());

        assertEquals(1, ContextMachineCache.instance().contextNames().size());
        assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(1, waitingCount.get());

        ContextInstance instance = ContextMachineCache.instance().getByContextInstanceId("identifier").getContext();
        Assert.assertEquals(920311200000L, instance.getProjectedEndTime());

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instance.getProjectedEndTime())
            , ZoneId.of("Australia/Sydney"));
        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Australia/Sydney")));

        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Australia/Sydney")));
        Assert.assertEquals("0 0 5 2 3 ? 1999", endCron.getValue());
        Assert.assertEquals("Australia/Sydney", endCronTimezone.getValue());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_2nd_weekday_of_month_before_now_singapore() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context-nth-business-day-of-month.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() - 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        contextInstance.setTimeWindowStart("0 0 0 2 3 ? 1999");
        contextInstance.setTimezone("Asia/Singapore");
        contextInstance.setContextTtlMilliseconds(1000*60*60*5);
        contextInstance.setId("identifier");
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() - 100000L);


        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1), 1, 1));


        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(1, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalJobResults.getResultList());
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));
        when(this.scheduledContextService.findById(anyString())).thenReturn(this.scheduledContextRecord);
        ContextTemplate jobPlan = new ContextTemplateImpl();
        jobPlan.setName("jobPlan");
        jobPlan.setTimeWindowStart("0 0 0 2 3 ? 1999");
        jobPlan.setTimezone("Australia/Sydney");
        jobPlan.setContextTtlMilliseconds(1000*60*60*5);
        when(this.scheduledContextRecord.getContext()).thenReturn(jobPlan);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        Thread.sleep(2000);

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(1)).populateContextParameters();
        verify(contextParametersInstanceService, times(1)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(4)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(3)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(contextInstancePublicationService, times(6)).remove(anyString(), any());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService, times(2)).findById(anyString());
        verify(contextInstanceStateChangeEventBroadcaster, times(3)).broadcast(any());
        verify(timeService, times(4)).getLocalDateNow();

        ArgumentCaptor<String> endCron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCronTimezone = ArgumentCaptor.forClass(String.class);
        verify(contextInstanceSchedulerService, times(1)).registerEndJobAndTrigger(anyString(), endCron.capture(), endCronTimezone.capture(), anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            contextInstanceSchedulerService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(1, ContextMachineCache.instance().getAllByContextName("ContextName1").size());

        assertEquals(1, ContextMachineCache.instance().contextNames().size());
        assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(1, waitingCount.get());

        ContextInstance instance = ContextMachineCache.instance().getByContextInstanceId("identifier").getContext();
        Assert.assertEquals(920322000000L, instance.getProjectedEndTime());

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instance.getProjectedEndTime())
            , ZoneId.of("Asia/Singapore"));
        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Asia/Singapore")));

        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Asia/Singapore")));
        Assert.assertEquals("0 0 5 2 3 ? 1999", endCron.getValue());
        Assert.assertEquals("Asia/Singapore", endCronTimezone.getValue());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_2nd_weekday_of_month_before_now_london() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context-nth-business-day-of-month.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared1 = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() - 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        contextInstance.setTimeWindowStart("0 0 0 2 3 ? 1999");
        contextInstance.setTimezone("Europe/London");
        contextInstance.setContextTtlMilliseconds(1000*60*60*5);
        contextInstance.setId("identifier");
        prepared1.setContextInstance(contextInstance);
        prepared1.setContextName("ContextName1");
        prepared1.setStartTime(System.currentTimeMillis() - 100000L);


        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared1), 1, 1));


        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(1, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(getStatusesToLookFor())).thenReturn(instanceResults);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalJobResults.getResultList());
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults = new GlobalEventJobTestSearchResults(1);
        schedulerJobInstanceService.save(globalEventJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));
        when(this.scheduledContextService.findById(anyString())).thenReturn(this.scheduledContextRecord);
        ContextTemplate jobPlan = new ContextTemplateImpl();
        jobPlan.setName("jobPlan");
        jobPlan.setTimeWindowStart("0 0 0 2 3 ? 1999");
        jobPlan.setTimezone("Australia/Sydney");
        jobPlan.setContextTtlMilliseconds(1000*60*60*5);
        when(this.scheduledContextRecord.getContext()).thenReturn(jobPlan);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));

        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        Thread.sleep(2000);

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(1)).populateContextParameters();
        verify(contextParametersInstanceService, times(1)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(4)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(3)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(contextInstancePublicationService, times(6)).remove(anyString(), any());
        verify(scheduledContextInstanceService, times(3)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService, times(2)).findById(anyString());
        verify(contextInstanceStateChangeEventBroadcaster, times(3)).broadcast(any());
        verify(timeService, times(4)).getLocalDateNow();

        ArgumentCaptor<String> endCron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endCronTimezone = ArgumentCaptor.forClass(String.class);
        verify(contextInstanceSchedulerService, times(1)).registerEndJobAndTrigger(anyString(), endCron.capture(), endCronTimezone.capture(), anyString());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            contextInstanceSchedulerService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(1, ContextMachineCache.instance().getAllByContextName("ContextName1").size());

        assertEquals(1, ContextMachineCache.instance().contextNames().size());
        assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(1, waitingCount.get());

        ContextInstance instance = ContextMachineCache.instance().getByContextInstanceId("identifier").getContext();
        Assert.assertEquals(920350800000L, instance.getProjectedEndTime());

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instance.getProjectedEndTime())
            , ZoneId.of("Europe/London"));
        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Europe/London")));

        Assert.assertEquals(zonedDateTime, ZonedDateTime.of(LocalDateTime.of(LocalDate.of(1999, 03, 02)
            , LocalTime.of(5,0)), ZoneId.of("Europe/London")));
        Assert.assertEquals("0 0 5 2 3 ? 1999", endCron.getValue());
        Assert.assertEquals("Europe/London", endCronTimezone.getValue());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window_no_prepared() throws Exception {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextRecord scheduledContextRecord1 = new ScheduledContextRecordImpl();
        ContextTemplateImpl contextTemplate = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        contextTemplate.setName("ContextName1");
        scheduledContextRecord1.setContextName("ContextName1");
        scheduledContextRecord1.setContext(contextTemplate);

        when(this.scheduledContextService.findById("ContextName1")).thenReturn(scheduledContextRecord1);

        ScheduledContextRecord scheduledContextRecord2 = new ScheduledContextRecordImpl();
        contextTemplate = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        contextTemplate.setName("ContextName2");
        scheduledContextRecord2.setContextName("ContextName2");
        scheduledContextRecord2.setContext(contextTemplate);

        when(this.scheduledContextService.findById("ContextName2")).thenReturn(scheduledContextRecord2);

        ScheduledContextRecord scheduledContextRecord3 = new ScheduledContextRecordImpl();
        contextTemplate = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        contextTemplate.setName("ContextName3");
        scheduledContextRecord3.setContextName("ContextName3");
        scheduledContextRecord3.setContext(contextTemplate);

        when(this.scheduledContextService.findById("ContextName3")).thenReturn(scheduledContextRecord3);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 1, 1));


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
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData());
        when(timeService.getDateNow()).thenReturn(Date.from(now(ZoneId.of("Europe/London")).toInstant()));
        when(timeService.getLocalDateNow()).thenReturn(LocalDateTime.now());
        // execute
        contextInstanceRecoveryServiceImpl.recoverInstances();

        // verify
        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(getStatusesToLookFor());
        verify(scheduledContextService).findAll();
        verify(contextParametersInstanceService, times(12)).populateContextParameters();
        verify(contextParametersInstanceService, times(12)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(moduleMetadataService, times(16)).find(any(), any(), eq(-1), eq(-1));
        verify(timeService, times(1)).getDateNow();
        verify(timeService, times(6)).getLocalDateNow();
        verify(scheduledContextInstanceService, times(15)).save(any(ScheduledContextInstanceRecord.class));
        verify(contextInstancePublicationService, times(36)).publish(any(String.class), any(ContextInstance.class));
        verify(contextInstancePublicationService, times(3)).removeAll(anyString());
        verify(scheduledContextInstanceService, times(6)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(scheduledContextService, times(3)).findById(any());
        verify(contextInstanceStateChangeEventBroadcaster, times(3)).broadcast(any());

        verifyNoMoreInteractions(scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
        );

        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName1").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName2").size());
        assertEquals(2, ContextMachineCache.instance().getAllByContextName("ContextName3").size());

        assertEquals(3, ContextMachineCache.instance().contextNames().size());
        assertEquals(6, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(3, preparedCount.get());
        assertEquals(3, waitingCount.get());
    }

    @Test
    public void should_create_context_machine_no_agents() throws IOException {
        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ScheduledContextInstanceRecordImpl prepared = new ScheduledContextInstanceRecordImpl();
        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setStartTime(System.currentTimeMillis() + 100000L);
        contextInstance.setName("ContextName1");
        contextInstance.setStatus(PREPARED);
        prepared.setContextInstance(contextInstance);
        prepared.setContextName("ContextName1");
        prepared.setStartTime(System.currentTimeMillis() + 100000L);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared), 1, 1));

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
        verify(moduleMetadataService, times(4)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService, times(2)).populateContextParameters();
        verify(contextParametersInstanceService, times(2)).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(timeService).getDateNow();
        verify(scheduledContextInstanceService, times(1)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(2)).save(contextInstanceCaptor.capture());
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            timeService,
            this.jobProvisionService,
            this.schedulerJobService
            );

        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName("ContextName1");
        assertNotNull(contextMachine);
        assertEquals(1, ContextMachineCache.instance().contextNames().size());
        assertEquals(3, ContextMachineCache.instance().contextInstanceIdentifiers().size());

        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        ContextMachineCache.instance().contextInstanceIdentifiers().forEach(id -> {
            if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(WAITING)) {
                verifyContextMachineById(id);
                waitingCount.getAndIncrement();
            }
            else if(ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(PREPARED)) {
                preparedCount.getAndIncrement();
            }
        });

        assertEquals(1, preparedCount.get());
        assertEquals(2, waitingCount.get());
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService
            );

        // ensure no contexts
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    private void verifyContextMachineById(String contextId) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextId);
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
        return List.of(WAITING, RUNNING, ERROR, COMPLETE);
    }

    /**
     * Retrieves a specific JobLockCacheData object from the jobLockCacheDataMap based on the key "environment".
     * This method accesses the underlying ConcurrentHashMap containing JobLockCacheData objects via reflection.
     *
     * Note that this method is protected and should be used according to the access control restrictions of the class.
     */
    protected JobLockCacheData getJobLockCacheData() {
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(JobLockCacheImpl.instance(), "jobLockCacheDataMap");
        return jobLockCacheDataMap.get("environment");
    }
}