package org.ikasan.orchestration.service.context.recovery;

import static org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults.AGENT_NAME;
import static org.ikasan.orchestration.service.utils.ScheduledContextRecordTestSearchResults.CONTEXT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.orchestration.service.utils.ContextInstanceTestSearchResults;
import org.ikasan.orchestration.service.utils.InternalEventDrivenJobTestSearchResults;
import org.ikasan.orchestration.service.utils.ScheduledContextRecordTestSearchResults;
import org.ikasan.orchestration.service.utils.TestUtils;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
    private SchedulerService schedulerService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private ContextParametersUpdateService<ContextParameterInstance> contextParametersUpdateService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Mock
    private ExecutorService executor;

    private ContextInstanceRecoveryServiceImpl contextInstanceRecoveryServiceImpl;

    @Before
    public void setUp() {
        TestUtils.resetContextMachineCache();
        JobLockCacheImpl.instance().reset();

        contextInstanceRecoveryServiceImpl = new ContextInstanceRecoveryServiceImpl(
            "bigQueue/dir",
            scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService
        );

        ReflectionTestUtils.setField(contextInstanceRecoveryServiceImpl, "executor", executor);
    }

    @After
    public void tearDown() {
        JobLockCacheImpl.instance().reset();
        TestUtils.resetContextMachineCache();
    }

    @Test
    public void should_do_nothing_if_outside_of_operating_window_no_contexts() {
        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(0, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void should_call_executor_to_create_context_inside_operating_window_no_instance() {
        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);
        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(jobLockInstance);
        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();
        verify(jobLockCacheService).get();
        verify(executor).execute(any(ContextInstanceRecoveryBackFillerRunner.class));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void should_do_nothing_context_machine_outside_of_operating_window() {
        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, false);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void should_create_context_machine_with_agents_all_inside_operating_window() throws Exception {
        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(3, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(3, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        InternalEventDrivenJobTestSearchResults internalJobResults = new InternalEventDrivenJobTestSearchResults(3);
        when(internalEventDrivenJobService.findByContext(CONTEXT_NAME + "1", -1, -1)).thenReturn(internalJobResults);
        when(internalEventDrivenJobService.findByContext(CONTEXT_NAME + "2", -1, -1)).thenReturn(internalJobResults);
        when(internalEventDrivenJobService.findByContext(CONTEXT_NAME + "3", -1, -1)).thenReturn(internalJobResults);
        when(moduleMetadataService.findById(AGENT_NAME + "1")).thenReturn(TestUtils.createModuleMetaData("1"));
        when(moduleMetadataService.findById(AGENT_NAME + "2")).thenReturn(TestUtils.createModuleMetaData("2"));
        when(moduleMetadataService.findById(AGENT_NAME + "3")).thenReturn(TestUtils.createModuleMetaData("3"));

        JobLockCacheRecordImpl jobLockCacheRecord = new JobLockCacheRecordImpl();
        JobLockCacheImpl jobLockInstance = JobLockCacheImpl.instance();
        jobLockInstance.setJobLockCacheService(jobLockCacheService);
        jobLockCacheRecord.setJobLockCache(jobLockInstance);
        when(jobLockCacheService.get()).thenReturn(jobLockCacheRecord);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();
        verify(internalEventDrivenJobService).findByContext("ContextName1", -1, -1);
        verify(internalEventDrivenJobService).findByContext("ContextName2", -1, -1);
        verify(internalEventDrivenJobService).findByContext("ContextName3", -1, -1);
        verify(moduleMetadataService, times(3)).findById(AGENT_NAME + "1");
        verify(moduleMetadataService, times(3)).findById(AGENT_NAME + "2");
        verify(moduleMetadataService, times(3)).findById(AGENT_NAME + "3");

        verify(jobLockCacheService, times(3)).get();
        verify(schedulerJobInstanceService, times(3)).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        assertNotNull(ContextMachineCache.instance().getByContextName("ContextName1"));
        assertNotNull(ContextMachineCache.instance().getByContextName("ContextName2"));
        assertNotNull(ContextMachineCache.instance().getByContextName("ContextName3"));

        assertEquals(3, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void should_create_context_machine_no_agents() throws Exception {
        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ContextInstanceTestSearchResults results = new ContextInstanceTestSearchResults(1, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(results);

        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(1, false);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        when(internalEventDrivenJobService.findByContext("ContextName1", -1, -1)).thenReturn(new InternalEventDrivenJobTestSearchResults(0));
        when(jobLockCacheService.get()).thenReturn(null);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();
        verify(internalEventDrivenJobService).findByContext("ContextName1", -1, -1);
        verify(jobLockCacheService).get();
        verify(schedulerJobInstanceService).initialiseSchedulerJobInstancesForContext(any(ContextInstance.class));

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName("ContextName1");
        assertNotNull(contextMachine);
        assertEquals(1, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void does_nothing_if_no_context_instance_records_or_context_records() {
        ContextInstanceTestSearchResults instanceResults = new ContextInstanceTestSearchResults(0, true);
        when(scheduledContextInstanceService.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING))).thenReturn(instanceResults);
        ScheduledContextRecordTestSearchResults contextResults = new ScheduledContextRecordTestSearchResults(0, true);
        when(scheduledContextService.findAll()).thenReturn(contextResults);

        contextInstanceRecoveryServiceImpl.recoverInstances();

        verify(scheduledContextInstanceService).getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));
        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            executor);

        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }
}