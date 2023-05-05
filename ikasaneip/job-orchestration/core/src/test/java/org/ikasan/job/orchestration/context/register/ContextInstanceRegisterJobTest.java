package org.ikasan.job.orchestration.context.register;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.RandomStringUtils;
import org.ikasan.serialiser.model.JobExecutionContextDefaultImpl;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContextInstanceRegisterJobTest {

    @Mock
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    private ContextInstanceRegisterJob contextInstanceRegisterJob;

    private String contextName;

    private String timezone;

    @Mock
    private ContextProvisionService contextProvisionService;

    @Before
    public void setUp() {
        contextName = RandomStringUtils.randomAlphabetic(22);
        timezone = RandomStringUtils.randomAlphabetic(22);
        contextInstanceRegisterJob = new ContextInstanceRegisterJob(contextName, "0 0 6 ? * * *", timezone,
            contextInstanceRegistrationService);
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

        verify(contextInstanceRegistrationService).register(contextName, null);
    }
}