package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SchedulerJobLockParticipantBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        SchedulerJobLockParticipantBuilder schedulerJobLockParticipantBuilder = new SchedulerJobLockParticipantBuilder();

        schedulerJobLockParticipantBuilder
            .withAgentName("agentName")
            .withContextName("contextId")
            .withJobName("jobName")
            .withLockCount(10);

        JSONAssert.assertEquals(super.loadDataFile("/data/scheduler-job-lock-participant.json"),
            ObjectMapperFactory.newInstance().writeValueAsString(schedulerJobLockParticipantBuilder.build()), JSONCompareMode.STRICT);
    }
}
