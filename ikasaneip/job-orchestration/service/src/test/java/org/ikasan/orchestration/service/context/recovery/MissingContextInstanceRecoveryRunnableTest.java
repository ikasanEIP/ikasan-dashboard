package org.ikasan.orchestration.service.context.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.core.machine.JobLogicMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.JobLockCacheInitialisationServiceImpl;
import org.ikasan.orchestration.service.utils.*;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
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
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.ikasan.orchestration.service.utils.TestUtils.AGENT_URL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
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
    private ContextInstanceSchedulerService contextInstanceSchedulerService;
    @Mock
    private TimeService timeService;

    private MissingContextInstanceRecoveryRunnable backFiller;

    private String contextName;

    private ScheduledContextRecord record;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Before
    public void setUp() {
        ContextMachineCache.instance().resetAllCache();

        // Stub this Implementation due to two difference SchedulerJobInstance can be returned
        schedulerJobInstanceService = new StubSchedulerJobInstanceServiceTestImpl();
        
        contextName = RandomStringUtils.randomAlphabetic(22);
        record = new ScheduledContextRecordImpl();
        record.setContextName(contextName);
        ContextTemplateImpl context = new ContextTemplateImpl();
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
            timeService
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
            schedulerJobStateChangeEventBroadcaster
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
            schedulerJobStateChangeEventBroadcaster
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