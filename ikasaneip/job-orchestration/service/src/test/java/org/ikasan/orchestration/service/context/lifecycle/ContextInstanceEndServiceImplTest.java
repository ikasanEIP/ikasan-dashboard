package org.ikasan.orchestration.service.context.lifecycle;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.orchestration.service.utils.StubSchedulerJobInstanceServiceTestImpl;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceEndServiceImplTest {

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
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @Mock
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    @Mock
    private TimeService timeService;
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

    @Mock
    private ContextInstanceStateChangeEvent contextInstanceStateChangeEvent;
    @Mock
    private ContextInstanceImpl contextInstance;
    @Mock
    private ScheduledContextInstanceRecordImpl preparedContextInstanceRecord;

    private ContextInstanceImpl preparedContextInstance;

    private ContextTemplateImpl contextTemplate;

    @Mock
    private SearchResults searchResults;
    @Mock
    private ModuleMetadataSearchResults agentSearchResults;
    @Mock
    private ScheduledContextRecord scheduledContextRecord;

    private ContextInstanceEndServiceImpl contextInstanceEndService;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Before
    public void setUp() {
        // Stub this Implementation due to two difference SchedulerJobInstance can be returned
        this.schedulerJobInstanceService = new StubSchedulerJobInstanceServiceTestImpl();

        this.contextInstanceEndService = new ContextInstanceEndServiceImpl (
            "bigQueue/dir",
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.jobLockCacheService,
            this.scheduledContextService,
            this.schedulerJobInstanceService,
            this.jobLockCacheInitialisationService,
            this.timeService,
            this.systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            this.contextInstanceSchedulerService,
            true);

        this.contextTemplate = new ContextTemplateImpl();
        this.contextTemplate.setName("context-name");

        ContextMachineCache.instance().resetAllCache();
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @After
    public void tearDown() {
        ContextMachineCache.instance().resetAllCache();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_contextInstanceSchedulerService() {
        new ContextInstanceEndServiceImpl (
            "bigQueue/dir",
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.jobLockCacheService,
            this.scheduledContextService,
            this.schedulerJobInstanceService,
            this.jobLockCacheInitialisationService,
            this.timeService,
            this.systemEventService,
            this.jobUtilsService,
            this.jobProvisionService,
            this.schedulerJobService,
            null,
            true);
    }

    @Test
    public void test_does_not_end_job_plan_instance_due_to_not_being_configured_to_automatically_end() throws Exception {

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(false);

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();

        verifyNoMoreInteractions(
            this.contextInstance,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }

    @Test
    public void test_does_not_end_job_plan_instance_due_to_due_to_instance_being_marked_to_end_manually() throws Exception {

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(true);
        when(this.contextInstance.isRunContextUntilManuallyEnded()).thenReturn(true);

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();
        verify(this.contextInstance, times(1)).isRunContextUntilManuallyEnded();

        verifyNoMoreInteractions(
            this.contextInstance,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }

    @Test
    public void test_does_not_end_job_plan_instance_due_to_due_to_instance_not_complete_yet() throws Exception {

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(true);
        when(this.contextInstance.isRunContextUntilManuallyEnded()).thenReturn(false);
        when(this.contextInstanceStateChangeEvent.getNewStatus()).thenReturn(InstanceStatus.RUNNING);

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();
        verify(this.contextInstance, times(1)).isRunContextUntilManuallyEnded();
        verify(this.contextInstanceStateChangeEvent, times(1)).getNewStatus();

        verifyNoMoreInteractions(
            this.contextInstance,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }

    @Test
    public void test_end_job_plan_instance_success_prepared_registered_as_scheduled_to_register_in_the_past() throws Exception {
        this.contextTemplate = new ContextTemplateImpl();
        this.contextTemplate.setName("context-name");
        this.contextTemplate.setAbleToRunConcurrently(false);
        this.contextTemplate.setTimeWindowStart("0 0 1 ? * * *");

        this.preparedContextInstance = new ContextInstanceImpl();
        this.preparedContextInstance.setName("context-name");
        this.preparedContextInstance.setId("id");
        this.preparedContextInstance.setStatus(InstanceStatus.PREPARED);
        this.preparedContextInstance.setStartTime(System.currentTimeMillis() - 1000000L);
        this.preparedContextInstance.setTimeWindowStart("0 0 1 ? * * *");

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(true);
        when(this.contextInstance.isRunContextUntilManuallyEnded()).thenReturn(false);
        when(this.contextInstanceStateChangeEvent.getNewStatus()).thenReturn(InstanceStatus.COMPLETE);
        when(this.contextInstance.getName()).thenReturn("context-name");
        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(this.searchResults);
        when(this.searchResults.getResultList()).thenReturn(List.of(preparedContextInstanceRecord));
        when(this.preparedContextInstanceRecord.getContextInstance()).thenReturn(this.preparedContextInstance);
        when(this.scheduledContextService.findById(anyString())).thenReturn(this.scheduledContextRecord);
        when(this.scheduledContextRecord.getContext()).thenReturn(this.contextTemplate);
        when(this.scheduledContextRecord.getContextName()).thenReturn("context-name");
        when(this.moduleMetadataService.find(any(), any(), anyInt(), anyInt())).thenReturn(this.agentSearchResults);
        when(this.agentSearchResults.getResultList()).thenReturn(List.of());

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();
        verify(this.contextInstance, times(1)).isRunContextUntilManuallyEnded();
        verify(this.contextInstance, times(6)).getName();
        verify(this.contextInstanceStateChangeEvent, times(3)).getContextInstanceId();
        verify(this.contextInstanceStateChangeEvent, times(1)).getNewStatus();
        verify(this.scheduledContextInstanceService,times(3)).getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull());
        verify(this.scheduledContextService, times(2)).findById(anyString());
        verify(this.scheduledContextRecord, times(3)).getContext();
        verify(this.scheduledContextRecord, times(2)).getContextName();
        verify(this.preparedContextInstanceRecord, times(3)).getContextInstance();
        verify(this.moduleMetadataService, times(2)).find(any(), any(), anyInt(), anyInt());
        verify(this.agentSearchResults, times(2)).getResultList();
        verify(this.scheduledContextInstanceService, times(3)).save(any());
        verify(this.contextParametersInstanceService, times(1)).populateContextParameters();
        verify(this.contextParametersInstanceService, times(1)).populateContextParametersOnContextInstance(any(), any());

        verifyNoMoreInteractions(
            this.contextInstance,
            this.preparedContextInstanceRecord,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }

    @Test
    public void test_end_job_plan_instance_success_prepared_not_registered_as_scheduled_to_register_in_the_future() throws Exception {
        this.contextTemplate = new ContextTemplateImpl();
        this.contextTemplate.setName("context-name");
        this.contextTemplate.setAbleToRunConcurrently(false);
        this.contextTemplate.setTimeWindowStart("0 0 1 ? * * *");

        this.preparedContextInstance = new ContextInstanceImpl();
        this.preparedContextInstance.setName("context-name");
        this.preparedContextInstance.setId("id");
        this.preparedContextInstance.setStatus(InstanceStatus.PREPARED);
        this.preparedContextInstance.setStartTime(System.currentTimeMillis() + 1000000L);
        this.preparedContextInstance.setTimeWindowStart("0 0 1 ? * * *");

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(true);
        when(this.contextInstance.isRunContextUntilManuallyEnded()).thenReturn(false);
        when(this.contextInstanceStateChangeEvent.getNewStatus()).thenReturn(InstanceStatus.COMPLETE);
        when(this.contextInstance.getName()).thenReturn("context-name");
        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(this.searchResults);
        when(this.searchResults.getResultList()).thenReturn(List.of(preparedContextInstanceRecord));
        when(this.preparedContextInstanceRecord.getContextInstance()).thenReturn(this.preparedContextInstance);

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();
        verify(this.contextInstance, times(1)).isRunContextUntilManuallyEnded();
        verify(this.contextInstance, times(3)).getName();
        verify(this.contextInstanceStateChangeEvent, times(3)).getContextInstanceId();
        verify(this.contextInstanceStateChangeEvent, times(1)).getNewStatus();
        verify(this.scheduledContextInstanceService,times(1)).getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull());
        verify(this.preparedContextInstanceRecord, times(1)).getContextInstance();

        verifyNoMoreInteractions(
            this.contextInstance,
            this.preparedContextInstanceRecord,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }

    @Test
    public void test_end_job_plan_instance_exception_prepared_with_bad_cron_expression() throws Exception {
        this.contextTemplate = new ContextTemplateImpl();
        this.contextTemplate.setName("context-name");
        this.contextTemplate.setAbleToRunConcurrently(false);
        this.contextTemplate.setTimeWindowStart("0 0 1 ? * * *");

        this.preparedContextInstance = new ContextInstanceImpl();
        this.preparedContextInstance.setName("context-name");
        this.preparedContextInstance.setId("id");
        this.preparedContextInstance.setStatus(InstanceStatus.PREPARED);
        this.preparedContextInstance.setStartTime(System.currentTimeMillis() - 1000000L);
        this.preparedContextInstance.setTimeWindowStart("bad cron expression");

        when(this.contextInstanceStateChangeEvent.getContextInstance()).thenReturn(this.contextInstance);
        when(this.contextInstanceStateChangeEvent.getContextInstanceId()).thenReturn("id");
        when(this.contextInstance.isEndJobPlanUponCompletion()).thenReturn(true);
        when(this.contextInstance.isRunContextUntilManuallyEnded()).thenReturn(false);
        when(this.contextInstanceStateChangeEvent.getNewStatus()).thenReturn(InstanceStatus.COMPLETE);
        when(this.contextInstance.getName()).thenReturn("context-name");
        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(this.searchResults);
        when(this.searchResults.getResultList()).thenReturn(List.of(preparedContextInstanceRecord));
        when(this.preparedContextInstanceRecord.getContextInstance()).thenReturn(this.preparedContextInstance);
        when(this.scheduledContextService.findById(anyString())).thenReturn(this.scheduledContextRecord);
        when(this.scheduledContextRecord.getContext()).thenReturn(this.contextTemplate);
        when(this.scheduledContextRecord.getContextName()).thenReturn("context-name");
        when(this.moduleMetadataService.find(any(), any(), anyInt(), anyInt())).thenReturn(this.agentSearchResults);
        when(this.agentSearchResults.getResultList()).thenReturn(List.of());

        this.contextInstanceEndService.receiveBroadcast(this.contextInstanceStateChangeEvent);

        verify(this.contextInstance, times(1)).isEndJobPlanUponCompletion();
        verify(this.contextInstance, times(1)).isRunContextUntilManuallyEnded();
        verify(this.contextInstance, times(6)).getName();
        verify(this.contextInstanceStateChangeEvent, times(4)).getContextInstanceId();
        verify(this.contextInstanceStateChangeEvent, times(1)).getNewStatus();
        verify(this.scheduledContextInstanceService,times(3)).getScheduledContextInstancesByFilter
            (any(), anyInt(), anyInt(), isNull(), isNull());
        verify(this.scheduledContextService, times(2)).findById(anyString());
        verify(this.scheduledContextRecord, times(3)).getContext();
        verify(this.scheduledContextRecord, times(2)).getContextName();
        verify(this.preparedContextInstanceRecord, times(3)).getContextInstance();
        verify(this.moduleMetadataService, times(2)).find(any(), any(), anyInt(), anyInt());
        verify(this.agentSearchResults, times(2)).getResultList();
        verify(this.scheduledContextInstanceService, times(2)).save(any());
        verify(this.contextParametersInstanceService, times(1)).populateContextParameters();
        verify(this.contextParametersInstanceService, times(1)).populateContextParametersOnContextInstance(any(), any());

        verifyNoMoreInteractions(
            this.contextInstance,
            this.preparedContextInstanceRecord,
            this.scheduledContextInstanceService,
            this.jobInitiationService,
            this.moduleMetadataService,
            this.internalEventDrivenJobService,
            this.contextParametersInstanceService,
            this.contextInstancePublicationService,
            this.scheduledContextService,
            this.jobLockCacheService,
            this.jobProvisionService,
            this.schedulerJobService
        );
    }
}