package org.ikasan.job.orchestration.context.register;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

import java.time.ZoneId;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.quartz.TriggerBuilder.newTrigger;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceEndJobTest {

    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    @Mock
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    private ContextInstanceEndJob contextInstanceEndJob;

    private String contextName;

    private String timezone;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        timezone = RandomStringUtils.randomAlphabetic(22);
        contextInstanceEndJob = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, "0 0 2 ? * * *"
            , timezone, contextInstanceRegistrationService, contextInstanceSchedulerService);
    }

    @Test
    public void getJobName() {
        assertEquals(contextName + "-EndJob", contextInstanceEndJob.getJobName());
    }

    @Test
    public void getTimezone() {
        assertEquals(timezone, contextInstanceEndJob.getTimezone());
    }

    @Test
    public void getCronExpressionEndTime() {
        assertEquals("0 0 2 ? * * *", contextInstanceEndJob.getCronExpression());
    }

    @Test
    public void execute() throws Exception {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONTEXT_INSTANCE_ID, "xx");
        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();
        Trigger trigger = newTrigger().usingJobData(jobDataMap).build();
        jobExecutionContext.setTrigger(trigger);
        contextInstanceEndJob.execute(jobExecutionContext);

        verify(contextInstanceSchedulerService).removeEndJobTrigger(trigger);
        verify(contextInstanceRegistrationService).deRegisterById("xx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullJobName_throwsException() {
        new ContextInstanceEndJob(null, "0 0 2 ? * * *", timezone,
            contextInstanceRegistrationService, contextInstanceSchedulerService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobNameWithoutEndJobSuffix_throwsException() {
        new ContextInstanceEndJob("InvalidJobName", "0 0 2 ? * * *", timezone,
            contextInstanceRegistrationService, contextInstanceSchedulerService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullCronExpression_throwsException() {
        new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, null, timezone,
            contextInstanceRegistrationService, contextInstanceSchedulerService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullContextInstanceRegistrationService_throwsException() {
        new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, "0 0 2 ? * * *", timezone,
            null, contextInstanceSchedulerService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullContextInstanceSchedulerService_throwsException() {
        new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, "0 0 2 ? * * *", timezone,
            contextInstanceRegistrationService, null);
    }

    @Test
    public void constructor_nullTimezone_defaultsToSystemTimezone() {
        ContextInstanceEndJob job = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION,
            "0 0 2 ? * * *", null, contextInstanceRegistrationService, contextInstanceSchedulerService);

        assertEquals(ZoneId.systemDefault().getId(), job.getTimezone());
    }

    @Test(expected = JobExecutionException.class)
    public void execute_deRegistrationThrowsException_throwsJobExecutionException() throws Exception {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONTEXT_INSTANCE_ID, "xx");
        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();
        jobExecutionContext.setTrigger(newTrigger().usingJobData(jobDataMap).build());

        doThrow(new RuntimeException("Deregistration failed")).when(contextInstanceRegistrationService)
            .deRegisterById("xx");

        contextInstanceEndJob.execute(jobExecutionContext);
    }

    @Test(expected = JobExecutionException.class)
    public void execute_nullContext_throwsJobExecutionException() throws Exception {
        contextInstanceEndJob.execute(null);
    }

    @Test
    public void execute_validContext_logsContextInstanceId() throws Exception {
        final String contextInstanceId = "test-instance-123";
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONTEXT_INSTANCE_ID, contextInstanceId);
        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();
        Trigger trigger = newTrigger().usingJobData(jobDataMap).build();
        jobExecutionContext.setTrigger(trigger);

        contextInstanceEndJob.execute(jobExecutionContext);

        verify(contextInstanceRegistrationService).deRegisterById(contextInstanceId);
        verify(contextInstanceSchedulerService).removeEndJobTrigger(trigger);
    }

    @Test(expected = JobExecutionException.class)
    public void execute_removeEndJobTriggerThrowsException_throwsJobExecutionException() throws Exception {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONTEXT_INSTANCE_ID, "xx");
        JobExecutionContextDefaultImpl jobExecutionContext = new JobExecutionContextDefaultImpl();
        Trigger trigger = newTrigger().usingJobData(jobDataMap).build();
        jobExecutionContext.setTrigger(trigger);

        doThrow(new RuntimeException("Trigger removal failed")).when(contextInstanceSchedulerService)
            .removeEndJobTrigger(trigger);

        contextInstanceEndJob.execute(jobExecutionContext);
    }
}