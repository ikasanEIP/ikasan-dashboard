package org.ikasan.job.orchestration.context.register;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.util.ScheduledContextRecordTestSearchResults;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.springframework.test.util.ReflectionTestUtils;


@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceSchedulerServiceTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduledJobFactory scheduledJobFactory;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Mock
    private JobLockCacheService jobLockCacheService;

    @Mock
    private ModuleMetaDataService moduleMetadataService;

    @Mock
    private ContextParametersInstanceService contextParametersInstanceService;

    @Mock
    private ContextParametersUpdateService contextParametersUpdateService;

    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    @Before
    public void setUp() {
        contextInstanceSchedulerService = new ContextInstanceSchedulerService(scheduler, scheduledJobFactory,
            scheduledContextService, scheduledContextInstanceService, schedulerService,
            internalEventDrivenJobService, "/data/somequeue", jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService, true);
    }

    @Test
    public void should_do_nothing_if_featured_flagged_off() {
        assertEquals(0, ContextMachineCache.instance().contextNames().size());

        ReflectionTestUtils.setField(contextInstanceSchedulerService, "usePostConstructs", false);

        contextInstanceSchedulerService.registerJobs();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory,
            scheduledContextService, scheduledContextInstanceService, schedulerService,
            internalEventDrivenJobService, jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService);

        // ensure no contexts
        assertEquals(0, ContextMachineCache.instance().contextNames().size());
    }

    @Test
    public void registerJobs_does_nothing_no_search_results() {
        ScheduledContextRecordTestSearchResults results = new ScheduledContextRecordTestSearchResults(0, true);
        when(scheduledContextService.findAll()).thenReturn(results);

        contextInstanceSchedulerService.registerJobs();

        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory,
            scheduledContextService, scheduledContextInstanceService, schedulerService,
            internalEventDrivenJobService, jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService);

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(0, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(0, dashboardJobDetailsMap.size());
    }

    @Test
    public void registerJobs_if_not_outside_of_operating_window() {
        ScheduledContextRecordTestSearchResults results = new ScheduledContextRecordTestSearchResults(3, false);
        when(scheduledContextService.findAll()).thenReturn(results);

        contextInstanceSchedulerService.registerJobs();

        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory,
            scheduledContextService, scheduledContextInstanceService, schedulerService,
            internalEventDrivenJobService, jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService);

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(0, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(0, dashboardJobDetailsMap.size());
    }

    @Test
    public void registerJobs_if_outside_operating_window() throws SchedulerException {
        ScheduledContextRecordTestSearchResults results = new ScheduledContextRecordTestSearchResults(3, true);
        when(scheduledContextService.findAll()).thenReturn(results);

        JobDetailImpl detail1 = new JobDetailImpl();
        detail1.setName("ContextName1");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName1"), eq("context"))).thenReturn(detail1);

        JobDetailImpl endDetail1 = new JobDetailImpl();
        endDetail1.setName("ContextName1-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName1-EndJob"), eq("context"))).thenReturn(endDetail1);

        JobDetailImpl detail2 = new JobDetailImpl();
        detail2.setName("ContextName2");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName2"), eq("context"))).thenReturn(detail2);

        JobDetailImpl endDetail2 = new JobDetailImpl();
        endDetail2.setName("ContextName2-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName2-EndJob"), eq("context"))).thenReturn(endDetail2);

        JobDetailImpl detail3 = new JobDetailImpl();
        detail3.setName("ContextName3");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName3"), eq("context"))).thenReturn(detail3);

        JobDetailImpl endDetail3 = new JobDetailImpl();
        endDetail3.setName("ContextName3-EndJob");
        when(scheduledJobFactory.createJobDetail(any(), any(), eq("ContextName3-EndJob"), eq("context"))).thenReturn(endDetail3);


        when(scheduler.checkExists(detail1.getKey())).thenReturn(true).thenReturn(false);
        when(scheduler.checkExists(detail2.getKey())).thenReturn(true).thenReturn(false);
        when(scheduler.checkExists(detail3.getKey())).thenReturn(true).thenReturn(false);

        when(scheduler.checkExists(endDetail1.getKey())).thenReturn(true).thenReturn(false);
        when(scheduler.checkExists(endDetail2.getKey())).thenReturn(true).thenReturn(false);
        when(scheduler.checkExists(endDetail3.getKey())).thenReturn(true).thenReturn(false);

        when(scheduler.deleteJob(detail1.getKey())).thenReturn(true);
        when(scheduler.deleteJob(detail2.getKey())).thenReturn(true);
        when(scheduler.deleteJob(detail3.getKey())).thenReturn(true);

        when(scheduler.deleteJob(endDetail1.getKey())).thenReturn(true);
        when(scheduler.deleteJob(endDetail2.getKey())).thenReturn(true);
        when(scheduler.deleteJob(endDetail3.getKey())).thenReturn(true);

        when(scheduler.scheduleJob(any(), any())).thenReturn(new Date());

        contextInstanceSchedulerService.registerJobs();

        verify(scheduledContextService).findAll();
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName1"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName2"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName3"), eq("context"));

        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName1-EndJob"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName2-EndJob"), eq("context"));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName3-EndJob"), eq("context"));

        verify(scheduler, times(2)).checkExists(detail1.getKey());
        verify(scheduler, times(2)).checkExists(detail2.getKey());
        verify(scheduler, times(2)).checkExists(detail3.getKey());

        verify(scheduler, times(2)).checkExists(endDetail1.getKey());
        verify(scheduler, times(2)).checkExists(endDetail2.getKey());
        verify(scheduler, times(2)).checkExists(endDetail3.getKey());

        verify(scheduler).deleteJob(detail1.getKey());
        verify(scheduler).deleteJob(detail2.getKey());
        verify(scheduler).deleteJob(detail3.getKey());

        verify(scheduler).deleteJob(endDetail1.getKey());
        verify(scheduler).deleteJob(endDetail2.getKey());
        verify(scheduler).deleteJob(endDetail3.getKey());

        verify(scheduler, times(6)).scheduleJob(any(), any());

        verifyNoMoreInteractions(scheduler, scheduledJobFactory,
            scheduledContextService, scheduledContextInstanceService, schedulerService,
            internalEventDrivenJobService, jobLockCacheService, contextParametersInstanceService,
            moduleMetadataService, contextParametersUpdateService);

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(6, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(6, dashboardJobDetailsMap.size());
    }
}