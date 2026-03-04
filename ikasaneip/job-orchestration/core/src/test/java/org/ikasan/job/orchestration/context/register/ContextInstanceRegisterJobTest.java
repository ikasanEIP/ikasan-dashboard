package org.ikasan.job.orchestration.context.register;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRegisterJobTest {

    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    @Mock
    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    private ContextInstanceRegisterJob contextInstanceRegisterJob;

    private String contextName;

    private String timezone;

    @Mock
    private ContextTemplate contextTemplate;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        timezone = RandomStringUtils.randomAlphabetic(22);
        contextInstanceRegisterJob = new ContextInstanceRegisterJob(contextName, "0 0 6 ? * * *", timezone,
            contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.contextTemplate
            , 5, 2000);
    }

    @Test
    public void getJobName() {
        assertEquals(contextName, contextInstanceRegisterJob.getJobName());
    }

    @Test
    public void getTimezone() {
        assertEquals(timezone, contextInstanceRegisterJob.getTimezone());
    }

    @Test
    public void getCronExpressionEndTime() {
        assertEquals("0 0 6 ? * * *", contextInstanceRegisterJob.getCronExpression());
    }

    @Test
    public void execute() throws Exception {
        contextInstanceRegisterJob.execute(new JobExecutionContextDefaultImpl());

        verify(contextInstanceRegistrationService).register(contextName, this.contextInstanceSchedulerService);
    }

    @Test
    public void execute_with_retry_followed_by_success() throws Exception {
        doThrow(new RuntimeException("error!"))
            .doNothing()
            .when(contextInstanceRegistrationService).register(any(), any());

        contextInstanceRegisterJob.execute(new JobExecutionContextDefaultImpl());

        verify(contextInstanceRegistrationService, times(2))
            .register(contextName, this.contextInstanceSchedulerService);
    }

    @Test(expected = JobExecutionException.class)
    public void execute_with_retry_followed_by_recovery_but_retries_exceeded() throws Exception {
        Exception exception = new RuntimeException("error!");

        doThrow(exception)
            .doThrow(exception)
            .doThrow(exception)
            .doThrow(exception)
            .doThrow(exception)
            .doNothing()
            .when(contextInstanceRegistrationService).register(any(), any());

        contextInstanceRegisterJob.execute(new JobExecutionContextDefaultImpl());

        verify(contextInstanceRegistrationService, times(5))
            .register(contextName, this.contextInstanceSchedulerService);
    }

    @Test(expected = JobExecutionException.class)
    public void execute_with_one_retry_exception() throws Exception {
        contextInstanceRegisterJob = new ContextInstanceRegisterJob(contextName, "0 0 6 ? * * *", timezone,
            contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.contextTemplate
            , 1, 2000);

        Exception exception = new RuntimeException("error!");

        doThrow(exception)
            .when(contextInstanceRegistrationService).register(any(), any());

        contextInstanceRegisterJob.execute(new JobExecutionContextDefaultImpl());

        verify(contextInstanceRegistrationService, times(1))
            .register(contextName, this.contextInstanceSchedulerService);
    }
}