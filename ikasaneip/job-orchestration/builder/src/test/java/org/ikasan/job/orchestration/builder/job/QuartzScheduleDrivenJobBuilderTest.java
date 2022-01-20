package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;

public class QuartzScheduleDrivenJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        QuartzScheduleDrivenJobBuilder quartzScheduleDrivenJobBuilder = new QuartzScheduleDrivenJobBuilder();

        quartzScheduleDrivenJobBuilder.withCronExpression("cronExpression")
            .withJobGroup("jobGroup")
            .withTimeZone("timezone")
            .withAgentName("agentName")
            .withContextId("contextId")
            .withDescription("description")
            .withJobName("jobName");

        JSONAssert.assertEquals(super.loadDataFile("/data/quartz-event-driven-job-builder-result.json"),
            service.getQuartzScheduleDrivenJobString(quartzScheduleDrivenJobBuilder.build()), JSONCompareMode.STRICT);
    }
}
