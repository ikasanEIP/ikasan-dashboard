package org.ikasan.orchestration.service.context.upload;

import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextUploadInitialisationServiceImplTest {
    @Mock
    private Scheduler scheduler;
    @Mock
    private ScheduledJobFactory scheduledJobFactory;
    @Mock
    private ScheduledContextService scheduledContextService;
    @Mock
    private ModuleMetaDataService moduleMetadataService;
    @Mock
    private SchedulerJobService schedulerJobService;
    @Mock
    private JobProvisionModuleService jobProvisionModuleRestService;
    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextUploadInitialisationServiceImpl service;

    @Before
    public void setUp() {
        service = new ContextUploadInitialisationServiceImpl(
            scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, true
        );
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_context() {
        service.uploadContextAndJobs(null, Collections.emptyList());
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_jobs() {
        service.uploadContextAndJobs(new ContextTemplateImpl(), null);
    }

    @Test
    public void should_upload_provision_jobs_and_not_create_context_outside_of_window() throws Exception {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setTimeWindowEnd("0 0 0 ? * * *");
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        JobDetailImpl detail = new JobDetailImpl();
        detail.setName("ContextName");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName), eq("context"))).thenReturn(detail);

        JobDetailImpl endDetail = new JobDetailImpl();
        endDetail.setName("ContextName-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName + "-EndJob"), eq("context"))).thenReturn(endDetail);

        service.uploadContextAndJobs(contextTemplate, contextJobs);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName-EndJob"), eq("context"));
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler).scheduleJob(eq(detail), any(Trigger.class));
        verify(scheduler).scheduleJob(eq(endDetail), any(Trigger.class));

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context() throws Exception {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setTimeWindowEnd("59 59 23 ? * * *");
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        JobDetailImpl detail = new JobDetailImpl();
        detail.setName("ContextName");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName), eq("context"))).thenReturn(detail);

        JobDetailImpl endDetail = new JobDetailImpl();
        endDetail.setName("ContextName-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName + "-EndJob"), eq("context"))).thenReturn(endDetail);

        service.uploadContextAndJobs(contextTemplate, contextJobs);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName-EndJob"), eq("context"));
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler).scheduleJob(eq(detail), any(Trigger.class));
        verify(scheduler).scheduleJob(eq(endDetail), any(Trigger.class));

        verify(contextInstanceRegistrationService).register(contextName);

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService);
    }

    @Test
    public void should_upload_not_provision_jobs_and_create_context() throws Exception {
        ReflectionTestUtils.setField(service, "uploadProvisionJobs", false);
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setTimeWindowEnd("59 59 23 ? * * *");
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);

        JobDetailImpl detail = new JobDetailImpl();
        detail.setName("ContextName");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName), eq("context"))).thenReturn(detail);

        JobDetailImpl endDetail = new JobDetailImpl();
        endDetail.setName("ContextName-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(contextName + "-EndJob"), eq("context"))).thenReturn(endDetail);

        service.uploadContextAndJobs(contextTemplate, contextJobs);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName-EndJob"), eq("context"));
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler).scheduleJob(eq(detail), any(Trigger.class));
        verify(scheduler).scheduleJob(eq(endDetail), any(Trigger.class));

        verify(contextInstanceRegistrationService).register(contextName);

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService);
    }

    @Test
    public void register_jobs_does_nothing() {
        service.registerJobs();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService);
    }

}