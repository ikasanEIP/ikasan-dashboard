package org.ikasan.job.orchestration.provision.context;

import org.ikasan.job.orchestration.model.context.ContextBundleImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneId;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextProvisionServiceImplTest {
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
    @Mock
    private ContextProfileService contextProfileService;
    @Mock
    private EmailNotificationDetailsService emailNotificationDetailsService;

    private ContextProvisionServiceImpl service;

    @Before
    public void setUp() {
        service = new ContextProvisionServiceImpl(
            scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, true
        );
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_context() {
        ContextBundle contextBundle = new ContextBundleImpl(null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_jobs() {
        ContextBundle contextBundle = new ContextBundleImpl(new ContextTemplateImpl(), null, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

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

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(eq(detail), triggerCaptor.capture());
        Assert.assertEquals(ZoneId.systemDefault().getId(), ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());
        verify(scheduler, times(1)).scheduleJob(eq(endDetail), triggerCaptor.capture());
        Assert.assertEquals(ZoneId.systemDefault().getId(), ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService);
    }

    @Test
    public void should_upload_provision_jobs_and_not_create_context_outside_of_window_with_timezone() throws Exception {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setTimeWindowEnd("0 0 0 ? * * *");
        contextTemplate.setTimezone("Asia/Singapore");
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertEquals("Asia/Singapore", actualContextRecord.getContext().getTimezone());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000
            && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName-EndJob"), eq("context"));
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());
        verify(scheduler, times(2)).checkExists(detail.getKey());
        verify(scheduler, times(2)).checkExists(endDetail.getKey());

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(eq(detail), triggerCaptor.capture());
        Assert.assertEquals("Asia/Singapore", ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());
        verify(scheduler, times(1)).scheduleJob(eq(endDetail), triggerCaptor.capture());
        Assert.assertEquals("Asia/Singapore", ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

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

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(eq(detail), triggerCaptor.capture());
        Assert.assertEquals(ZoneId.systemDefault().getId(), ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());
        verify(scheduler, times(1)).scheduleJob(eq(endDetail), triggerCaptor.capture());
        Assert.assertEquals(ZoneId.systemDefault().getId(), ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());

        verify(contextInstanceRegistrationService).register(contextName);

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_timezone() throws Exception {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setTimeWindowEnd("59 59 23 ? * * *");
        contextTemplate.setTimezone("Asia/Singapore");
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

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

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(eq(detail), triggerCaptor.capture());
        Assert.assertEquals("Asia/Singapore", ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());
        verify(scheduler, times(1)).scheduleJob(eq(endDetail), triggerCaptor.capture());
        Assert.assertEquals("Asia/Singapore", ((CronTriggerImpl)triggerCaptor.getValue()).getTimeZone().toZoneId().getId());

        verify(contextInstanceRegistrationService).register(contextName);

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_job_lock() throws Exception {
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
        InternalEventDrivenJob internalEventDrivenJob1 = new InternalEventDrivenJobImpl();
        internalEventDrivenJob1.setJobName("jobName1");
        internalEventDrivenJob1.setAgentName("agentName1");
        internalEventDrivenJob1.setIdentifier("agentName1-jobName1");
        InternalEventDrivenJob internalEventDrivenJob2 = new InternalEventDrivenJobImpl();
        internalEventDrivenJob2.setJobName("jobName2");
        internalEventDrivenJob2.setAgentName("agentName1");
        internalEventDrivenJob2.setIdentifier("agentName1-jobName2");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob1);
        contextJobs.add(internalEventDrivenJob2);

        Map<String, List<SchedulerJob>> lockMap = new HashMap<>();
        lockMap.put("ContextName", List.of(internalEventDrivenJob1));

        JobLock jobLock = new JobLockImpl();
        jobLock.setName("testLock");
        jobLock.setJobs(lockMap);

        contextTemplate.setJobLocks(List.of(jobLock));

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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());
        contextJobs.forEach(job -> {
            if(job instanceof InternalEventDrivenJob) {
                if(job.getJobName().equals("jobName1")) {
                    Assert.assertEquals(true, ((InternalEventDrivenJob) job).isParticipatesInLock());
                }
                else if(job.getJobName().equals("jobName2")) {
                    Assert.assertEquals(false, ((InternalEventDrivenJob) job).isParticipatesInLock());
                }
            }
        });

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
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles() throws Exception {
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

        ContextProfileRecord contextProfileRecord1 = new SolrContextProfileRecordImpl();
        ContextProfileRecord contextProfileRecord2 = new SolrContextProfileRecordImpl();

        List<ContextProfileRecord> contextProfileRecords = List.of(contextProfileRecord1, contextProfileRecord2);

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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");
        verify(contextProfileService).save(contextProfileRecords);

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
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

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
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles_and_notification() throws Exception {
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

        ContextProfileRecord contextProfileRecord1 = new SolrContextProfileRecordImpl();
        ContextProfileRecord contextProfileRecord2 = new SolrContextProfileRecordImpl();

        List<ContextProfileRecord> contextProfileRecords = List.of(contextProfileRecord1, contextProfileRecord2);

        // Email Notification
        EmailNotificationDetails emailNotificationDetails1 = new SolrEmailNotificationDetails();
        EmailNotificationDetails emailNotificationDetails2 = new SolrEmailNotificationDetails();

        List<EmailNotificationDetails> emailNotificationDetails = List.of(emailNotificationDetails1, emailNotificationDetails2);

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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs);
        verify(contextProfileService).save(contextProfileRecords);
        verify(emailNotificationDetailsService).saveEmailNotificationDetails(emailNotificationDetails);

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
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService);
    }

    @Test
    public void register_jobs_does_nothing() {
        service.registerJobs();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, emailNotificationDetailsService);
    }

}