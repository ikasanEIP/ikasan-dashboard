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

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
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
        jobExecutionContext.setTrigger(newTrigger().usingJobData(jobDataMap).build());
        contextInstanceEndJob.execute(jobExecutionContext);

        verify(contextInstanceRegistrationService).deRegisterById("xx");
    }
}