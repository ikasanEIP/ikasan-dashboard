package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;

public class GlobalEventJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        GlobalEventJobBuilder globalEventJobBuilder = new GlobalEventJobBuilder();

        globalEventJobBuilder
            .withAgentName(JobConstants.GLOBAL_EVENT)
            .withContextName("contextId")
            .addChildContextId("childContextId")
            .withDescription("description")
            .withJobName("jobName")
            .withStartupControlType("MANUAL");

        JSONAssert.assertEquals(super.loadDataFile("/data/global-event-job-builder-result.json"),
            service.getGlobalEventJobString(globalEventJobBuilder.build()), JSONCompareMode.STRICT);
    }

}
