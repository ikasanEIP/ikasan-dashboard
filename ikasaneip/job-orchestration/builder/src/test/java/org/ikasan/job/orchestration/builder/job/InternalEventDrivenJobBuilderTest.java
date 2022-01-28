package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;

public class InternalEventDrivenJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        InternalEventDrivenJobBuilder fileEventDrivenJobBuilder = new InternalEventDrivenJobBuilder();
        fileEventDrivenJobBuilder
            .withCommandLine("commandLine")
            .withMaxExecutionTime(100000L)
            .withMinExecutionTime(1000L)
            .withWorkingDirectory("working directory")
            .addContextParameter(fileEventDrivenJobBuilder.getContextParameterBuilder()
                .withName("name1")
                .withType("java.lang.String")
                .build())
            .addContextParameter(fileEventDrivenJobBuilder.getContextParameterBuilder()
                .withName("name2")
                .withType("java.lang.String")
                .build())
            .addSuccessfulReturnCode("0")
            .withContextId("contextId")
            .withAgentName("agentName")
            .withContextId("contextId")
            .withDescription("description")
            .withJobName("jobName");

        JSONAssert.assertEquals(super.loadDataFile("/data/internal-event-driven-job-buildr-result.json"),
            service.getInternalEventDrivenJobString(fileEventDrivenJobBuilder.build()), JSONCompareMode.STRICT);
    }
}
