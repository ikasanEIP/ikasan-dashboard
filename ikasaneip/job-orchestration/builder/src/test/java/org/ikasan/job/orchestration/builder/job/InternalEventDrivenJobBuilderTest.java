package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.Calendar;

public class InternalEventDrivenJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    public void test_builder_success() throws IOException, JSONException {
        InternalEventDrivenJobBuilder internalEventDrivenJobBuilder = new InternalEventDrivenJobBuilder();
        internalEventDrivenJobBuilder
            .withCommandLine("commandLine")
            .withMaxExecutionTime(100000L)
            .withMinExecutionTime(1000L)
            .withWorkingDirectory("working directory")
            .addDayOfWeekToRun(Calendar.MONDAY).addDayOfWeekToRun(Calendar.TUESDAY)
            .addContextParameter(internalEventDrivenJobBuilder.getContextParameterBuilder()
                .withName("name1")
                .withType("java.lang.String")
                .build())
            .addContextParameter(internalEventDrivenJobBuilder.getContextParameterBuilder()
                .withName("name2")
                .withType("java.lang.String")
                .build())
            .addSuccessfulReturnCode("0")
            .withContextId("contextId")
            .withAgentName("agentName")
            .withContextId("contextId")
            .addChildContextId("childContextId")
            .withDescription("description")
            .withJobName("jobName");

        JSONAssert.assertEquals(super.loadDataFile("/data/internal-event-driven-job-buildr-result.json"),
            service.getInternalEventDrivenJobString(internalEventDrivenJobBuilder.build()), JSONCompareMode.STRICT);
    }
}
