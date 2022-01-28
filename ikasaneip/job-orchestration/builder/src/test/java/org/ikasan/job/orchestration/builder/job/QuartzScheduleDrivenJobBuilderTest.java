package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuartzScheduleDrivenJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        QuartzScheduleDrivenJobBuilder quartzScheduleDrivenJobBuilder = new QuartzScheduleDrivenJobBuilder();

        Map<String, String> passthroughProperties = new HashMap<>();
        passthroughProperties.put("key", "value");

        quartzScheduleDrivenJobBuilder.withCronExpression("cronExpression")
            .withJobGroup("jobGroup")
            .withTimeZone("timezone")
            .withEager(true)
            .withIgnoreMisfire(true)
            .withMaxEagerCallbacks(10)
            .withPassthroughProperties(passthroughProperties)
            .withPersistentRecovery(true)
            .withRecoveryTolerance(100L)
            .withAgentName("agentName")
            .withContextId("contextId")
            .withDescription("description")
            .withJobName("jobName")
            .withStartupControlType("MANUAL");

        JSONAssert.assertEquals(super.loadDataFile("/data/quartz-event-driven-job-builder-result.json"),
            service.getQuartzScheduleDrivenJobString(quartzScheduleDrivenJobBuilder.build()), JSONCompareMode.STRICT);
    }
}
