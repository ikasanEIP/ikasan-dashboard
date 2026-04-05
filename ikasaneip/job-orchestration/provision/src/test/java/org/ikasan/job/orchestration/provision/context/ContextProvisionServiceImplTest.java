package org.ikasan.job.orchestration.provision.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.model.context.ContextBundleImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.provision.job.JobProvisionLockException;
import org.ikasan.job.orchestration.rest.client.dto.ErrorDto;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextProvisionServiceImplTest extends AbstractTest {
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
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private SecurityService securityService;

    private ContextProvisionServiceImpl service;

    @Before
    public void setUp() {
        service = new ContextProvisionServiceImpl(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService, emailNotificationDetailsService,
            emailNotificationContextService, true, contextInstanceSchedulerService, this.scheduledContextInstanceService
            , 3, securityService);
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_context() {
        ContextBundle contextBundle = new ContextBundleImpl(null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);
    }

    @Test(expected = RuntimeException.class)
    public void should_validate_not_null_jobs() {
        ContextBundle contextBundle = new ContextBundleImpl(new ContextTemplateImpl(), null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
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

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);

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
        verify(securityService).setJobPlanRoles(contextTemplate.getName(), roleList);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
        verify(securityService).setJobPlanRoles(contextTemplate.getName(), roleList);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName("agentName1");
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
        verify(securityService).setJobPlanRoles(contextTemplate.getName(), roleList);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
    }

    @Test(expected = JobProvisionLockException.class)
    public void should_upload_provision_jobs_and_create_context_exception_rest_agent_lock_exception() throws JsonProcessingException {
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
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName("agentName1");
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        contextTemplate.setScheduledJobs(contextJobs);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorCode("LOCK_ACQUISITION_ERROR");
        errorDto.setErrorMessage("Lock error!");

        String error = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(errorDto);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatusCode.valueOf(401)
            , "Lock Exception!", error.getBytes(), null);
        RuntimeException runtimeException = new RuntimeException(exception);

        doThrow(runtimeException).when(this.jobProvisionModuleRestService).provisionJobs(anyString(), any());

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
        service.provisionContext(contextBundle);
    }
    @Test
    public void should_upload_provision_jobs_and_create_context_for_three_different_job_plan_bundles() throws InterruptedException {
        ContextBundle contextBundle1 = this.createContextBundle("context1", "agent1");
        ContextBundle contextBundle2 = this.createContextBundle("context2", "agent2");
        ContextBundle contextBundle3 = this.createContextBundle("context3", "agent3");

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agent1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        // Add a delay for the rest call so that the lock comes into effect
        doAnswer(answersWithDelay(1000, invocation -> {
            return null; // Return null for void methods
        })).when(this.jobProvisionModuleRestService).provisionJobs(anyString(), any());

        AtomicReference<JobProvisionLockException> exception =
            new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() ->  {
            try {
                service.provisionContext(contextBundle1);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        // Add sleep to make sure provision job thread gets lock
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle2);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle3);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });

        ConcurrentHashMap<String, ReentrantLock> locks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
        Assert.assertEquals(3, locks.size());

        // Confirm all threads locked
        Assert.assertTrue(locks.get("agent1").isLocked());
        Assert.assertTrue(locks.get("agent2").isLocked());
        Assert.assertTrue(locks.get("agent3").isLocked());


        // Make sure all threads do not throw and exception
        Awaitility.await().pollDelay(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> Assert.assertNull(exception.get()));

        // Confirm that all locks are released.
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                ConcurrentHashMap<String, ReentrantLock> agentLocks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
                Assert.assertFalse(agentLocks.get("agent1").isLocked());
            });
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                ConcurrentHashMap<String, ReentrantLock> agentLocks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
                Assert.assertFalse(agentLocks.get("agent2").isLocked());
            });
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                ConcurrentHashMap<String, ReentrantLock> agentLocks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
                Assert.assertFalse(agentLocks.get("agent3").isLocked());
            });
    }

    @Test(expected = JobProvisionLockException.class)
    public void should_upload_provision_jobs_and_create_context_for_three_different_job_plan_bundles_across_2_agents_with_exception() throws InterruptedException {
        ContextBundle contextBundle1 = this.createContextBundle("context1", "agent1");
        ContextBundle contextBundle2 = this.createContextBundle("context2", "agent2");
        ContextBundle contextBundle3 = this.createContextBundle("context3", "agent2");

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agent1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        // Add a delay for the rest call so that the lock comes into effect
        doAnswer(answersWithDelay(1000, invocation -> {
            return null; // Return null for void methods
        })).when(this.jobProvisionModuleRestService).provisionJobs(anyString(), any());

        AtomicReference<JobProvisionLockException> exception =
            new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() ->  {
            try {
                service.provisionContext(contextBundle1);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        // Add sleep to make sure provision job thread gets lock
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle2);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle3);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });

        ConcurrentHashMap<String, ReentrantLock> locks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
        Assert.assertEquals(2, locks.size());

        // Confirm all threads locked
        Assert.assertTrue(locks.get("agent1").isLocked());
        Assert.assertTrue(locks.get("agent2").isLocked());

        // Make sure all threads do not throw and exception
        Awaitility.await().pollDelay(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> Assert.assertNotNull(exception.get()));

        // Confirm that all locks are released.
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                ConcurrentHashMap<String, ReentrantLock> agentLocks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
                Assert.assertFalse(agentLocks.get("agent1").isLocked());
            });
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                ConcurrentHashMap<String, ReentrantLock> agentLocks = (ConcurrentHashMap<String, ReentrantLock>) ReflectionTestUtils.getField(this.service, "agentLocks");
                Assert.assertFalse(agentLocks.get("agent2").isLocked());
            });

        throw exception.get();
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_but_second_thread_throws_lock_exception_lock_clears_for_success_provision() throws InterruptedException {
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
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName("agentName1");
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        contextTemplate.setScheduledJobs(contextJobs);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        // Add a delay for the rest call so that the lock comes into effect
        doAnswer(answersWithDelay(1000, invocation -> {
            return null; // Return null for void methods
        })).when(this.jobProvisionModuleRestService).provisionJobs(anyString(), any());

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);

        AtomicReference<JobProvisionLockException> exception =
            new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() ->  {
            try {
                service.provisionContext(contextBundle);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        // Add sleep to make sure provision job thread gets lock
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });

        Awaitility.await().atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> Assert.assertNotNull(exception.get()));

        Assert.assertEquals("Cannot provision job plan[ContextName] as another process has locked the " +
                "agent[agentName1] for modification."
            , exception.get().getMessage());

        // Wait until the lock has been cleared
        Thread.sleep(2000);

        service.provisionContext(contextBundle);
    }

    @Test(expected = JobProvisionLockException.class)
    public void should_upload_provision_jobs_and_create_context_but_second_thread_throws_lock_exception() throws InterruptedException {
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
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName("agentName1");
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        contextTemplate.setScheduledJobs(contextJobs);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        // Add a delay for the rest call so that the lock comes into effect
        doAnswer(answersWithDelay(1000, invocation -> {
            return null; // Return null for void methods
        })).when(this.jobProvisionModuleRestService).provisionJobs(anyString(), any());

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);

        AtomicReference<JobProvisionLockException> exception =
            new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() ->  {
            try {
                service.provisionContext(contextBundle);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });
        // Add sleep to make sure provision job thread gets lock
        Thread.sleep(200);
        executor.submit(() -> {
            try {
                service.provisionContext(contextBundle);
            }
            catch (Exception e) {
                e.printStackTrace();
                exception.set((JobProvisionLockException)e);
            }
        });

        Awaitility.await().atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> Assert.assertNotNull(exception.get()));

        Assert.assertEquals("Cannot provision job plan[ContextName] as another process has locked the " +
                "agent[agentName1] for modification."
            , exception.get().getMessage());

        throw exception.get();
    }

    @Test
    public void should_upload_not_provision_jobs_due_to_delay_job_synchronisation_and_create_context() {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        String contextName = "ContextName";
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
        contextTemplate.setName(contextName);
        contextTemplate.setRequiresAgentSynchronisation(true);
        contextTemplate.setDelayAgentSynchronisationUntilNextInstance(true);

        when(this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), any(), any()))
            .thenReturn(new SearchResultsImpl<>(List.of(), 0, 0));

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName("agentName1");
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName("agentName1");
        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName("agentName1");
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName("agentName1");
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");

        List<String> roleList = List.of("role1", "role2");
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(schedulerJobService).save(contextJobs, "system");

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(securityService).setJobPlanRoles(contextTemplate.getName(), roleList);

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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
        List<SchedulerJobLockParticipant> schedulerJobLockParticipants = new ArrayList<>();
        schedulerJobLockParticipants.add(lockParticipant1);
        lockMap.put("ContextName", schedulerJobLockParticipants);

        JobLock jobLock = new JobLockImpl();
        jobLock.setName("testLock");
        jobLock.setJobs(lockMap);

        List<JobLock> jobLocks = new ArrayList<>();
        jobLocks.add(jobLock);

        contextTemplate.setJobLocks(jobLocks);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_empty_job_lock() {
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

        // A job lock without any jobs...
        JobLock jobLock = new JobLockImpl();
        jobLock.setName("testLock");

        List<JobLock> jobLocks = new ArrayList<>();
        jobLocks.add(jobLock);

        contextTemplate.setJobLocks(jobLocks);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));
        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
                    assertFalse(((InternalEventDrivenJob) job).isParticipatesInLock());
                }
                else if(job.getJobName().equals("jobName2")) {
                    assertFalse(((InternalEventDrivenJob) job).isParticipatesInLock());
                }
            }
        });

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
        verify(schedulerJobService).save(contextJobs, "system");

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertEquals(contextName, actualContextRecord.getContextName());
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        contextTemplate.setScheduledJobs(contextJobs);

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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
        ArgumentCaptor<SchedulerJobWrapperImpl> jobWrapperArgumentCaptor = ArgumentCaptor.forClass(SchedulerJobWrapperImpl.class);
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), jobWrapperArgumentCaptor.capture());

        SchedulerJobWrapperImpl wrapper = jobWrapperArgumentCaptor.getValue();

        assertEquals(2, wrapper.getJobs().size());

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
    }

    @Test
    public void should_upload_provision_jobs_and_create_context_with_context_profiles_and_notification_details_no_jobs_in_context_to_provision() {
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, null, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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
        ArgumentCaptor<SchedulerJobWrapperImpl> jobWrapperArgumentCaptor = ArgumentCaptor.forClass(SchedulerJobWrapperImpl.class);
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), jobWrapperArgumentCaptor.capture());

        SchedulerJobWrapperImpl wrapper = jobWrapperArgumentCaptor.getValue();

        assertEquals(0, wrapper.getJobs().size());

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, emailNotificationDetails, emailNotificationContext, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
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

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, contextJobs, contextProfileRecords, Collections.EMPTY_LIST, emailNotificationContext, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(contextName);
        verify(contextProfileService).deleteByContextName(contextName);
        verify(emailNotificationDetailsService).deleteByContextName(contextName);
        verify(emailNotificationContextService).deleteByContextName(contextName);
        verify(contextInstanceRegistrationService).deRegisterByName(contextName, this.contextInstanceSchedulerService);
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

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
    }

    @Test
    public void confirm_child_context_names_populated_on_jobs() throws IOException {
        ContextService contextService = new ContextService();
        List<SchedulerJob> schedulerJobs = new ArrayList<>();
        loadFileJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/file");
        loadCommandJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/internal");
        loadQuartzJobs(schedulerJobs, "./src/test/resources/data/full-context/CONTEXT-1793100514/jobs/quartz");

        String contextJson = loadDataFile("/data/full-context/CONTEXT-1793100514/context/-1793100514.json");
        ContextTemplate contextTemplate = contextService.getContextTemplate(contextJson);

        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("http://some/url");
        moduleMetaData.setName("agentName1");
        when(moduleMetadataService.find(anyList(), any(ModuleType.class), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(List.of(moduleMetaData), 1, 1));

        EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();

        ContextBundle contextBundle = new ContextBundleImpl(contextTemplate, schedulerJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, emailNotificationContext, new ArrayList<>());
        service.provisionContext(contextBundle);

        verify(schedulerJobService).deleteByContextName(anyString());
        verify(contextProfileService).deleteByContextName(anyString());
        verify(emailNotificationDetailsService).deleteByContextName(anyString());
        verify(emailNotificationContextService).deleteByContextName(anyString());
        verify(contextInstanceRegistrationService).deRegisterByName(anyString(), any());
        verify(schedulerJobService).save(schedulerJobs, "system");
        verify(emailNotificationContextService).saveEmailNotificationContext(emailNotificationContext);

        ArgumentCaptor<ScheduledContextRecord> contextCaptor = ArgumentCaptor.forClass(ScheduledContextRecord.class);
        verify(scheduledContextService).save(contextCaptor.capture());
        ScheduledContextRecord actualContextRecord = contextCaptor.getValue();
        assertNull(null, actualContextRecord.getId());
        assertNotNull(actualContextRecord.getContext());
        assertTrue(actualContextRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && actualContextRecord.getTimestamp() <= System.currentTimeMillis());

        verify(moduleMetadataService).find(anyList(), any(ModuleType.class), anyInt(), anyInt());
        verify(jobProvisionModuleRestService).provisionJobs(anyString(), any(SchedulerJobWrapperImpl.class));

        // Assert that context names has been populated on all jobs!
        contextBundle.getSchedulerJobs().forEach(schedulerJob -> Assert.assertFalse(schedulerJob.getChildContextNames().isEmpty()));

        verifyNoMoreInteractions(
            scheduledContextService, moduleMetadataService, schedulerJobService,
            jobProvisionModuleRestService, contextInstanceRegistrationService, contextProfileService,
            emailNotificationDetailsService, emailNotificationContextService, securityService);
    }

    private ContextBundle createContextBundle(String contextName, String agentName) {
        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();
        contextTemplate.setTimeWindowStart("0 0 0 ? * * *");
        contextTemplate.setContextTtlMilliseconds(86400000);
        contextTemplate.setName(contextName);

        List<SchedulerJob> contextJobs = new ArrayList<>();
        FileEventDrivenJob fileJobRecord = new FileEventDrivenJobImpl();
        fileJobRecord.setAgentName(agentName);
        QuartzScheduleDrivenJob quartzDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzDrivenJob.setAgentName(agentName);
        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setAgentName(agentName);
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setAgentName(agentName);
        InternalEventDrivenJob internalEventDrivenJobTemplate = new InternalEventDrivenJobImpl();
        internalEventDrivenJobTemplate.setAgentName(agentName);
        internalEventDrivenJobTemplate.setTemplateJob(true);

        contextJobs.add(fileJobRecord);
        contextJobs.add(quartzDrivenJob);
        contextJobs.add(internalEventDrivenJob);
        contextJobs.add(internalEventDrivenJobTemplate);

        // Does not get provision
        contextJobs.add(globalEventJob);

        contextTemplate.setScheduledJobs(contextJobs);

        List<String> roleList = List.of("role1", "role2");
        return new ContextBundleImpl(contextTemplate, contextJobs, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, roleList);
    }
}