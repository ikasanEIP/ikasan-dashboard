package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatus;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

// todo extensive tests need to be written here
public class ContextHelperTest {

    private static Logger logger = LoggerFactory.getLogger(ContextHelperTest.class);

    ContextService contextService = new ContextService();

    ContextHelper contextHelper = new ContextHelper();

    ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Test
    public void test_context_template_token_replacement() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/-1793100514-with-tokens.json")
            , ObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_context_template_token_replacement_not_using_underscore_convention() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(false);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/-1793100514-with-tokens_no_underscore.json")
            , ObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_context_template_token_replacement_with_job_locks() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks-with-tokens.json")
            , ObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_scheduler_job_token_replacement() {
        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);

        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setJobName("jobName");
        internalEventDrivenJob.setAgentName("agentName");
        internalEventDrivenJob.setIdentifier("agentName-jobName");
        internalEventDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(internalEventDrivenJob);

        Assert.assertEquals("[[agent.name]]", internalEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", internalEventDrivenJob.getIdentifier());
        Assert.assertEquals("contextName_[[env.name]]", internalEventDrivenJob.getContextName());

        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setJobName("jobName");
        quartzScheduleDrivenJob.setAgentName("agentName");
        quartzScheduleDrivenJob.setIdentifier("agentName-jobName");
        quartzScheduleDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(quartzScheduleDrivenJob);

        Assert.assertEquals("[[agent.name]]", quartzScheduleDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", quartzScheduleDrivenJob.getIdentifier());
        Assert.assertEquals("contextName_[[env.name]]", quartzScheduleDrivenJob.getContextName());

        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setJobName("jobName");
        fileEventDrivenJob.setAgentName("agentName");
        fileEventDrivenJob.setIdentifier("agentName-jobName");
        fileEventDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(fileEventDrivenJob);

        Assert.assertEquals("[[agent.name]]", fileEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", fileEventDrivenJob.getIdentifier());
        Assert.assertEquals("contextName_[[env.name]]", fileEventDrivenJob.getContextName());

        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setJobName("jobName");
        globalEventJob.setAgentName("agentName");
        globalEventJob.setIdentifier("agentName-jobName");
        globalEventJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(globalEventJob);

        Assert.assertEquals("GLOBAL_EVENT", globalEventJob.getAgentName());
        Assert.assertEquals("GLOBAL_EVENT-jobName", globalEventJob.getIdentifier());
        Assert.assertEquals("contextName_[[env.name]]", globalEventJob.getContextName());
    }

    @Test
    public void test_scheduler_job_token_replacement_no_underscore() {
        contextHelper.setUseUnderscoreSeparatedContextNameConvention(false);

        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
        internalEventDrivenJob.setJobName("jobName");
        internalEventDrivenJob.setAgentName("agentName");
        internalEventDrivenJob.setIdentifier("agentName-jobName");
        internalEventDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(internalEventDrivenJob);

        Assert.assertEquals("[[agent.name]]", internalEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", internalEventDrivenJob.getIdentifier());
        Assert.assertEquals("[[context.name]]", internalEventDrivenJob.getContextName());

        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setJobName("jobName");
        quartzScheduleDrivenJob.setAgentName("agentName");
        quartzScheduleDrivenJob.setIdentifier("agentName-jobName");
        quartzScheduleDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(quartzScheduleDrivenJob);

        Assert.assertEquals("[[agent.name]]", quartzScheduleDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", quartzScheduleDrivenJob.getIdentifier());
        Assert.assertEquals("[[context.name]]", quartzScheduleDrivenJob.getContextName());

        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setJobName("jobName");
        fileEventDrivenJob.setAgentName("agentName");
        fileEventDrivenJob.setIdentifier("agentName-jobName");
        fileEventDrivenJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(fileEventDrivenJob);

        Assert.assertEquals("[[agent.name]]", fileEventDrivenJob.getAgentName());
        Assert.assertEquals("[[agent.name]]-jobName", fileEventDrivenJob.getIdentifier());
        Assert.assertEquals("[[context.name]]", fileEventDrivenJob.getContextName());

        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setJobName("jobName");
        globalEventJob.setAgentName("agentName");
        globalEventJob.setIdentifier("agentName-jobName");
        globalEventJob.setContextName("contextName");

        ContextHelper.addSchedulerJobReplacementTokens(globalEventJob);

        Assert.assertEquals("GLOBAL_EVENT", globalEventJob.getAgentName());
        Assert.assertEquals("GLOBAL_EVENT-jobName", globalEventJob.getIdentifier());
        Assert.assertEquals("[[context.name]]", globalEventJob.getContextName());
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

        ContextHelper.holdAllJobs(contextInstance, createInternalJobsInstancesMap(contextTemplate, true));

        AggregateContextInstanceStatus aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isHeldJobs());
    }

    @Test
    public void test_get_aggregate_context_status() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/-1793100514.json"));

        ContextHelper.enrichJobs(contextInstance);

        AggregateContextInstanceStatus aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance, contextInstance, createInternalJobsMap(contextTemplate));

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());

        ContextHelper.setJobStatusAll(contextInstance, this.createInternalJobsInstancesMap(contextTemplate, false), InstanceStatus.SKIPPED);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance, contextInstance, createInternalJobsMap(contextTemplate));

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());

        ContextHelper.setJobStatusAll(contextInstance, this.createInternalJobsInstancesMap(contextTemplate, false), InstanceStatus.DISABLED);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance, contextInstance, createInternalJobsMap(contextTemplate));

        Assert.assertTrue(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());

        ContextHelper.setJobStatusAll(contextInstance, this.createInternalJobsInstancesMap(contextTemplate, false), InstanceStatus.ON_HOLD);

        aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance, contextInstance, createInternalJobsMap(contextTemplate));

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertTrue(aggregateContextInstanceStatus.isHeldJobs());
    }

    @Test
    public void test_hold_all_jobs_for_context_instance_when_job_in_error() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextInstance contextInstance = this.contextService
            .getContextInstance(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));

        contextInstance.getScheduledJobs().forEach(schedulerJobInstance -> schedulerJobInstance.setStatus(InstanceStatus.ERROR));

        ContextHelper.enrichJobs(contextInstance);

        ContextHelper.holdAllJobs(contextInstance, createInternalJobsInstancesMap(contextTemplate, true));

        AggregateContextInstanceStatus aggregateContextInstanceStatus
            = ContextHelper.getAggregateContextInstanceStatus(contextInstance);

        Assert.assertFalse(aggregateContextInstanceStatus.isDisabledJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isSkippedJobs());
        Assert.assertFalse(aggregateContextInstanceStatus.isHeldJobs());
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
    public void test_status_helper() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context-status-plan.json"));
        ContextInstance contextInstance = this.contextService.getContextInstance(loadDataFile("/data/context-status-instance.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = createInternalJobsInstancesMap(context, false);

        // Set some jobs as targetResidingContextOnly = true on the template
        internalEventDrivenJobs.get("agent-name1-JOB_TEST_TRUE-some-test-name1").setTargetResidingContextOnly(true);
        internalEventDrivenJobs.get("agent-name1-JOB_TEST_TRUE-JOB_TEST_TRUE").setTargetResidingContextOnly(true);

        ContextJobInstanceStatus contextJobInstanceStatus = ContextHelper.getContextJobInstanceStatus(contextInstance, internalEventDrivenJobs);

        contextJobInstanceStatus.getJobDetails().forEach(status ->{

            // Check that if targetResidingContextOnly = true then we create a separate record for it
            if (status.getJobName().equals("JOB_TEST_TRUE")) {
                if (status.getChildContextName().contains("JOB_TEST_TRUE")) {
                    Assert.assertTrue(true);
                } else if ((status.getChildContextName().contains("some-test-name1"))) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }

            // Check that if targetResidingContextOnly = false then we create 1 record
            if (status.getJobName().equals("POP_ABCDE_MD")) {
                Assert.assertEquals(2, status.getChildContextName().size());
                Assert.assertTrue(status.getChildContextName().contains("ABCDE_POP_HOPE5_DECOMP"));
                Assert.assertTrue(status.getChildContextName().contains("ABCDE_POP_MD"));
            }
        });
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

    @Test
    public void test_get_unique_context_parameters_from_job_instances() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstanceMap = createInternalJobsInstancesMap(contextTemplate, true);

        List<ContextParameter> contextParameterInstances1 = new ArrayList<>();
        contextParameterInstances1.add(newContextParameterInstance("name1", "value1"));
        contextParameterInstances1.add(newContextParameterInstance("name2", "value2"));
        contextParameterInstances1.add(newContextParameterInstance("name3", "value3"));
        contextParameterInstances1.add(newContextParameterInstance("name4", "value4"));
        contextParameterInstances1.add(newContextParameterInstance("name5", "value5"));

        List<ContextParameter> contextParameterInstances2 = new ArrayList<>();
        contextParameterInstances2.add(newContextParameterInstance("name1", "value1"));
        contextParameterInstances2.add(newContextParameterInstance("name4", "value4"));
        contextParameterInstances2.add(newContextParameterInstance("name5", "value5"));

        int i=0;
        for(InternalEventDrivenJobInstance instance: internalEventDrivenJobInstanceMap.values()) {
            if(i%2 == 0) {
                instance.setContextParameters(contextParameterInstances1);
            }
            else {
                instance.setContextParameters(contextParameterInstances2);
            }
        }

        List<ContextParameterInstance> contextParameterInstances
            = ContextHelper.getUniqueContextParameterInstancesFromJobInstances(internalEventDrivenJobInstanceMap);

        Assert.assertTrue(contextParameterInstances.size() == 5);

    }

    @Test
    public void test_get_unique_context_parameters_from_jobs() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = createInternalJobsMap(contextTemplate);

        List<ContextParameter> contextParameterInstances1 = new ArrayList<>();
        contextParameterInstances1.add(newContextParameterInstance("name1", "value1"));
        contextParameterInstances1.add(newContextParameterInstance("name2", "value2"));
        contextParameterInstances1.add(newContextParameterInstance("name3", "value3"));
        contextParameterInstances1.add(newContextParameterInstance("name4", "value4"));
        contextParameterInstances1.add(newContextParameterInstance("name5", "value5"));

        List<ContextParameter> contextParameterInstances2 = new ArrayList<>();
        contextParameterInstances2.add(newContextParameterInstance("name1", "value1"));
        contextParameterInstances2.add(newContextParameterInstance("name4", "value4"));
        contextParameterInstances2.add(newContextParameterInstance("name5", "value5"));

        int i=0;
        for(InternalEventDrivenJob instance: internalEventDrivenJobMap.values()) {
            if(i%2 == 0) {
                instance.setContextParameters(contextParameterInstances1);
            }
            else {
                instance.setContextParameters(contextParameterInstances2);
            }
        }

        List<ContextParameterInstance> contextParameterInstances
            = ContextHelper.getUniqueContextParameterInstancesFromJobs(internalEventDrivenJobMap);

        Assert.assertTrue(contextParameterInstances.size() == 5);

    }

    @Test
    public void test_get_context_start_jobs_from_context() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);

        Map<String, ContextStartJob> contextStartJobMap = ContextHelper.getContextStartJobsMapFromContext(context);

        Assert.assertNotNull(contextStartJobMap);
        Assert.assertEquals(12, contextStartJobMap.size());
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 6 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 6 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 2 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 2 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 3 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 3 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_AM_2 Step 1 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_AM_2 Step 1 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 1 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 1 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 7 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 7 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 5 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 5 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_AM_1 Step 1 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_AM_1 Step 1 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 8 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 8 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 1 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 1 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT1 Step 1 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT1 Step 1 Start"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 2 Start"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 2 Start"));
    }

    @Test
    public void test_get_context_start_job_instances_from_context() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);

        Map<String, ContextStartJobInstance> contextStartJobMap
            = ContextHelper.getContextStartJobInstancesMapFromContextForInstance(context, contextInstance);

        Assert.assertNotNull(contextStartJobMap);
        Assert.assertEquals(12, contextStartJobMap.size());
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 6 Start-TEST_IK_GLOB Step 6"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 6 Start-TEST_IK_GLOB Step 6"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 2 Start-TEST_IK_LOCK_1 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 2 Start-TEST_IK_LOCK_1 Step 2"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 3 Start-TEST_IK_GLOB Step 3"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 3 Start-TEST_IK_GLOB Step 3"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_AM_2 Step 1 Start-TEST_IK_AM_2 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_AM_2 Step 1 Start-TEST_IK_AM_2 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 1 Start-TEST_IK_EVENT2 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 1 Start-TEST_IK_EVENT2 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 7 Start-TEST_IK_GLOB Step 7"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 7 Start-TEST_IK_GLOB Step 7"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 5 Start-TEST_IK_GLOB Step 5"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 5 Start-TEST_IK_GLOB Step 5"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_AM_1 Step 1 Start-TEST_IK_AM_1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_AM_1 Step 1 Start-TEST_IK_AM_1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_GLOB Step 8 Start-TEST_IK_GLOB Step 8"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_GLOB Step 8 Start-TEST_IK_GLOB Step 8"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 1 Start-TEST_IK_LOCK_1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_LOCK_1 Step 1 Start-TEST_IK_LOCK_1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT1 Step 1 Start-TEST_IK_EVENT1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT1 Step 1 Start-TEST_IK_EVENT1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 2 Start-TEST_IK_EVENT2 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_START_JOB-TEST_IK_EVENT2 Step 2 Start-TEST_IK_EVENT2 Step 2"));
    }

    @Test
    public void test_get_context_terminal_jobs_from_context() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);

        Map<String, ContextTerminalJob> contextStartJobMap = ContextHelper.getContextTerminalJobsMapFromContext(context);

        Assert.assertNotNull(contextStartJobMap);
        Assert.assertEquals(13, contextStartJobMap.size());
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 8 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 8 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 1 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 3 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 3 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal"));
    }

    @Test
    public void test_get_context_terminal_job_instances_from_context() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);
        ContextInstance contextInstance = this.contextService.getContextInstance(contextJson);

        Map<String, ContextTerminalJobInstance> contextStartJobMap 
            = ContextHelper.getContextTerminalJobInstancesMapFromContextForInstance(context, contextInstance);

        Assert.assertNotNull(contextStartJobMap);
        Assert.assertEquals(26, contextStartJobMap.size());
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_EVENT1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_EVENT1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal-TEST_IK_LOCK_1 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal-TEST_IK_LOCK_1 Step 2"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal-TEST_IK_LOCK_1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 1 Terminal-TEST_IK_LOCK_1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_EVENT2 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_EVENT2 Step 2"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal-TEST_IK_GLOB Step 5"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal-TEST_IK_GLOB Step 5"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal-TEST_IK_GLOB Step 6"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 5 Terminal-TEST_IK_GLOB Step 6"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal-TEST_IK_EVENT2 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal-TEST_IK_EVENT2 Step 2"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal-TEST_IK_EVENT2 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 1 Terminal-TEST_IK_EVENT2 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal-TEST_IK_EVENT1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal-TEST_IK_EVENT1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal-TEST_IK_GLOB Step 7"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal-TEST_IK_GLOB Step 7"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal-TEST_IK_GLOB Step 8"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 7 Terminal-TEST_IK_GLOB Step 8"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal-TEST_IK_AM_2 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal-TEST_IK_AM_2 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal-TEST_IK_AM_1 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_1 Step 1 Terminal-TEST_IK_AM_1 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 8 Terminal-TEST_IK_GLOB Step 8"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 8 Terminal-TEST_IK_GLOB Step 8"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 3 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 3 Terminal-TEST_IK_GLOB Step 3"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal-TEST_IK_LOCK_1 Step 2"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_LOCK_1 Step 2 Terminal-TEST_IK_LOCK_1 Step 2"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal-TEST_IK_AM_2 Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_AM_2 Step 1 Terminal-TEST_IK_AM_2 Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 1 Terminal-TEST_IK_GLOB Step 1"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 1 Terminal-TEST_IK_GLOB Step 1"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_GLOB Step 5"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT2 Step 2 Terminal-TEST_IK_GLOB Step 5"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal-TEST_IK_GLOB Step 7"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal-TEST_IK_GLOB Step 7"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal-TEST_IK_GLOB Step 6"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_GLOB Step 6 Terminal-TEST_IK_GLOB Step 6"));
        Assert.assertTrue(contextStartJobMap.containsKey("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal-TEST_IK_GLOB Step 5"));
        Assert.assertNotNull(contextStartJobMap.get("CONTEXT_TERMINAL_JOB-TEST_IK_EVENT1 Step 1 Terminal-TEST_IK_GLOB Step 5"));
    }

    @Test
    public void test_enrich_child_context_names_on_jobs_success() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);

        List<InternalEventDrivenJob> internalEventDrivenJobs = this.loadInternalEventDrivenJobInstanceMap
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal",
                "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal");

        List<ContextTerminalJob> contextTerminalJobs = loadContextTerminalJobInstanceMap
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/terminal",
            "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/terminal");

        List<ContextStartJob> contextStartJobs = loadContextStartJobInstanceMap
            ( "./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start",
            "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start");

        List<SchedulerJob> allJobs = new ArrayList<>();
        allJobs.addAll(internalEventDrivenJobs);
        allJobs.addAll(contextStartJobs);
        allJobs.addAll(contextTerminalJobs);

        // Assert all child context names are empty.
        allJobs.forEach(schedulerJob -> Assert.assertTrue(schedulerJob.getChildContextNames().isEmpty()));

        ContextHelper.populateChildContextNamesOnSchedulerJobs(context, allJobs);

        Assert.assertNotNull(allJobs);
        Assert.assertEquals(49, allJobs.size());

        // Assert all child context names have been populated.
        allJobs.forEach(schedulerJob -> Assert.assertFalse(schedulerJob.getChildContextNames().isEmpty()));
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 1", "TEST_IK_AM_1 Step 1"), allJobs.get(0).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1", "TEST_IK_EVENT2 Step 1"), allJobs.get(1).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 7"), allJobs.get(2).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 5"), allJobs.get(3).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 8"), allJobs.get(4).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1", "TEST_IK_EVENT2 Step 2"), allJobs.get(5).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 8"), allJobs.get(6).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 2"), allJobs.get(7).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_2 Step 1"), allJobs.get(8).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 2"), allJobs.get(9).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 2"), allJobs.get(10).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_2 Step 1"), allJobs.get(11).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 3", "TEST_IK_LOCK_1 Step 1"), allJobs.get(12).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 2"), allJobs.get(13).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_1 Step 1"), allJobs.get(14).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 8"), allJobs.get(15).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_1 Step 1"), allJobs.get(16).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 1"), allJobs.get(17).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 6"), allJobs.get(18).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_2 Step 1"), allJobs.get(19).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT2 Step 1"), allJobs.get(20).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_1 Step 1"), allJobs.get(21).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 1"), allJobs.get(22).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1"), allJobs.get(23).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1"), allJobs.get(24).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_2 Step 1"), allJobs.get(25).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 2"), allJobs.get(26).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 6"), allJobs.get(27).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 3"), allJobs.get(28).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT2 Step 1"), allJobs.get(29).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_1 Step 1"), allJobs.get(30).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT2 Step 2"), allJobs.get(31).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 8"), allJobs.get(32).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 5"), allJobs.get(33).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 1"), allJobs.get(34).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 7"), allJobs.get(35).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1", "TEST_IK_EVENT2 Step 2", "TEST_IK_GLOB Step 5"), allJobs.get(36).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT2 Step 1", "TEST_IK_EVENT2 Step 2"), allJobs.get(37).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 8"), allJobs.get(38).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_2 Step 1", "TEST_IK_GLOB Step 3"), allJobs.get(39).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 6", "TEST_IK_GLOB Step 7"), allJobs.get(40).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 1", "TEST_IK_AM_1 Step 1"), allJobs.get(41).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 5", "TEST_IK_GLOB Step 6"), allJobs.get(42).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 3", "TEST_IK_LOCK_1 Step 2"), allJobs.get(43).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 7", "TEST_IK_GLOB Step 8"), allJobs.get(44).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_LOCK_1 Step 1", "TEST_IK_LOCK_1 Step 2"), allJobs.get(45).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_GLOB Step 3", "TEST_IK_EVENT1 Step 1"), allJobs.get(46).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_AM_1 Step 1", "TEST_IK_AM_2 Step 1"), allJobs.get(47).getChildContextNames());
        Assert.assertEquals(List.of("TEST_IK_EVENT1 Step 1", "TEST_IK_GLOB Step 5"), allJobs.get(48).getChildContextNames());
    }

    /**
     * Loads the content of a data file.
     *
     * @param fileName the name of the file to load
     * @return the content of the file as a string
     * @throws IOException if an I/O error occurs while loading the file
     */
    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    /**
     * Loads a data file as an input stream.
     *
     * @param fileName the name of the file to load
     * @return an InputStream representing the loaded file
     * @throws IOException if an error occurs while reading the file
     */
    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    /**
     * Creates a map of internal event-driven job instances based on the given context template.
     *
     * @param contextTemplate           the context template
     * @param targetResidingContextOnly flag to indicate whether to include only the target residing context
     * @return a map of internal event-driven job instances
     */
    public Map<String, InternalEventDrivenJobInstance> createInternalJobsInstancesMap(ContextTemplate contextTemplate, boolean targetResidingContextOnly) {
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName(), targetResidingContextOnly)));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobInstances(template, internalEventDrivenJobs, targetResidingContextOnly));
        }

        return internalEventDrivenJobs;
    }

    /**
     * Creates a map of internal event-driven jobs based on the provided context template.
     *
     * @param contextTemplate the context template to create the internal event-driven jobs map from
     * @return a map of internal event-driven jobs, where the key is a combination of the job identifier and the context name, and the value is the internal event-driven job instance
     *
     */
    public Map<String, InternalEventDrivenJob> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName(), true)));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }

        return internalEventDrivenJobs;
    }

    /**
     * Adds internal job instances to the given map based on the provided context template and target residency flag.
     *
     * @param contextTemplate The context template to process.
     * @param internalEventDrivenJobs The map to store the internal job instances.
     * @param targetResidingContextOnly Flag indicating whether to consider only target residing contexts.
     */
    private void addInternalJobInstances(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs, boolean targetResidingContextOnly) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName(), targetResidingContextOnly)));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobInstances(template, internalEventDrivenJobs, targetResidingContextOnly));
        }
    }

    /**
     * Creates a new instance of InternalEventDrivenJobInstance with the given parameters.
     *
     * @param jobIdentifier            the identifier of the job
     * @param childContextName         the name of the child context
     * @param jobName                  the name of the job
     * @param targetResidingContextOnly determines whether the job should only target the residing context
     * @return a new instance of InternalEventDrivenJobInstance
     */
    private InternalEventDrivenJobInstance newInternalEventDrivenJobInstance(String jobIdentifier, String childContextName, String jobName, boolean targetResidingContextOnly) {
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));
        job.setTargetResidingContextOnly(targetResidingContextOnly);

        return job;
    }

    /**
     * Adds internal event-driven jobs to the given map based on the provided context template.
     *
     * @param contextTemplate the context template
     * @param internalEventDrivenJobs the map to add the internal event-driven jobs to
     */
    private void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJob> internalEventDrivenJobs) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    /**
     * Creates a new instance of InternalEventDrivenJob with the given parameters.
     *
     * @param jobIdentifier    the identifier of the job
     * @param childContextName the name of the child context
     * @param jobName          the name of the job
     * @return a new instance of InternalEventDrivenJob with the specified parameters
     */
    private InternalEventDrivenJob newInternalEventDrivenJob(String jobIdentifier, String childContextName, String jobName) {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextNames(List.of(childContextName));
        job.setTargetResidingContextOnly(true);

        return job;
    }

    /**
     * Creates a new instance of ContextParameterInstance with the given name and value.
     *
     * @param name  the name of the context parameter
     * @param value the value of the context parameter
     * @return a new instance of ContextParameterInstance
     */
    private ContextParameterInstance newContextParameterInstance(String name, String value) {
        ContextParameterInstance instance = new ContextParameterInstanceImpl();
        instance.setName(name);
        instance.setValue(value);

        return instance;
    }

    /**
     * Loads a list of ContextTerminalJob instances from the specified directory.
     *
     * @param directory the directory where the job files are located
     * @param jobsBase the base directory for the job files
     * @return a list of ContextTerminalJob instances loaded from the directory
     * @throws IOException if an error occurs while reading the files
     */
    public List<ContextTerminalJob> loadContextTerminalJobInstanceMap(String directory, String jobsBase) throws IOException {
        return Files.list(Path.of(directory)).map(path -> {
            ContextTerminalJob contextTerminalJobInstance = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                contextTerminalJobInstance = objectMapper.readValue(jobJson, ContextTerminalJobImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return contextTerminalJobInstance;
        }).collect(Collectors.toList());
    }

    /**
     * Loads a list of ContextStartJob instances from the specified directory.
     *
     * @param directory the directory where the job files are located
     * @param jobsBase the base directory for the job files
     * @return a list of ContextStartJob instances loaded from the directory
     * @throws IOException if an error occurs while reading the files
     */
    public List<ContextStartJob> loadContextStartJobInstanceMap(String directory, String jobsBase) throws IOException {
        return Files.list(Path.of(directory)).map(path -> {
            ContextStartJob contextStartJobInstance = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                contextStartJobInstance = objectMapper.readValue(jobJson, ContextStartJobImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return contextStartJobInstance;
        }).collect(Collectors.toList());
    }

    /**
     * Loads a list of InternalEventDrivenJob instances from the specified directory.
     *
     * @param directory the directory where the job files are located
     * @param jobsBase the base directory for the job files
     * @return a list of InternalEventDrivenJob instances loaded from the directory
     * @throws IOException if an error occurs while reading the files
     */
    public List<InternalEventDrivenJob> loadInternalEventDrivenJobInstanceMap(String directory, String jobsBase) throws IOException {
        return Files.list(Path.of(directory)).map(path -> {
            InternalEventDrivenJob internalEventDrivenJob = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                internalEventDrivenJob = objectMapper.readValue(jobJson, InternalEventDrivenJobImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return internalEventDrivenJob;
        }).collect(Collectors.toList());
    }
}
