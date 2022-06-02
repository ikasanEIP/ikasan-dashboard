package org.ikasan.job.orchestration.context.register;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceEndJobTest {

    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextInstanceEndJob contextInstanceEndJob;

    private String contextName;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        contextInstanceEndJob = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, "0 0 2 ? * * *", contextInstanceRegistrationService);
    }

    @Test
    public void getJobName() {
        assertEquals(contextName + "-EndJob", contextInstanceEndJob.getJobName());
    }

    @Test
    public void getCronExpressionEndTime() {
        assertEquals("0 0 2 ? * * *", contextInstanceEndJob.getCronExpression());
    }

    @Test
    public void execute() throws Exception {
        contextInstanceEndJob.execute(new JobExecutionContextDefaultImpl());

        verify(contextInstanceRegistrationService).deRegister(contextName);
    }
}