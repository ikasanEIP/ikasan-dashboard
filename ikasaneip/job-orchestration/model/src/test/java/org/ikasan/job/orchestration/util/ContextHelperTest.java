package org.ikasan.job.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// todo extensive tests need to be written here
public class ContextHelperTest {

    ContextService contextService = new ContextService();

    @Test
    public void test_context_template_token_replacement() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/-1793100514-with-tokens.json")
            , ObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.STRICT);
    }

    @Test
    public void test_context_template_token_replacement_with_job_locks() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));

        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks-with-tokens.json")
            , ObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.STRICT);
    }

    @Test
    public void test_scheduler_job_token_replacement() {
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setJobName("jobName");
        internalEventDrivenJob.setAgentName("agentName");
        internalEventDrivenJob.setIdentifier("agentName-jobName");

        ContextHelper.addSchedulerJobReplacementTokens(internalEventDrivenJob);

        Assert.assertEquals("[[agent-name]]", internalEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent-name]]-jobName", internalEventDrivenJob.getIdentifier());
        Assert.assertEquals("[[context-name]]", internalEventDrivenJob.getContextName());

        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setJobName("jobName");
        quartzScheduleDrivenJob.setAgentName("agentName");
        quartzScheduleDrivenJob.setIdentifier("agentName-jobName");

        ContextHelper.addSchedulerJobReplacementTokens(quartzScheduleDrivenJob);

        Assert.assertEquals("[[agent-name]]", quartzScheduleDrivenJob.getAgentName());
        Assert.assertEquals("[[agent-name]]-jobName", quartzScheduleDrivenJob.getIdentifier());
        Assert.assertEquals("[[context-name]]", quartzScheduleDrivenJob.getContextName());

        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setJobName("jobName");
        fileEventDrivenJob.setAgentName("agentName");
        fileEventDrivenJob.setIdentifier("agentName-jobName");

        ContextHelper.addSchedulerJobReplacementTokens(fileEventDrivenJob);

        Assert.assertEquals("[[agent-name]]", fileEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent-name]]-jobName", fileEventDrivenJob.getIdentifier());
        Assert.assertEquals("[[context-name]]", fileEventDrivenJob.getContextName());

        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setJobName("jobName");
        globalEventJob.setAgentName("agentName");
        globalEventJob.setIdentifier("agentName-jobName");

        ContextHelper.addSchedulerJobReplacementTokens(globalEventJob);

        Assert.assertEquals("GLOBAL_EVENT", globalEventJob.getAgentName());
        Assert.assertEquals("GLOBAL_EVENT-jobName", globalEventJob.getIdentifier());
        Assert.assertEquals("[[context-name]]", globalEventJob.getContextName());
    }

    @Test
    public void test_get_preceding_jobs_from_outside_context() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        List<SchedulerJobInstance> precedingJobsFromOutsideContext = ContextHelper.getPrecedingJobsFromOutsideContext
            (contextInstance, "-505061472", "CONTEXT--2036736597", new HashMap<>());

        Assert.assertFalse(precedingJobsFromOutsideContext.isEmpty());
        Assert.assertEquals(precedingJobsFromOutsideContext.get(0).getJobName(), "-1140126585");
        Assert.assertEquals(precedingJobsFromOutsideContext.get(1).getJobName(), "1442271260");
        Assert.assertEquals(precedingJobsFromOutsideContext.get(2).getJobName(), "328062799");
        Assert.assertEquals(precedingJobsFromOutsideContext.get(3).getJobName(), "141012036");
    }

    @Test
    public void test_hold_all_jobs_for_context_instance() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        ContextHelper.holdAllJobs(contextInstance, createInternalJobsMap(contextTemplate));

        AggregateContextInstanceStatus aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isHeldJobs());
    }

    @Test
    public void test_trace_job_through_context() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        LinkedList<List<SchedulerJob>> identifiers = ContextHelper.traceJobThroughContext
            (contextInstance, "1010295672", "CONTEXT-1892741766");

        Assert.assertEquals("185916817", identifiers.get(0).get(0).getJobName());
        Assert.assertEquals("-958075417", identifiers.get(1).get(0).getJobName());
    }

    @Test
    public void test_get_aggregate_instance_status() throws IOException {
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514_with_jobs_held_disabled_and_skipped.json"));

        AggregateContextInstanceStatus aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance);

        Assert.assertTrue(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isHeldJobs());


        ContextInstance childContext = (ContextInstance) ContextHelper
            .getChildContext("CONTEXT--1209755884", contextInstance);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(childContext);

        Assert.assertTrue(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());

        childContext = (ContextInstance) ContextHelper
            .getChildContext("CONTEXT-774294372", contextInstance);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(childContext);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());

        childContext = (ContextInstance) ContextHelper
            .getChildContext("CONTEXT--2036736597", contextInstance);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(childContext);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isHeldJobs());

        childContext = (ContextInstance) ContextHelper
            .getChildContext("CONTEXT--129403053", contextInstance);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(childContext);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());
    }

    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    public Map<String, InternalEventDrivenJobInstance> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }

        return internalEventDrivenJobs;
    }

    private void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    private InternalEventDrivenJobInstance newInternalEventDrivenJob(String jobIdentifier, String childContextName, String jobName) {
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));
        job.setTargetResidingContextOnly(true);

        return job;
    }
}
