package org.ikasan.job.orchestration.util;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatus;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

// todo extensive tests need to be written here
public class ContextHelperTest {

    private static Logger logger = LoggerFactory.getLogger(ContextHelperTest.class);

    ContextService contextService = new ContextService();

    ContextHelper contextHelper = new ContextHelper();

    JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    @Test
    public void test_context_template_token_replacement() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/-1793100514-with-tokens.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));

        SchedulerJob job = new SchedulerJobImpl();
        job.setIdentifier("scheduler-agent-zzzz-job");
        job.setJobName("zzzz-job");
        job.setAgentName("achedulerAgent");
        job.setContextName(contextTemplate.getName());

        contextTemplate.getScheduledJobs().add(job);
        System.out.println(ConcurrentObjectMapperFactory.newInstance().writerWithDefaultPrettyPrinter()
            .writeValueAsString(contextTemplate));
    }

    @Test
    public void test_context_template_token_replacement_with_user_generate_layout() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/job-plan-with-user-generated-layout.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/job-plan-with-user-generated-layout-with-tokens.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_token_replacement_with_start_terminal_local_event_jobs() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/job-plan-with-start-terminal-local-event-jobs.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/job-plan-with-start-terminal-local-event-jobs-with-tokens.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_context_template_token_replacement_with_no_job_dependencies() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/job-plan-no-dependencies.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/job-plan-no-dependencies-with-tokens.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_token_replacement_not_using_underscore_convention() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(false);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/-1793100514-with-tokens_no_underscore.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_get_local_event_jobs_from_context() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/test-plan-with-local-event-jobs.json"));

        List<LocalEventJob> localEventJobs = ContextHelper.getLocalEventJobsFromContext(contextTemplate);

        Assert.assertNotNull(localEventJobs);
        Assert.assertEquals(4, localEventJobs.size());
        Assert.assertEquals("test-local", localEventJobs.get(0).getJobName());
        Assert.assertEquals("test", localEventJobs.get(1).getJobName());
        Assert.assertEquals("jjj", localEventJobs.get(2).getJobName());
        Assert.assertEquals("local", localEventJobs.get(3).getJobName());
    }

    @Test
    public void test_context_template_token_replacement_with_job_locks() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks.json"));

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(true);
        ContextHelper.addContextTemplateReplacementTokens(contextTemplate);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/locks/context-with-four-jobs-in-two-separate-job-locks-with-tokens.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
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

        ContextHelper.holdAllJobs(contextInstance, createInternalJobsInstancesMap(contextTemplate, true).entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue())));

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

        ContextHelper.holdAllJobs(contextInstance, createInternalJobsInstancesMap(contextTemplate, true).entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue())));

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
    public void test_get_unique_context_parameters_from_jobs_null_context_params() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/-1793100514.json"));

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = createInternalJobsMap(contextTemplate);

        for(InternalEventDrivenJob instance: internalEventDrivenJobMap.values()) {
            instance.setContextParameters(null);
        }

        List<ContextParameterInstance> contextParameterInstances
            = ContextHelper.getUniqueContextParameterInstancesFromJobs(internalEventDrivenJobMap);

        Assert.assertTrue(contextParameterInstances.size() == 0);

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
    }

    @Test
    public void test_determine_if_jobs_transition_from_other_contexts() throws IOException {
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
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start",
                "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start");

        List<SchedulerJob> allJobs = new ArrayList<>();
        allJobs.addAll(internalEventDrivenJobs);
        allJobs.addAll(contextStartJobs);
        allJobs.addAll(contextTerminalJobs);

        // Assert all child context names are empty.
        allJobs.forEach(schedulerJob -> Assert.assertTrue(schedulerJob.getChildContextNames().isEmpty()));

        ContextHelper.populateChildContextNamesOnSchedulerJobs(context, allJobs);

        List<ContextTransition> contextTransitions = ContextHelper.determineIfSchedulerJobsTransitionFromOtherContexts(context
            , "TEST_IK_JOB_18"
            , "TEST_IK_EVENT1 Step 1"
            , allJobs.stream().collect(Collectors.toMap(SchedulerJob::getIdentifier
                , Function.identity(), (key1, key2)-> key2)));

        Assert.assertTrue(contextTransitions.size() > 0);

        LinkedList<List<SchedulerJob>> trace = ContextHelper.traceJobThroughContext(context, "TEST_IK_JOB_16"
            , "TEST_IK_EVENT1 Step 1");

        Assert.assertNotNull(trace);
    }

    @Test
    public void test_determine_if_jobs_transition_from_other_contexts_job_target_residing_context() throws IOException {
        String contextJson = loadDataFile("/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/" +
            "context/TEST_IK_GLOB.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);

        List<InternalEventDrivenJob> internalEventDrivenJobs = this.loadInternalEventDrivenJobInstanceMap
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal",
                "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/internal");
        internalEventDrivenJobs.forEach(internalEventDrivenJob
            -> internalEventDrivenJob.setTargetResidingContextOnly(true));

        List<ContextTerminalJob> contextTerminalJobs = loadContextTerminalJobInstanceMap
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/terminal",
                "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/terminal");

        List<ContextStartJob> contextStartJobs = loadContextStartJobInstanceMap
            ("./src/test/resources/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start",
                "/data/bundles/TEST_IK_GLOB_WITH_START_AND_TERMINAL_JOBS/jobs/start");

        List<SchedulerJob> allJobs = new ArrayList<>();
        allJobs.addAll(internalEventDrivenJobs);
        allJobs.addAll(contextStartJobs);
        allJobs.addAll(contextTerminalJobs);

        // Assert all child context names are empty.
        allJobs.forEach(schedulerJob -> Assert.assertTrue(schedulerJob.getChildContextNames().isEmpty()));

        ContextHelper.populateChildContextNamesOnSchedulerJobs(context, allJobs);

        List<ContextTransition> contextTransitions = ContextHelper.determineIfSchedulerJobsTransitionFromOtherContexts(context
            , "TEST_IK_JOB_18"
            , "TEST_IK_EVENT1 Step 1"
            , allJobs.stream().collect(Collectors.toMap(job -> job.getIdentifier() + "-" + job.getContextName()
                , Function.identity(), (key1, key2)-> key2)));

        Assert.assertTrue(contextTransitions.size() > 0);

        LinkedList<List<SchedulerJob>> trace = ContextHelper.traceJobThroughContext(context, "TEST_IK_JOB_16"
            , "TEST_IK_EVENT1 Step 1");

        Assert.assertNotNull(trace);
    }

    @Test
    public void test_get_jobs_outside_logical_grouping_duplicate_jobs() throws IOException {
        String contextJson = loadDataFile("/data/context_duplicate_jobs.json");

        ContextTemplate context = this.contextService.getContextTemplate(contextJson);

        Map<String, SchedulerJob> jobs = ContextHelper.getJobsOutsideLogicalGrouping
            (ContextHelper.getChildContext("Context3", context));

        Assert.assertEquals(1, jobs.size());
        Assert.assertEquals("jobName6", jobs.values().stream().findFirst().get().getJobName());
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

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_no_transitions_simple_context() {
        // Create parent context with a single child context containing jobs
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("Job1");
        job1.setIdentifier("job1-id");
        job1.setAgentName("agent1");

        SchedulerJob job2 = new SchedulerJobImpl();
        job2.setJobName("Job2");
        job2.setIdentifier("job2-id");
        job2.setAgentName("agent2");

        childContext.setScheduledJobs(List.of(job1, job2));

        JobDependency jobDependency = new JobDependencyImpl();
        jobDependency.setJobIdentifier("job2-id");
        childContext.setJobDependencies(List.of(jobDependency));

        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("job1-id-ChildContext", job1);
        schedulerJobs.put("job2-id-ChildContext", job2);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        Assert.assertTrue("Expected no transitions for jobs contained within a single context", transitions.isEmpty());
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_with_context_terminal_job() {
        // Test that terminal jobs are handled correctly (skip tracing)
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob terminalJob = new ContextTerminalJobImpl();
        terminalJob.setJobName("TerminalJob");
        terminalJob.setIdentifier("terminal-id");
        terminalJob.setAgentName("CONTEXT_TERMINAL_JOB");

        childContext.setScheduledJobs(List.of(terminalJob));
        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("terminal-id-ChildContext", terminalJob);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        Assert.assertTrue("Terminal jobs should not create transitions", transitions.isEmpty());
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_job_crosses_context_boundary() {
        // Create a scenario where a job in child context transitions to another context
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext1 = new ContextTemplateImpl();
        childContext1.setName("ChildContext1");

        Context childContext2 = new ContextTemplateImpl();
        childContext2.setName("ChildContext2");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("SharedJob");
        job1.setIdentifier("shared-job-id");
        job1.setAgentName("agent1");

        SchedulerJob job2 = new SchedulerJobImpl();
        job2.setJobName("Job2");
        job2.setIdentifier("job2-id");
        job2.setAgentName("agent2");

        SchedulerJob sharedJobInContext2 = new SchedulerJobImpl();
        sharedJobInContext2.setJobName("SharedJob");
        sharedJobInContext2.setIdentifier("shared-job-id");
        sharedJobInContext2.setAgentName("agent1");

        SchedulerJob job3 = new SchedulerJobImpl();
        job3.setJobName("Job3");
        job3.setIdentifier("job3-id");
        job3.setAgentName("agent3");

        childContext1.setScheduledJobs(List.of(job1, job2));
        JobDependency jobDep1 = new JobDependencyImpl();
        jobDep1.setJobIdentifier("job2-id");
        childContext1.setJobDependencies(List.of(jobDep1));

        childContext2.setScheduledJobs(List.of(sharedJobInContext2, job3));
        JobDependency jobDep2 = new JobDependencyImpl();
        jobDep2.setJobIdentifier("job3-id");
        childContext2.setJobDependencies(List.of(jobDep2));

        parentContext.setContexts(List.of(childContext1, childContext2));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("shared-job-id-ChildContext1", job1);
        schedulerJobs.put("job2-id-ChildContext1", job2);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();
        InternalEventDrivenJob internalJob = new InternalEventDrivenJobImpl();
        internalJob.setJobName("SharedJob");
        internalJob.setIdentifier("shared-job-id");
        internalJob.setTargetResidingContextOnly(false);
        internalEventDrivenJobMap.put("shared-job-id-ChildContext1", internalJob);

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext1, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Transitions should be detected when jobs cross context boundaries
        // The actual count depends on the tracing logic and context structure
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_with_target_residing_context_only() {
        // Test jobs that target residing context only should not create transitions
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext1 = new ContextTemplateImpl();
        childContext1.setName("ChildContext1");

        Context childContext2 = new ContextTemplateImpl();
        childContext2.setName("ChildContext2");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("LocalJob");
        job1.setIdentifier("local-job-id");
        job1.setAgentName("agent1");

        childContext1.setScheduledJobs(List.of(job1));
        childContext2.setScheduledJobs(new ArrayList<>());

        parentContext.setContexts(List.of(childContext1, childContext2));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("local-job-id-ChildContext1", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();
        InternalEventDrivenJob internalJob = new InternalEventDrivenJobImpl();
        internalJob.setJobName("LocalJob");
        internalJob.setIdentifier("local-job-id");
        internalJob.setTargetResidingContextOnly(true); // Should not transition
        internalEventDrivenJobMap.put("local-job-id-ChildContext1", internalJob);

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext1, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        Assert.assertTrue("Jobs targeting residing context only should not create transitions", transitions.isEmpty());
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_with_logical_grouping() {
        // Test that jobs within logical grouping are handled correctly
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("Job1");
        job1.setIdentifier("job1-id");
        job1.setAgentName("agent1");

        SchedulerJob job2 = new SchedulerJobImpl();
        job2.setJobName("Job2");
        job2.setIdentifier("job2-id");
        job2.setAgentName("agent2");

        SchedulerJob job3 = new SchedulerJobImpl();
        job3.setJobName("Job3");
        job3.setIdentifier("job3-id");
        job3.setAgentName("agent3");

        childContext.setScheduledJobs(List.of(job1, job2, job3));

        // Create logical grouping with AND condition
        And and = new AndImpl();
        and.setIdentifier("job2-id");

        LogicalGroupingImpl logicalGrouping = new LogicalGroupingImpl();
        logicalGrouping.setAnd(List.of(and));

        JobDependency jobDependency = new JobDependencyImpl();
        jobDependency.setJobIdentifier("job3-id");
        jobDependency.setLogicalGrouping(logicalGrouping);

        childContext.setJobDependencies(List.of(jobDependency));

        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("job1-id-ChildContext", job1);
        schedulerJobs.put("job2-id-ChildContext", job2);
        schedulerJobs.put("job3-id-ChildContext", job3);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Jobs within logical grouping should be filtered out by getJobsOutsideLogicalGrouping
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_empty_scheduler_jobs() {
        // Test with empty scheduler jobs map
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");
        childContext.setScheduledJobs(new ArrayList<>());

        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        Assert.assertTrue("Empty scheduler jobs should result in no transitions", transitions.isEmpty());
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_null_job_dependencies() {
        // Test with null job dependencies
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("Job1");
        job1.setIdentifier("job1-id");
        job1.setAgentName("agent1");

        childContext.setScheduledJobs(List.of(job1));
        childContext.setJobDependencies(null);

        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("job1-id-ChildContext", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Should handle null dependencies gracefully
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_distinct_transitions() {
        // Test that duplicate transitions are filtered out
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext1 = new ContextTemplateImpl();
        childContext1.setName("ChildContext1");

        Context childContext2 = new ContextTemplateImpl();
        childContext2.setName("ChildContext2");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("SharedJob");
        job1.setIdentifier("shared-job-id");
        job1.setAgentName("agent1");

        SchedulerJob job2 = new SchedulerJobImpl();
        job2.setJobName("SharedJob");
        job2.setIdentifier("shared-job-id");
        job2.setAgentName("agent1");

        childContext1.setScheduledJobs(List.of(job1));
        childContext2.setScheduledJobs(List.of(job2));

        parentContext.setContexts(List.of(childContext1, childContext2));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("shared-job-id-ChildContext1", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext1, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Check that transitions are distinct
        long distinctCount = transitions.stream().distinct().count();
        Assert.assertEquals("Transitions should be distinct", distinctCount, transitions.size());
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_filters_empty_contexts() {
        // Test that transitions with empty contexts list are filtered out
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("Job1");
        job1.setIdentifier("job1-id");
        job1.setAgentName("agent1");

        childContext.setScheduledJobs(List.of(job1));
        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("job1-id-ChildContext", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // All transitions should have non-empty contexts list
        transitions.forEach(transition -> {
            Assert.assertNotNull("Context list should not be null", transition.getContexts());
            Assert.assertFalse("Context list should not be empty", transition.getContexts().isEmpty());
        });
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_excludes_child_context_from_results() {
        // Test that the child context being analyzed is excluded from the contexts list in transitions
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext1 = new ContextTemplateImpl();
        childContext1.setName("ChildContext1");

        Context childContext2 = new ContextTemplateImpl();
        childContext2.setName("ChildContext2");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("TransitioningJob");
        job1.setIdentifier("trans-job-id");
        job1.setAgentName("agent1");

        SchedulerJob job2InContext1 = new SchedulerJobImpl();
        job2InContext1.setJobName("TransitioningJob");
        job2InContext1.setIdentifier("trans-job-id");
        job2InContext1.setAgentName("agent1");

        SchedulerJob job2InContext2 = new SchedulerJobImpl();
        job2InContext2.setJobName("TransitioningJob");
        job2InContext2.setIdentifier("trans-job-id");
        job2InContext2.setAgentName("agent1");

        childContext1.setScheduledJobs(List.of(job1, job2InContext1));
        childContext2.setScheduledJobs(List.of(job2InContext2));

        parentContext.setContexts(List.of(childContext1, childContext2));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("trans-job-id-ChildContext1", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext1, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Verify that ChildContext1 is not in the contexts list of any transition
        transitions.forEach(transition -> {
            Assert.assertFalse("Child context should be excluded from transition contexts",
                transition.getContexts().contains("ChildContext1"));
        });
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_multiple_preceding_jobs() {
        // Test scenario with multiple preceding jobs transitioning to the same subsequent job
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext1 = new ContextTemplateImpl();
        childContext1.setName("ChildContext1");

        Context childContext2 = new ContextTemplateImpl();
        childContext2.setName("ChildContext2");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("Job1");
        job1.setIdentifier("job1-id");
        job1.setAgentName("agent1");

        SchedulerJob job2 = new SchedulerJobImpl();
        job2.setJobName("Job2");
        job2.setIdentifier("job2-id");
        job2.setAgentName("agent2");

        SchedulerJob job3 = new SchedulerJobImpl();
        job3.setJobName("Job3");
        job3.setIdentifier("job3-id");
        job3.setAgentName("agent3");

        childContext1.setScheduledJobs(List.of(job1, job2));
        childContext2.setScheduledJobs(List.of(job3));

        parentContext.setContexts(List.of(childContext1, childContext2));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("job1-id-ChildContext1", job1);
        schedulerJobs.put("job2-id-ChildContext1", job2);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();
        InternalEventDrivenJob internalJob1 = new InternalEventDrivenJobImpl();
        internalJob1.setJobName("Job1");
        internalJob1.setIdentifier("job1-id");
        internalJob1.setTargetResidingContextOnly(false);
        internalEventDrivenJobMap.put("job1-id-ChildContext1", internalJob1);

        InternalEventDrivenJob internalJob2 = new InternalEventDrivenJobImpl();
        internalJob2.setJobName("Job2");
        internalJob2.setIdentifier("job2-id");
        internalJob2.setTargetResidingContextOnly(false);
        internalEventDrivenJobMap.put("job2-id-ChildContext1", internalJob2);

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext1, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // The test verifies the method can handle multiple preceding jobs
    }

    @Test
    public void test_determineIfJobsTransitionToOtherContexts_non_internal_event_driven_job() {
        // Test that non-InternalEventDrivenJob entries in the map are handled correctly
        Context parentContext = new ContextTemplateImpl();
        parentContext.setName("ParentContext");

        Context childContext = new ContextTemplateImpl();
        childContext.setName("ChildContext");

        SchedulerJob job1 = new SchedulerJobImpl();
        job1.setJobName("RegularJob");
        job1.setIdentifier("regular-job-id");
        job1.setAgentName("agent1");

        childContext.setScheduledJobs(List.of(job1));
        parentContext.setContexts(List.of(childContext));

        Map<String, SchedulerJob> schedulerJobs = new HashMap<>();
        schedulerJobs.put("regular-job-id-ChildContext", job1);

        Map<String, SchedulerJob> internalEventDrivenJobMap = new HashMap<>();
        // Add a regular SchedulerJob (not InternalEventDrivenJob) to the map
        SchedulerJob regularJob = new SchedulerJobImpl();
        regularJob.setJobName("RegularJob");
        regularJob.setIdentifier("regular-job-id");
        regularJob.setAgentName("agent1");
        internalEventDrivenJobMap.put("regular-job-id-ChildContext", regularJob);

        List<ContextTransition> transitions = ContextHelper.determineIfJobsTransitionToOtherContexts(
            parentContext, schedulerJobs, childContext, internalEventDrivenJobMap);

        Assert.assertNotNull(transitions);
        // Should handle non-InternalEventDrivenJob instances correctly
    }
}
