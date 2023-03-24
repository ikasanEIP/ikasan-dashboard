package org.ikasan.job.orchestration.context.register;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_START_GROUP;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceSchedulerServiceTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduledJobFactory scheduledJobFactory;

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    @Before
    public void setUp() {
        contextInstanceSchedulerService = new ContextInstanceSchedulerService(scheduler, scheduledJobFactory,
            scheduledContextService, contextInstanceRegistrationService, true);
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void should_do_nothing_if_featured_flagged_off() {
        assertTrue(ContextMachineCache.instance().cacheIsEmpty());

        ReflectionTestUtils.setField(contextInstanceSchedulerService, "isContextLifeCycleActive", false);

        contextInstanceSchedulerService.registerJobs();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService);
    }

    @Test
    public void registerJobs_does_nothing_no_search_results() {
        ScheduledContextRecordTestSearchResults results = new ScheduledContextRecordTestSearchResults(0, true);
        when(scheduledContextService.findAll()).thenReturn(results);

        contextInstanceSchedulerService.registerJobs();

        verify(scheduledContextService).findAll();

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService);

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(0, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(0, dashboardJobDetailsMap.size());
    }

    @Test
    public void registerScheduledContextJobsCreatesStartJobsButNotEndJobs() throws SchedulerException {
        ScheduledContextRecordTestSearchResults results = new ScheduledContextRecordTestSearchResults(3, true);
        when(scheduledContextService.findAll()).thenReturn(results);

        createJobDetailAndSetStub("ContextName1", CONTEXT_START_GROUP);
        createJobDetailAndSetStub("ContextName2", CONTEXT_START_GROUP);
        createJobDetailAndSetStub("ContextName3", CONTEXT_START_GROUP);
        when(scheduler.scheduleJob(any(), any())).thenReturn(new Date());

        contextInstanceSchedulerService.registerJobs();

        verify(scheduledContextService).findAll();
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName1"), eq(CONTEXT_START_GROUP));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName2"), eq(CONTEXT_START_GROUP));
        verify(scheduledJobFactory).createJobDetail(any(), any(), eq("ContextName3"), eq(CONTEXT_START_GROUP));

        verify(scheduler, times(3)).checkExists((JobKey) any());
        verify(scheduler, times(3)).scheduleJob(any(), any());
        verify(scheduler, times(3)).getTriggersOfJob(any());

        verifyNoMoreInteractions(scheduler, scheduledJobFactory, scheduledContextService);

        Map<String, DashboardJob> dashboardJobsMap = (Map<String, DashboardJob>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobsMap");
        Map<String, JobDetail> dashboardJobDetailsMap = (Map<String, JobDetail>) ReflectionTestUtils.getField(contextInstanceSchedulerService, "dashboardJobDetailsMap");

        assertNotNull(dashboardJobsMap);
        assertEquals(3, dashboardJobsMap.size());

        assertNotNull(dashboardJobDetailsMap);
        assertEquals(3, dashboardJobDetailsMap.size());
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
//                    context.setTimeWindowEnd("59 59 23 ? * * *");
                } else {
                    context.setTimeWindowStart("* * 0 ? * * *");
//                    context.setTimeWindowEnd("* * 23 ? * * *");
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