package org.ikasan.job.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ContextHelperTest {

    ContextService contextService = new ContextService();

    @Test
    public void test_get_() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        List<SchedulerJobInstance> precedingJobsFromOutsideContext = ContextHelper.getPrecedingJobsFromOutsideContext
            (contextInstance, "-505061472", "CONTEXT--2036736597", new HashMap<>());

        System.out.println(precedingJobsFromOutsideContext);
    }

    @Test
    public void test_hold_all_jobs_for_context_instance() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.holdAllJobs(contextInstance);

        System.out.println(contextInstance);
    }

    @Test
    public void test_hold_all_jobs_for_context_template() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        ContextHelper.holdAllJobs(contextTemplate);

        System.out.println(contextTemplate);
    }

    @Test
    public void test() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/CONTEXT-36916071.json"));

        ContextHelper.enrichJobs(contextInstance);

        LinkedList<List<SchedulerJobInstance>> identifiers = ContextHelper.traceJobThroughContextInstance
            (contextInstance, "1779796515", "CONTEXT-1967431808");

        System.out.println(identifiers);
    }

    @Test
    public void test2() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        LinkedList<List<SchedulerJobInstance>> identifiers = ContextHelper.traceJobThroughContextInstance
            (contextInstance, "1010295672", "CONTEXT-1892741766");

        System.out.println(identifiers);

        identifiers = ContextHelper.traceJobThroughContextInstance
            (contextInstance, "1010295672", "CONTEXT-1892741766");

        System.out.println(identifiers);
    }

    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }
}
