package org.ikasan.orchestration.service.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.utils.*;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
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
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEventService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    @Mock
    private TimeService timeService;
    @Mock
    private ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster;
    @Mock
    private SystemEventService systemEventService;
    @Mock
    private JobUtilsService jobUtilsService;

    @Mock
    private SchedulerJobInstanceService mockSchedulerJobInstanceService;

    @Mock
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    @Mock
    private InternalEventDrivenJobInstance mockInternalEventDrivenJobInstance;
    @Mock
    private JobProvisionService jobProvisionService;
    @Mock
    private SchedulerJobService schedulerJobService;

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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            true);
        ContextMachineCache.instance().resetAllCache();
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @After
    public void tearDown() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test(expected = RuntimeException.class)
    public void register_with_params_null_ScheduledContextRecord() {
        // set up
        when(scheduledContextService.findById(contextName)).thenReturn(null);

        // execute
        contextInstanceRegistrationService.register(contextName, null, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void register_null_ScheduledContextRecord() {
        // set up
        when(scheduledContextService.findById(contextName)).thenReturn(null);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void not_scheduler_instance_register() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.register("contextName", this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void not_scheduler_instance_register_with_params() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.register("contextName", List.of(), this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void not_scheduler_instance_deregister_by_id() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.deRegisterById("contextId");

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void not_scheduler_instance_deregister_by_name() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.deRegisterByName("contextName", this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void not_scheduler_instance_deregister_manually() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.deregisterManually("contextId");

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void not_scheduler_instance_reschedule() {
        ContextInstanceRegistrationServiceImpl registrationService = new ContextInstanceRegistrationServiceImpl (
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);

        registrationService.reSchedule("contextName", this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );
    }

    @Test
    public void register_with_params_with_agents_not_found_module_metadata() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
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

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName
            , null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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
    public void register_with_agents_not_found_module_metadata() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(), 0, 0));
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_params_with_agents() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

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
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_agents_requires_agent_synchronisation() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);

        // Set flags so that jobs will be synchronised with the agent.
        context.setDelayAgentSynchronisationUntilNextInstance(true);
        context.setRequiresAgentSynchronisation(true);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));
        when(this.schedulerJobService.findByContext(anyString(), anyInt(), anyInt()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(3)).findById(contextName);
        verify(scheduledContextService, times(1)).save(any());
        verify(schedulerJobService, times(1)).findByContext(anyString(), anyInt(), anyInt());
        verify(jobProvisionService, times(1)).provisionJobs(any(), anyString());
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_agents_requires_agent_synchronisation_with_jobs_to_provision() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);

        // Set flags so that jobs will be synchronised with the agent.
        context.setDelayAgentSynchronisationUntilNextInstance(true);
        context.setRequiresAgentSynchronisation(true);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SchedulerJob job = new InternalEventDrivenJobImpl();
        job.setIdentifier("scheduler-agent--801842677");
        TestSchedulerJobRecord schedulerJobRecord = new TestSchedulerJobRecord();
        schedulerJobRecord.setJob(job);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));
        when(this.schedulerJobService.findByContext(anyString(), anyInt(), anyInt()))
            .thenReturn(new SearchResultsImpl<>(List.of(schedulerJobRecord), 1, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(3)).findById(contextName);
        verify(scheduledContextService, times(1)).save(any());
        verify(schedulerJobService, times(1)).findByContext(anyString(), anyInt(), anyInt());
        ArgumentCaptor<List<SchedulerJob>> provisionedJobs = ArgumentCaptor.forClass(ArrayList.class);
        verify(jobProvisionService, times(1)).provisionJobs(provisionedJobs.capture(), anyString());
        Assert.assertEquals(1, provisionedJobs.getValue().size());
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_agents_2nd_business_day_of_month() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-nth-business-day-of-month.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);
        when(timeService.getLocalDateNow()).thenReturn(LocalDateTime.now());
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

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
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(timeService, times(2)).getLocalDateNow();
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_params_with_agents_and_context_parameters() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
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

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
        contextParameterInstance.setName("name1");
        contextParameterInstance.setValue("value1");
        contextParameterInstance.setDefaultValue("defaul1");

        contextParameterInstances.add(contextParameterInstance);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register(contextName
            , contextParameterInstances, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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
    public void register_with_agents_and_context_parameters() throws Exception {
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
        contextParameterInstance.setName("name1");
        contextParameterInstance.setValue("value1");
        contextParameterInstance.setDefaultValue("defaul1");

        contextParameterInstances.add(contextParameterInstance);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_params_non_concurrent_plan_and_assert_second_plan_is_not_registered() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setAbleToRunConcurrently(false);

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

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
        contextParameterInstance.setName("name1");
        contextParameterInstance.setValue("value1");
        contextParameterInstance.setDefaultValue("defaul1");

        contextParameterInstances.add(contextParameterInstance);

        // execute
        String contextInstanceId = contextInstanceRegistrationService.register
            (contextName, contextParameterInstances, this.contextInstanceSchedulerService);

        String secondContextInstanceId = contextInstanceRegistrationService
            .register(contextName, contextParameterInstances, this.contextInstanceSchedulerService);
        assertNull(secondContextInstanceId);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(systemEventService, times(1)).logSystemEvent(anyString(), anyString(), anyString());
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService,
            systemEventService
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
    public void register_non_concurrent_plan_and_assert_second_plan_is_not_registered() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setAbleToRunConcurrently(false);
        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
        contextParameterInstance.setName("name1");
        contextParameterInstance.setValue("value1");
        contextParameterInstance.setDefaultValue("defaul1");

        contextParameterInstances.add(contextParameterInstance);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(3)).findById(contextName);
        verify(systemEventService, times(1)).logSystemEvent(anyString(), anyString(), anyString());
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService,
            systemEventService
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_non_concurrent_plan_and_assert_second_plan_is_not_registered_prepared_already_in_cache() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setAbleToRunConcurrently(false);
        record.setContext(context);
        record.setContextName(contextName);

        ScheduledContextInstanceRecordImpl prepared = new ScheduledContextInstanceRecordImpl();
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());

        ContextInstance contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);
        contextInstance.setAbleToRunConcurrently(false);
        contextInstance.setName(contextName);
        contextInstance.setStatus(InstanceStatus.PREPARED);
        prepared.setContextInstance(contextInstance);
        prepared.setContextName(contextName);

        when(scheduledContextService.findById(contextName)).thenReturn(record);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(3);
        schedulerJobInstanceService.save(internalEventDrivenJobRecordSearchResults.getResultList());
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(prepared), 0, 1));

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        List<ContextParameterInstance> contextParameterInstances = new ArrayList<>();
        ContextParameterInstance contextParameterInstance = new ContextParameterInstanceImpl();
        contextParameterInstance.setName("name1");
        contextParameterInstance.setValue("value1");
        contextParameterInstance.setDefaultValue("defaul1");

        contextParameterInstances.add(contextParameterInstance);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(3)).findById(contextName);
        verify(systemEventService, times(1)).logSystemEvent(anyString(), anyString(), anyString());
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            schedulerJobStateChangeEventBroadcaster,
            this.jobProvisionService,
            this.schedulerJobService,
            systemEventService
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }


    @Test
    public void register_with_params_agents_outside_cron_blackout_window_so_should_register() throws Exception {
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);


        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_params_with_agents_outside_datetime_window_so_should_register() throws Exception {
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
    }

    @Test
    public void register_with_params_with_agents_outside_datetime_window_with_timezone_so_should_register() throws Exception {
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(moduleMetadataService, times(2)).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster, times(2)).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(), any());

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachines = ContextMachineCache.instance().getAllByContextName(contextName);
        assertNotNull(contextMachines);
        assertEquals(2, contextMachines.size());

        contextMachines.forEach(contextMachine -> {
            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
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
            else if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener
                    = (SchedulerJobInitiationEventRaisedListener) ReflectionTestUtils.getField(contextMachine, "schedulerJobInitiationEventRaisedListener");
                assertNull(schedulerJobInitiationEventRaisedListener);

                List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners
                    = (List<ContextInstanceStateChangeEventListener>) ReflectionTestUtils.getField(contextMachine, "contextInstanceStateChangeEventListeners");
                assertNotNull(contextInstanceStateChangeEventListeners);
                assertEquals(0, contextInstanceStateChangeEventListeners.size());

                JobLogicMachine jobLogicMachine = (JobLogicMachine) ReflectionTestUtils.getField(contextMachine, "jobLogicMachine");
                assertNotNull(jobLogicMachine);
                List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners
                    = (List<SchedulerJobInstanceStateChangeEventListener>) ReflectionTestUtils.getField(jobLogicMachine, "schedulerJobInstanceStateChangeEventListeners");
                assertNotNull(schedulerJobInstanceStateChangeEventListeners);
                assertEquals(2, schedulerJobInstanceStateChangeEventListeners.size());
            }
        });
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_params_with_agents_inside_datetime_blackout_window_so_should_not_register() throws Exception {
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

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_agents_inside_datetime_blackout_window_so_should_not_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-inside-focused.json").getInputStream().readAllBytes());

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        long now = System.currentTimeMillis();
        context.setTimeWindowStart(CronUtils.buildCronFromOriginalWithMillisecondOffset(now, 1000000L, ZoneId.systemDefault().toString()));
        context.setBlackoutWindowDateTimeRanges(Map.of(now-200000L, now+2000000L));

        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
       contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        List<ContextMachine> contextMachine = ContextMachineCache.instance().getAllByContextName(contextName);
        assertTrue(contextMachine.isEmpty());
    }

    @Test
    public void register_with_agents_inside_datetime_blackout_window_so_should_not_register_register_prepared() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-inside-focused.json").getInputStream().readAllBytes());

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        long now = System.currentTimeMillis();
        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(now, 1000000L, ZoneId.systemDefault().toString());
        cron = cron.substring(0, cron.lastIndexOf(" "));
        cron = cron + " *";
        context.setTimeWindowStart(cron);
        context.setBlackoutWindowDateTimeRanges(Map.of(now-200000L, now+2000000L));

        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1)))
            .thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1"), TestUtils.createModuleMetaData("2")
                , TestUtils.createModuleMetaData("3")), 3, 0));

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(contextInstanceStateChangeEventBroadcaster, times(1)).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(1)).save(contextInstanceCaptor.capture());
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
        assertNotNull(contextMachine);
        assertEquals(InstanceStatus.PREPARED, contextMachine.getContext().getStatus());
    }

    @Test
    public void register_with_params_with_agents_inside_datetime_with_timezone_blackout_window_so_should_not_register() throws Exception {
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

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
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
        context.setName(contextName);
        long now = System.currentTimeMillis();
        context.setTimeWindowStart(CronUtils.buildCronFromOriginalWithMillisecondOffset(now, 1000000L, ZoneId.systemDefault().toString()));
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

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
        assertNull(contextMachine);
    }

    @Test
    public void register_with_agents_inside_datetime_with_timezone_blackout_window_so_should_not_register_register_prepared() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-inside.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        String timezone = "Asia/Singapore";
        ZonedDateTime zdtNowInSingapore = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        long now = System.currentTimeMillis();
        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(now, 1000000L, ZoneId.systemDefault().toString());
        cron = cron.substring(0, cron.lastIndexOf(" "));
        cron = cron + " *";
        context.setTimeWindowStart(cron);
        context.setBlackoutWindowDateTimeRanges(Map.of(now-200000L, now+2000000L));

        // Pretend we are in Singapore
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInSingapore.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of( zdtNowInSingapore.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInSingapore.plus(Duration.ofMinutes(200)).toInstant().toEpochMilli()));

        record.setContext(context);
        record.setContextName(contextName);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

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
        contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService, times(2)).findById(contextName);
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        verify(contextInstanceStateChangeEventBroadcaster, times(1)).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(1)).save(contextInstanceCaptor.capture());
        verify(moduleMetadataService, times(1)).find(any(), any(), eq(-1), eq(-1));
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        Assert.assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
        assertNotNull(contextMachine);
        Assert.assertEquals(InstanceStatus.PREPARED, contextMachine.getContext().getStatus());
    }

    @Test
    public void register_with_params_with_disabled_agent() throws Exception {
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
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
       contextInstanceRegistrationService.register(contextName, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().contextInstanceIdentifiers().isEmpty());
    }

    @Test
    public void register_with_agents_with_skipped_jobs() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        String timeZone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timeZone));
        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));
        context.setTimezone(timeZone);

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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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
    public void de_register_context_machine_that_not_in_cache() {
        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
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
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null, null,
            null, null, null, null, null, null, null, moduleMetadataService, JobLockCacheImpl.instance(), null,
            null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService, this.jobUtilsService);

        ContextMachineCache.instance().put(contextMachine);

        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults = new InternalEventDrivenJobTestSearchResults(1);
        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));

        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void deregister_should_save_instance_as_ended_kill_running_jobs() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");


        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);

        AtomicLong pid = new AtomicLong(12345L);
        contextInstance.getScheduledJobs().forEach(job -> {
            ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
            event.setPid(pid.getAndIncrement());
            job.setStatus(InstanceStatus.RUNNING);
            job.setScheduledProcessEvent(event);
        });

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, null
            , null, null, null, null, null
            , moduleMetadataService, JobLockCacheImpl.instance(), null,
            null, this.mockSchedulerJobInstanceService, this.jobLockCacheInitialisationService
            , this.contextInstancePublicationService, this.jobUtilsService);

        ContextMachineCache.instance().put(contextMachine);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));
        when(moduleMetadataService.findById(any())).thenReturn(TestUtils.createModuleMetaData("1"));

        when(this.mockSchedulerJobInstanceService.findByContextIdJobNameChildContextName(anyString(),anyString(), anyString()))
            .thenReturn(this.schedulerJobInstanceRecord);

        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance()).thenReturn(this.mockInternalEventDrivenJobInstance);

        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(moduleMetadataService).findById(anyString());
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verify(mockSchedulerJobInstanceService, times(2)).findByContextIdJobNameChildContextName(anyString(), anyString(), anyString());

        verify(jobUtilsService, times(2)).killJob(anyString(), anyLong(), anyBoolean());

        verify(mockInternalEventDrivenJobInstance, times(2)).setKilled(true);
        verify(mockInternalEventDrivenJobInstance, times(2)).setStatus(InstanceStatus.KILLED);


        verify(mockSchedulerJobInstanceService, times(2)).save(schedulerJobInstanceRecord);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            mockInternalEventDrivenJobInstance,
            jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            mockSchedulerJobInstanceService
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void deregister_should_save_instance_as_ended_kill_running_jobs_same_pid_kill_called_once() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");


        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);

        AtomicLong pid = new AtomicLong(12345L);
        contextInstance.getScheduledJobs().forEach(job -> {
            ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
            event.setPid(pid.get());
            job.setStatus(InstanceStatus.RUNNING);
            job.setScheduledProcessEvent(event);
        });

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, null, null
            , null, null, null, null, moduleMetadataService, JobLockCacheImpl.instance()
            , null, null, this.mockSchedulerJobInstanceService, this.jobLockCacheInitialisationService
            , this.contextInstancePublicationService, this.jobUtilsService);

        ContextMachineCache.instance().put(contextMachine);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));
        when(moduleMetadataService.findById(any())).thenReturn(TestUtils.createModuleMetaData("1"));

        when(this.mockSchedulerJobInstanceService.findByContextIdJobNameChildContextName(anyString(),anyString(), anyString()))
            .thenReturn(this.schedulerJobInstanceRecord);

        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance()).thenReturn(this.mockInternalEventDrivenJobInstance);

        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(moduleMetadataService).findById(anyString());
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verify(mockSchedulerJobInstanceService, times(1)).findByContextIdJobNameChildContextName(anyString(), anyString(), anyString());

        verify(jobUtilsService, times(1)).killJob(anyString(), anyLong(), anyBoolean());

        verify(mockInternalEventDrivenJobInstance, times(1)).setKilled(true);
        verify(mockInternalEventDrivenJobInstance, times(1)).setStatus(InstanceStatus.KILLED);


        verify(mockSchedulerJobInstanceService, times(1)).save(schedulerJobInstanceRecord);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            mockInternalEventDrivenJobInstance,
            jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            mockSchedulerJobInstanceService
        );

        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @Test
    public void deregister_should_save_instance_as_ended_kill_running_jobs_no_process_event_kill_called_once() throws Exception {
        // set up
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");


        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        ContextInstanceImpl contextInstance = objectMapper.readValue(jsonContext, ContextInstanceImpl.class);

        AtomicLong pid = new AtomicLong(12345L);
        ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
        event.setPid(pid.get());
        contextInstance.getScheduledJobs().get(0).setStatus(InstanceStatus.RUNNING);
        contextInstance.getScheduledJobs().get(0).setScheduledProcessEvent(event);

        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null, null
            , null, null, null, null, null
            , null, null, null, moduleMetadataService, JobLockCacheImpl.instance(), null,
            null, this.mockSchedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService
            , this.jobUtilsService);

        ContextMachineCache.instance().put(contextMachine);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));
        when(moduleMetadataService.findById(any())).thenReturn(TestUtils.createModuleMetaData("1"));

        when(this.mockSchedulerJobInstanceService.findByContextIdJobNameChildContextName(anyString(),anyString(), anyString()))
            .thenReturn(this.schedulerJobInstanceRecord);

        when(this.schedulerJobInstanceRecord.getSchedulerJobInstance()).thenReturn(this.mockInternalEventDrivenJobInstance);

        // execute
        contextInstanceRegistrationService.deRegisterByName(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(moduleMetadataService).findById(anyString());
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.ENDED.name(), actualContextInstanceRecord.getStatus());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        assertNull(null, actualContextInstanceRecord.getId());
        assertNotNull(actualContextInstanceRecord.getContextInstance());
        assertTrue(actualContextInstanceRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextInstanceRecord.getTimestamp() <= System.currentTimeMillis());

        verify(mockSchedulerJobInstanceService, times(1)).findByContextIdJobNameChildContextName(anyString(), anyString(), anyString());

        verify(jobUtilsService, times(1)).killJob(anyString(), anyLong(), anyBoolean());

        verify(mockInternalEventDrivenJobInstance, times(1)).setKilled(true);
        verify(mockInternalEventDrivenJobInstance, times(1)).setStatus(InstanceStatus.KILLED);


        verify(mockSchedulerJobInstanceService, times(1)).save(schedulerJobInstanceRecord);

        verifyNoMoreInteractions(
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            scheduledContextService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            mockInternalEventDrivenJobInstance,
            jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            mockSchedulerJobInstanceService
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
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, null, null
            , null, null, null, null, moduleMetadataService, JobLockCacheImpl.instance()
            , null, null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService
            , this.contextInstancePublicationService, this.jobUtilsService);

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
            this.jobProvisionService,
            this.schedulerJobService,
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
        ContextMachine contextMachine = new ContextMachine(context, contextInstance, null
            , null, null, null, null, null
            , null, null, null, null, moduleMetadataService, JobLockCacheImpl.instance()
            , null, null, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService
            , this.contextInstancePublicationService, this.jobUtilsService);

        ContextMachineCache.instance().put(contextMachine);

        when(moduleMetadataService.find(any(), any(), eq(-1), eq(-1))).thenReturn(new ModuleMetadataSearchResults(List.of(TestUtils.createModuleMetaData("1")), 0, 0));

        // execute
        contextInstanceRegistrationService.deregisterManually(contextInstance.getId());

        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextInstancePublicationService).remove(eq(AGENT_URL + "1"), argThat(new CustomBackFillerMatcher(contextInstance, contextName)));

        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

    @Test
    public void reschedule_job_plan() throws Exception {
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
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 1));

        List<ContextParameterInstance> params = TestUtils.createParams();

        ContextInstanceImpl contextInstance = this.objectMapper
            .readValue(this.objectMapper.writeValueAsBytes(record.getContext()), ContextInstanceImpl.class);
        contextInstance.setContextParameters(params);

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);

        // execute
        contextInstanceRegistrationService
            .reSchedule(contextName, this.contextInstanceSchedulerService);

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(scheduledContextInstanceService, times(2)).getScheduledContextInstancesByFilter(any(), eq(-1), eq(-1), isNull(), isNull());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(1)).save(contextInstanceCaptor.capture());
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ScheduledContextInstanceRecord actualContextInstanceRecord = contextInstanceCaptor.getValue();
        assertEquals(contextName, actualContextInstanceRecord.getContextName());
        assertEquals(InstanceStatus.PREPARED.name(), actualContextInstanceRecord.getStatus());
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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        Assert.assertEquals(1, ContextMachineCache.instance().contextInstanceIdentifiers().size());
        ContextMachine contextMachine = ContextMachineCache.instance().getFirstByContextName(contextName);
        assertNotNull(contextMachine);
        Assert.assertEquals(InstanceStatus.PREPARED, contextMachine.getContext().getStatus());
    }

    @Test
    public void reschedule_job_plan_not_scheduler_instance() throws Exception {
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
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            false);


        // execute
        contextInstanceRegistrationService
            .reSchedule(contextName, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        Assert.assertEquals(0, ContextMachineCache.instance().contextInstanceIdentifiers().size());
    }

    @Test(expected = RuntimeException.class)
    public void reschedule_job_plan_null_job_plan_record() throws Exception {
        // set up
        when(scheduledContextService.findById(contextName)).thenReturn(null);

        // execute
        contextInstanceRegistrationService
            .reSchedule(contextName, this.contextInstanceSchedulerService);
    }

    @Test
    public void reschedule_job_plan_disabled_job_plan() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setDisabled(true);
        record.setContext(context);
        record.setContextName(contextName);
        record.setDisabled(true);
        when(scheduledContextService.findById(contextName)).thenReturn(record);

        // execute
        contextInstanceRegistrationService.reSchedule(contextName, this.contextInstanceSchedulerService);

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
            this.jobProvisionService,
            this.schedulerJobService,
            schedulerJobStateChangeEventBroadcaster
        );

        Assert.assertEquals(0, ContextMachineCache.instance().contextInstanceIdentifiers().size());
    }

    /**
     * This test is to modify the Time Window Start to 20 minutes in the past of current time, having a TTL of 30 minutes
     * This is the normal behaviour.
     * <p>
     * Expected that this instance will end in 10 minutes time.
     *
     * @throws Exception
     */
    @Test
    public void register_with_params_with_agents_outside_datetime_window_end_time_in_future_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside-ttl.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        long testExecutionStartTime = System.currentTimeMillis();

        String timezone = ZoneId.systemDefault().toString();
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setContextTtlMilliseconds(1800000);
        context.setTimezone(timezone);
        context.setAbleToRunConcurrently(false);

        // minus 20 minutes and use this as the timeWindowStart for the context
        String cronExpression = CronUtils.buildCronFromOriginalAllDays(System.currentTimeMillis() - 1200000, timezone);
        context.setTimeWindowStart(cronExpression);

        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of(zdtNowInLondon.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInLondon.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        long testExecutionFinishTime = System.currentTimeMillis();

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        // Validate the start time is NOW, and the Proposed end time is 10 minutes in the future
        assert (contextMachine.getContext().getStartTime() >= testExecutionStartTime && contextMachine.getContext().getStartTime() <= testExecutionFinishTime);
        assert (contextMachine.getContext().getProjectedEndTime() >= (testExecutionStartTime + 599000) && contextMachine.getContext().getProjectedEndTime() <= testExecutionFinishTime + 600000);
    }

    /**
     * This test is to modify the Time Window Start to 20 minutes in the past of current time, having a TTL of 10 minutes
     * As this context will be started at current time, the proposed end time will have to be modified because the TTL
     * by default will be used from the time window start, which will put it back to an end time 10 minutes in the past.
     * Expectation is that the proposed end time is now 10 minutes forward.
     *
     * @throws Exception
     */
    @Test
    public void register_with_params_with_agents_outside_datetime_window_end_time_in_past_but_able_to_end_so_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside-ttl.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        long testExecutionStartTime = System.currentTimeMillis();

        String timezone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setContextTtlMilliseconds(600000);
        context.setTimezone(timezone);
        context.setAbleToRunConcurrently(false);

        // minus 20 minutes and use this as the timeWindowStart for the context
        String cronExpression = CronUtils.buildCronFromOriginalAllDays(System.currentTimeMillis() - 1200000, timezone);
        context.setTimeWindowStart(cronExpression);

        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of(zdtNowInLondon.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInLondon.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        long testExecutionFinishTime = System.currentTimeMillis();

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        // Validate the start time is NOW, and the Proposed end time is 10 minutes in the future
        assert (contextMachine.getContext().getStartTime() >= testExecutionStartTime && contextMachine.getContext().getStartTime() <= testExecutionFinishTime);
        assert (contextMachine.getContext().getProjectedEndTime() >= (testExecutionStartTime + 600000) && contextMachine.getContext().getProjectedEndTime() <= testExecutionFinishTime + 600000);
    }

    /**
     * This test is to modify the Time Window Start to 5 minutes in the future of current time, having a TTL of 15 minutes
     * As this context will be started at current time, the proposed end time will have to be modified because the TTL
     * by default will be used from the time window start, which will put it 23 hours and 50 minutes in the past.
     * <p>
     * Expectation is that the proposed end time is now 59/60 seconds (rounding due to cron not handing milliseconds) before the next start time.
     * This is because concurrency is off
     *
     * @throws Exception
     */
    @Test
    public void register_with_params_with_agents_outside_datetime_window_end_time_in_past_but_able_to_end_before_next_instance_start_so_should_register() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside-ttl.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        long testExecutionStartTime = System.currentTimeMillis();

        String timezone = ZoneId.systemDefault().toString();
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setContextTtlMilliseconds(900000);
        context.setTimezone(timezone);
        context.setAbleToRunConcurrently(false);

        // add 5 minutes and use this as the timeWindowStart for the context
        String cronExpression = CronUtils.buildCronFromOriginalAllDays(System.currentTimeMillis() + 300000, timezone);
        context.setTimeWindowStart(cronExpression);

        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of(zdtNowInLondon.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInLondon.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        long testExecutionFinishTime = System.currentTimeMillis();

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        // Validate the start time is NOW, and the Proposed end time is 5 minutes in the future
        assert (contextMachine.getContext().getStartTime() > testExecutionStartTime && contextMachine.getContext().getStartTime() < testExecutionFinishTime);
        assert (contextMachine.getContext().getProjectedEndTime() >= (testExecutionStartTime + 239000) && contextMachine.getContext().getProjectedEndTime() <= testExecutionFinishTime + 240000);
    }

    /**
     * This test is to modify the Time Window Start to 5 minutes in the future of current time, having a TTL of 15 minutes
     * As this context will be started at current time, the proposed end time will have to be modified because the TTL
     * by default will be used from the time window start, which will put it 23 hours and 50 minutes in the past.
     * <p>
     * Expectation is that the proposed end time is now 15 minutes in the future.
     * This is because concurrency is on
     *
     * @throws Exception
     */
    @Test
    public void register_with_params_with_agents_outside_datetime_window_end_time_in_past_but_able_to_end_before_next_instance_start_so_should_register_with_concurrency() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside-ttl.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        long testExecutionStartTime = System.currentTimeMillis();

        String timezone = "Europe/London";
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setContextTtlMilliseconds(900000);
        context.setTimezone(timezone);
        context.setAbleToRunConcurrently(true);

        // add 5 minutes and use this as the timeWindowStart for the context
        String cronExpression = CronUtils.buildCronFromOriginalAllDays(System.currentTimeMillis() + 300000, timezone);
        context.setTimeWindowStart(cronExpression);

        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of(zdtNowInLondon.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInLondon.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

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
        String contextInstanceId = contextInstanceRegistrationService
            .register(contextName, null, this.contextInstanceSchedulerService);

        long testExecutionFinishTime = System.currentTimeMillis();

        // verify
        verify(scheduledContextService).findById(contextName);
        verify(moduleMetadataService).find(any(), any(), eq(-1), eq(-1));
        verify(contextParametersInstanceService).populateContextParameters();
        verify(contextParametersInstanceService).populateContextParametersOnContextInstance(any(ContextInstance.class), any(Map.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "1"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "2"), any(ContextInstance.class));
        verify(contextInstancePublicationService).publish(eq(AGENT_URL + "3"), any(ContextInstance.class));
        verify(contextInstanceStateChangeEventBroadcaster).broadcast(any());
        ArgumentCaptor<ScheduledContextInstanceRecord> contextInstanceCaptor = ArgumentCaptor.forClass(ScheduledContextInstanceRecord.class);
        verify(scheduledContextInstanceService, times(3)).save(contextInstanceCaptor.capture());
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
            this.jobProvisionService,
            this.schedulerJobService,
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

        // Validate the start time is NOW, and the Proposed end time is 15 minutes in the future
        assert (contextMachine.getContext().getStartTime() > testExecutionStartTime && contextMachine.getContext().getStartTime() < testExecutionFinishTime);
        assert (contextMachine.getContext().getProjectedEndTime() >= (testExecutionStartTime + 900000) && contextMachine.getContext().getProjectedEndTime() <= testExecutionFinishTime + 900000);
    }

    /**
     * This test is to modify the Time Window Start to 30 seconds in the future of current time, having a TTL of 15 minutes
     * As this context will be started at current time, the proposed end time will have to be modified. However, we put in a rule
     * that says that we cannot start an instance if the context is marked as not concurrent and if the next start time is within a minute
     * <p>
     * Expectation is that this will throw a runtime exception.
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void register_with_params_with_agents_outside_datetime_window_end_time_to_close_to_next_start_time() throws Exception {
        // set up
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String jsonContext = new String(new ClassPathResource("context-with-datetime-blackout-window-outside-ttl.json").getInputStream().readAllBytes());
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        String timezone = ZoneId.systemDefault().toString();
        ZonedDateTime zdtNowInLondon = now(ZoneId.of(timezone));
        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setContextTtlMilliseconds(900000);
        context.setTimezone(timezone);
        context.setAbleToRunConcurrently(false);

        // add 30 seconds and use this as the timeWindowStart for the context
        String cronExpression = CronUtils.buildCronFromOriginalAllDays(System.currentTimeMillis() + 30000, timezone);
        context.setTimeWindowStart(cronExpression);

        // Pretend we are in London
        when(timeService.getDateNow()).thenReturn(Date.from(zdtNowInLondon.toInstant()));

        // The time in the windows is saved in UTC i.e. seconds from epoch
        context.setBlackoutWindowDateTimeRanges(
            Map.of(zdtNowInLondon.minus(Duration.ofMinutes(200)).toInstant().toEpochMilli(),
                zdtNowInLondon.minus(Duration.ofMinutes(100)).toInstant().toEpochMilli()));

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
        contextInstanceRegistrationService.register(contextName, null, this.contextInstanceSchedulerService);
    }
}