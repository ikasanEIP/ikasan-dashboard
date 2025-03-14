package org.ikasan.job.orchestration.context.register;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.orchestration.service.context.register.ContextInstanceRegistrationServiceImpl;
import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
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
import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEventService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_END_GROUP;
import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_START_GROUP;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRegistrationLifecycleTest {
    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduledJobFactory scheduledJobFactory;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobInstanceService schedulerJobInstanceService;

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
    private ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;

    @Mock
    private SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;

    @Mock
    private JobLockCacheInitialisationService jobLockCacheInitialisationService;
    @Mock
    private TimeService timeService;
    @Mock
    private ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster;
    @Mock
    private SystemEventService systemEventService;
    @Mock
    private JobUtilsService jobUtilsService;
    @Mock
    private JobExecutionContext jobExecutionContext;
    @Mock
    private JobDetail endJobDetail;
    @Mock
    private JobProvisionService jobProvisionService;
    @Mock
    private SchedulerJobService schedulerJobService;

    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    MockedStatic<AbstractDashboardSchedulerService>  mockStatic;

    @Before
    public void setUp() {
        mockStatic = mockStatic(AbstractDashboardSchedulerService.class);
        contextInstanceRegistrationService = new ContextInstanceRegistrationServiceImpl(
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

        contextInstanceSchedulerService = new ContextInstanceSchedulerServiceImpl(scheduler, scheduledJobFactory,
            scheduledContextService, contextInstanceRegistrationService, this.timeService, true, true);
        ContextMachineCache.instance().resetAllCache();
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());
    }

    @After
    public void tearDown() {
        mockStatic.close();
    }

    /**
     * Method to register a job plan configured to run on the second day of the month, with the time
     * yet to have fallen in the current month. This method sets up the necessary context, schedules
     * the job plan, and verifies the scheduling of the job based on the specified cron expression.
     *
     * @throws SchedulerException if an error occurs with the scheduler
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void register_job_plan_configured_to_run_on_second_day_of_month_with_time_yet_to_have_fallen_in_current_month() throws SchedulerException, IOException {
        // The job plan has a cron "0 17 14 2W * ?" and "customWeekDayOfMonth": true, therefore is subject to the Ikasan custom approach to
        // scheduling the job plan to run on the nth weekday of the month.
        String jsonContext = new String(new ClassPathResource("data/context-nth-business-day-of-month.json").getInputStream().readAllBytes());
        ContextTemplate contextTemplate = new ContextService().getContextTemplate(jsonContext);
        ContextInstance contextInstance = new ContextService().getContextInstance(jsonContext);
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setContextName(contextTemplate.getName());
        scheduledContextRecord.setContext(new ContextService().getContextTemplate(jsonContext));

        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setContextInstanceId(contextInstance.getId());
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());

        SearchResults contextSearchResults = new SearchResultsImpl(List.of(scheduledContextRecord), 1, 1);
        SearchResults contextInstanceSearchResults = new SearchResultsImpl(List.of(scheduledContextInstanceRecord), 1, 1);
        SearchResults internalEventDrivenJobRecordSearchResults = new SearchResultsImpl(List.of(), 1, 1);
        ModuleMetadataSearchResults agentSearchResults = new ModuleMetadataSearchResults(List.of(), 1, 1);

        when(scheduledContextService.findAll()).thenReturn(contextSearchResults);
        when(scheduledContextService.findById(anyString())).thenReturn(scheduledContextRecord);
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(contextInstanceSearchResults);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.find(any(), any(), anyInt(), anyInt())).thenReturn(agentSearchResults);
        when(this.scheduledJobFactory.createJobDetail(any(), any(), anyString(), anyString())).thenReturn(this.endJobDetail);
        when(this.endJobDetail.getKey()).thenReturn(new JobKey(contextTemplate.getName()));
        mockStatic.when(() -> AbstractDashboardSchedulerService.showAllTriggers(scheduler)).thenReturn("triggers");

        // We will pivot off the current time being 1st March 2025 - 14:15:00
        when(this.timeService.getLocalDateNow()).thenReturn(LocalDateTime.of(2025, 3, 1, 14, 15, 0));

        createJobDetailAndSetStub(contextTemplate.getName(), CONTEXT_START_GROUP);
        when(scheduler.scheduleJob(any(), any())).thenReturn(new Date());

        // Go ahead and register the jobs against the scheduler
        contextInstanceSchedulerService.registerJobs();

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(1, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(1, dashboardJobDetailsMap.size());

        // Confirm the the job has been scheduled to run at 4th March 2025 - 14:17:00. 1st and 2nd of March 2025 are Saturday and Sunday so we
        // expect the job plan to be started on the 4th March 2025.
        assertEquals("0 17 14 4 3 ? 2025", dashboardJobsMap.get("context start.CONTEXT-1436221681").getCronExpression());

        // Now wee will pivot off the current time being 4th March 2025 - 14:17:00 which is when the quartz job fires
        when(this.timeService.getLocalDateNow()).thenReturn(LocalDateTime.of(2025, 3, 4, 14,17, 0));

        // The act of executing the job will also schedule the job plan to run on the 2nd weekday of the next month.
        dashboardJobsMap.get("context start.CONTEXT-1436221681").execute(this.jobExecutionContext);

        // after the job has executed an end job is created
        assertNotNull(dashboardJobsMap.get("DEFAULT.CONTEXT-1436221681"));
        assertEquals("CONTEXT-1436221681-EndJob", dashboardJobsMap.get("DEFAULT.CONTEXT-1436221681").getJobName());


        verify(scheduledContextService).findAll();
        verify(scheduledJobFactory, times(2)).createJobDetail(any(), any(), eq(contextTemplate.getName()), eq(CONTEXT_START_GROUP));
        verify(scheduledJobFactory, times(1)).createJobDetail(any(), any(), eq(contextTemplate.getName()+"-EndJob"), eq(CONTEXT_END_GROUP));

        verify(scheduler, times(5)).checkExists((JobKey) any());
        verify(scheduler, times(3)).scheduleJob(any(), any());
        verify(scheduler, times(3)).getTriggersOfJob(any());
        verify(scheduledContextService, times(1)).findAll();
        verify(scheduledContextService, times(4)).findById(anyString());
        verify(scheduledContextInstanceService, times(4)).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(schedulerJobInstanceService, times(21)).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(moduleMetadataService, times(3)).find(any(), any(), anyInt(), anyInt());

        mockStatic.verify(() -> AbstractDashboardSchedulerService.showAllTriggers(scheduler),(times(2)));

        dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(2, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(2, dashboardJobDetailsMap.size());

        // Confirm the the job has been scheduled to run at 2nd April 2025 - 14:17:00.
        assertEquals("0 17 14 2 4 ? 2025", dashboardJobsMap.get("context start.CONTEXT-1436221681").getCronExpression());
        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService);
    }

    /**
     * Method to register a job plan configured to run on the second day of the month, with the time
     * already having fallen in the current month. This method sets up the necessary context, schedules
     * the job plan, and verifies the scheduling of the job based on the specified cron expression.
     *
     * @throws SchedulerException if an error occurs with the scheduler
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void register_job_plan_configured_to_run_on_second_day_of_month_with_time_already_have_fallen_in_current_month() throws SchedulerException, IOException {
        // The job plan has a cron "0 17 14 2W * ?" and "customWeekDayOfMonth": true, therefore is subject to the Ikasan custom approach to
        // scheduling the job plan to run on the nth weekday of the month.
        String jsonContext = new String(new ClassPathResource("data/context-nth-business-day-of-month.json").getInputStream().readAllBytes());
        ContextTemplate contextTemplate = new ContextService().getContextTemplate(jsonContext);
        ContextInstance contextInstance = new ContextService().getContextInstance(jsonContext);
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setContextName(contextTemplate.getName());
        scheduledContextRecord.setContext(new ContextService().getContextTemplate(jsonContext));

        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setContextInstanceId(contextInstance.getId());
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());

        SearchResults contextSearchResults = new SearchResultsImpl(List.of(scheduledContextRecord), 1, 1);
        SearchResults contextInstanceSearchResults = new SearchResultsImpl(List.of(scheduledContextInstanceRecord), 1, 1);
        SearchResults internalEventDrivenJobRecordSearchResults = new SearchResultsImpl(List.of(), 1, 1);
        ModuleMetadataSearchResults agentSearchResults = new ModuleMetadataSearchResults(List.of(), 1, 1);

        when(scheduledContextService.findAll()).thenReturn(contextSearchResults);
        when(scheduledContextService.findById(anyString())).thenReturn(scheduledContextRecord);
        when(scheduledContextInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(contextInstanceSearchResults);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull())).thenReturn(internalEventDrivenJobRecordSearchResults);
        when(moduleMetadataService.find(any(), any(), anyInt(), anyInt())).thenReturn(agentSearchResults);
        when(this.scheduledJobFactory.createJobDetail(any(), any(), anyString(), anyString())).thenReturn(this.endJobDetail);
        when(this.endJobDetail.getKey()).thenReturn(new JobKey(contextTemplate.getName()));
        mockStatic.when(() -> AbstractDashboardSchedulerService.showAllTriggers(scheduler)).thenReturn("triggers");

        // We will pivot off the current time being 4th March 2025 - 14:25:00
        when(this.timeService.getLocalDateNow()).thenReturn(LocalDateTime.of(2025, 3, 4, 14, 25, 0));

        createJobDetailAndSetStub(contextTemplate.getName(), CONTEXT_START_GROUP);
        when(scheduler.scheduleJob(any(), any())).thenReturn(new Date());

        // Go ahead and register the jobs against the scheduler
        contextInstanceSchedulerService.registerJobs();

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(1, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(1, dashboardJobDetailsMap.size());

        // Confirm the the job has been scheduled to run at 4th March 2025 - 14:17:00. 1st and 2nd of March 2025 are Saturday and Sunday so we
        // expect the job plan to be started on the 4th March 2025.
        assertEquals("0 17 14 2 4 ? 2025", dashboardJobsMap.get("context start.CONTEXT-1436221681").getCronExpression());

        // Now wee will pivot off the current time being 2nd April 2025 - 14:17:00 which is when the quartz job fires
        when(this.timeService.getLocalDateNow()).thenReturn(LocalDateTime.of(2025, 4, 2, 14,17, 0));

        // The act of executing the job will also schedule the job plan to run on the 2nd weekday of the next month.
        dashboardJobsMap.get("context start.CONTEXT-1436221681").execute(this.jobExecutionContext);

        // after the job has executed an end job is created
        assertNotNull(dashboardJobsMap.get("DEFAULT.CONTEXT-1436221681"));
        assertEquals("CONTEXT-1436221681-EndJob", dashboardJobsMap.get("DEFAULT.CONTEXT-1436221681").getJobName());


        verify(scheduledContextService).findAll();
        verify(scheduledJobFactory, times(2)).createJobDetail(any(), any(), eq(contextTemplate.getName()), eq(CONTEXT_START_GROUP));
        verify(scheduledJobFactory, times(1)).createJobDetail(any(), any(), eq(contextTemplate.getName()+"-EndJob"), eq(CONTEXT_END_GROUP));

        verify(scheduler, times(5)).checkExists((JobKey) any());
        verify(scheduler, times(3)).scheduleJob(any(), any());
        verify(scheduler, times(3)).getTriggersOfJob(any());
        verify(scheduledContextService, times(1)).findAll();
        verify(scheduledContextService, times(4)).findById(anyString());
        verify(scheduledContextInstanceService, times(4)).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(schedulerJobInstanceService, times(28)).getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(moduleMetadataService, times(4)).find(any(), any(), anyInt(), anyInt());

        mockStatic.verify(() -> AbstractDashboardSchedulerService.showAllTriggers(scheduler),(times(2)));

        dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(2, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(2, dashboardJobDetailsMap.size());

        // Confirm the the job has been scheduled to run at 2nd May 2025 - 14:17:00.
        assertEquals("0 17 14 2 5 ? 2025", dashboardJobsMap.get("context start.CONTEXT-1436221681").getCronExpression());
        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService);
    }

    private void createJobDetailAndSetStub(String name, String group) {
        JobDetailImpl detail = new JobDetailImpl();
        detail.setName(name);
        detail.setGroup(group);
        when(scheduledJobFactory.createJobDetail(any(), any(), eq(name), eq(group))).thenReturn(detail);
    }


    private class ScheduledContextRecordTestSearchResults<ANY> implements SearchResults<ScheduledContextRecord> {

        public static final String CONTEXT_NAME = "ContextName";

        private final int number;
        private final boolean outsideOfOperatingWindow;

        public ScheduledContextRecordTestSearchResults(int number, boolean outsideOfOperatingWindow) {
            this.number = number;
            this.outsideOfOperatingWindow = outsideOfOperatingWindow;

        }

        @Override
        public List<ScheduledContextRecord> getResultList() {
            List<ScheduledContextRecord> results = new ArrayList<>();
            for (int i = 1; i < number + 1; i++) {
                ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
                record.setContextName(CONTEXT_NAME + i);
                ContextTemplateImpl context = new ContextTemplateImpl();
                context.setName(CONTEXT_NAME + i);
                if (outsideOfOperatingWindow) {
                    context.setTimeWindowStart("59 59 23 ? * * *");
                } else {
                    context.setTimeWindowStart("* * 0 ? * * *");
                }
                record.setContext(context);
                results.add(record);
            }
            return results;
        }

        @Override
        public long getTotalNumberOfResults() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getQueryResponseTime() {
            throw new UnsupportedOperationException();
        }
    }
}
