package org.ikasan.job.orchestration.provision.context;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.model.context.ContextBundleImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextProvisionServiceImplTest {
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
    @Mock
    private EmailNotificationContextService emailNotificationContextService;
    @Mock
    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    private ContextProvisionServiceImpl service;

    @Before
    public void setUp() {
        service = new ContextProvisionServiceImpl(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService,
            emailNotificationContextService, true, contextInstanceSchedulerService, 3);
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_context() {
        ContextBundle contextBundle = new ContextBundleImpl(null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_jobs() {
        ContextBundle contextBundle = new ContextBundleImpl(new ContextTemplateImpl(), null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_exception_if_job_plan_cron_and_duration_tolerance_is_unacceptable() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("* * * ? * * *");
        contextTemplate.setContextTtlMilliseconds(1000000000L);
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);
    }

    @Test
    public void should_upload_provision_jobs_and_not_create_context_outside_of_window() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(1L);
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
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);

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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_not_create_context_outside_of_window_with_timezone() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(1L);
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
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        // Does not get provision
        contextJobs.add(globalEventJob);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
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

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_timezone() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
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

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_job_lock() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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

        SchedulerJobLockParticipant lockParticipant1 = new SchedulerJobLockParticipantImpl();
        lockParticipant1.setJobName("jobName1");
        lockParticipant1.setAgentName("agentName1");
        lockParticipant1.setIdentifier("agentName1-jobName1");

        Map<String, List<SchedulerJobLockParticipant>> lockMap = new HashMap<>();
        lockMap.put("ContextName", List.of(lockParticipant1));

        JobLock jobLock = new JobLockImpl();
        jobLock.setName("testLock");
        jobLock.setJobs(lockMap);

        contextTemplate.setJobLocks(List.of(jobLock));

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
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
                    assertTrue(((InternalEventDrivenJob) job).isParticipatesInLock());
                }
                else if(job.getJobName().equals("jobName2")) {
                    assertFalse(((InternalEventDrivenJob) job).isParticipatesInLock());
                }
            }
        });

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
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

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_not_provision_jobs_and_create_context() {
        ReflectionTestUtils.setField(service, "uploadProvisionJobs", false);
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles_and_notification_details() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, null);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");
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

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles_and_notification_details_and_context() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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

        // Email Notification Details
        EmailNotificationDetails emailNotificationDetails1 = new SolrEmailNotificationDetails();
        EmailNotificationDetails emailNotificationDetails2 = new SolrEmailNotificationDetails();

        List<EmailNotificationDetails> emailNotificationDetails = List.of(emailNotificationDetails1, emailNotificationDetails2);

        EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, emailNotificationContext);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");
        verify(contextProfileService).save(contextProfileRecords);
        verify(emailNotificationDetailsService).saveEmailNotificationDetails(emailNotificationDetails);
        verify(emailNotificationContextService).saveEmailNotificationContext(emailNotificationContext);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles_and_notification_context()  {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
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
        EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, Collections.EMPTY_LIST, emailNotificationContext);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");
        verify(contextProfileService).save(contextProfileRecords);
        verify(emailNotificationContextService).saveEmailNotificationContext(emailNotificationContext);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verify(contextInstanceRegistrationService).register(contextName, null);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService, emailNotificationContextService);
    }
}