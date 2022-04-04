package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileEventDrivenJobBuilderTest extends AbstractTest {

    private ContextService service = new ContextService();

    @Test
    @Ignore
    public void test_builder_success() throws IOException, JSONException {
        FileEventDrivenJobBuilder fileEventDrivenJobBuilder = new FileEventDrivenJobBuilder();
        Map<String, String> passthroughProperties = new HashMap<>();
        passthroughProperties.put("key", "value");

        List<String> filenames = new ArrayList<>();
        filenames.add("file1");
        filenames.add("file2");

        fileEventDrivenJobBuilder.withFilePath("filePath")
            .withFilenames(filenames)
            .withDirectoryDepth(5)
            .withEncoding("encoding")
            .withIgnoreFileRenameWhilstScanning(false)
            .withIncludeHeader(true)
            .withIncludeTrailer(true)
            .withLogMatchedFilenames(true)
            .withSortAscending(false)
            .withSortByModifiedDateTime(true)
            .withCronExpression("cronExpression")
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
            .addChildContextId("childContextId")
            .withDescription("description")
            .withJobName("jobName")
            .withStartupControlType("MANUAL");

        JSONAssert.assertEquals(super.loadDataFile("/data/file-event-driven-job-builder-result.json"),
            service.getFileEventDrivenJobString(fileEventDrivenJobBuilder.build()), JSONCompareMode.STRICT);
    }
}
