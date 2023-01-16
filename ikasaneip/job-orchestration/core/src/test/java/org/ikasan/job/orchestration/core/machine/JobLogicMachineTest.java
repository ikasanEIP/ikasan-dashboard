package org.ikasan.job.orchestration.core.machine;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.GlobalEventJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLogicMachineTest extends AbstractTest {
    private ContextService contextService = new ContextService();
    private JobLogicMachine jobLogicMachine = new JobLogicMachine(new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService);

    /**
     * This test evaluates a simple dependency:
     *      agentName1-jobName1 --> agentName2-jobName2
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName2-jobName2",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_single_dependency_relevant_event() throws IOException, InvalidContextTemplateException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        // todo make sure context has been updated
    }

    /**
     * This test evaluates a simple dependency:
     *      agentName1-jobName1 --> agentName2-jobName2
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName2-jobName2",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_single_dependency_relevant_event_job_skipped() throws IOException, InvalidContextTemplateException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");
        context.getScheduledJobsMap().get("agentName1-jobName1").setSkip(true);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);
        eventInstance.setSkipped(true);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency:
     *      agentName1-jobName1 --> agentName2-jobName2
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName2-jobName2",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_single_dependency_relevant_event_not_successful() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", false);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        // No event raise because the job was not successful.
        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency:
     *      agentName1-jobName1 --> agentName2-jobName2
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName2-jobName2",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_single_dependency_relevant_event_job_starting() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", false);
        eventInstance.setJobStarting(true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        // No event raise because the job is starting.
        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency:
     *      agentName1-jobName1 --> agentName2-jobName2
     *
     * The test asserts that no events are raised when an irrelevant scheduled job event is received.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName2-jobName2",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_single_dependency_irrelevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("irrelevantJobName1", "irrelevantAgentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency (a && b) -> c:
     *
     *      agentName1-jobName1 ----------> agentName3-jobName3
     *                               |
     *      agentName2-jobName2 -----
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName3-jobName3",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }, {
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_multiple_dependency_relevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-multiple-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency (a & b) -> c:
     *
     *      agentName1-jobName1 ----------> agentName3-jobName3
     *                               |
     *                              and
     *      agentName2-jobName2 -----
     *
     * The test asserts that no events are raised when an irrelevant scheduled job event is received.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName3-jobName3",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }, {
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "or" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_multiple_dependency_irrelevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-multiple-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("irrelevantJobName1", "irrelevantAgentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency (a || b) -> c:
     *
     *      agentName1-jobName1 ----------> agentName3-jobName3
     *                               |
     *                               or
     *      agentName2-jobName2 -----
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName3-jobName3",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "or" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }, {
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "and" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_or_dependency_relevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency (a || b) -> c:
     *
     *      agentName1-jobName1 ----------> agentName3-jobName3
     *                               |
     *                               or
     *      agentName2-jobName2 -----
     *
     * This test asserts that the same event is not raised twice when the second or event is received.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName3-jobName3",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "or" : [ {
     *         "identifier" : "agentName1-jobName1"
     *       }, {
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "and" : null,
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_or_dependency_relevant_event_make_sure_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);


        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || c) -> d:
     *
     *      agentName1-jobName1 ----------------> agentName4-jobName4
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that an event is raised when the and clause is satisfied.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName4-jobName4",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1",
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "or" :[ {
     *         "identifier" : "agentName3-jobName3"
     *       }],
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_or_dependency_relevant_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || c) -> d:
     *
     *      agentName1-jobName1 ----------------> agentName4-jobName4
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that an event is raised when the or clause is satisfied.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName4-jobName4",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1",
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "or" :[ {
     *         "identifier" : "agentName3-jobName3"
     *       }],
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_or_dependency_relevant_or_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || c) -> d:
     *
     *      agentName1-jobName1 ----------------> agentName4-jobName4
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that the same event is not raised twice when both and and or clauses are satisfied.
     *
     * "jobDependencies" : [ {
     *     "jobIdentifier" : "agentName4-jobName4",
     *     "logicalGrouping" : {
     *       "logicalGrouping" : null,
     *       "and" : [ {
     *         "identifier" : "agentName1-jobName1",
     *         "identifier" : "agentName2-jobName2"
     *       }],
     *       "or" :[ {
     *         "identifier" : "agentName3-jobName3"
     *       }],
     *       "not" : null
     *     }
     *   } ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_or_dependency_relevant_or_statement_fulfilled_assert_initiation_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency (((a && b) || c) && d) -> e:
     *
     *                                       agentName4-jobName5-----
     *                                                              and
     *                                                               |
     *      agentName1-jobName1 -----------O-------------------------0----------> agentName5-jobName5
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that an event is raised when inner and outer and clauses are satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": {
     *           "logicalGrouping": null,
     *           "and": [
     *             {
     *               "identifier": "agentName1-jobName1",
     *               "identifier": "agentName2-jobName2"
     *             }
     *           ],
     *           "or": [
     *             {
     *               "identifier": "agentName3-jobName3"
     *             }
     *           ],
     *           "not": null
     *         },
     *         "and": [
     *           {
     *             "identifier": "agentName4-jobName4"
     *           }
     *         ],
     *         "or": null,
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_and_outer_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency (((a && b) || c) && d) -> e:
     *
     *                                       agentName4-jobName4-----
     *                                                              and
     *                                                               |
     *      agentName1-jobName1 -----------O-------------------------0----------> agentName5-jobName5
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that an event is raised when inner or outer and clauses are satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": {
     *           "logicalGrouping": null,
     *           "and": [
     *             {
     *               "identifier": "agentName1-jobName1",
     *               "identifier": "agentName2-jobName2"
     *             }
     *           ],
     *           "or": [
     *             {
     *               "identifier": "agentName3-jobName3"
     *             }
     *           ],
     *           "not": null
     *         },
     *         "and": [
     *           {
     *             "identifier": "agentName4-jobName4"
     *           }
     *         ],
     *         "or": null,
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_or_outer_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency (((a && b) || c) && d) -> e:
     *
     *                                       agentName4-jobName4-----
     *                                                              and
     *                                                               |
     *      agentName1-jobName1 -----------O-------------------------0----------> agentName5-jobName5
     *                               |     |
     *                               and   |
     *      agentName2-jobName2 -----      |
     *                                     |
     *                                    or
     *      agentName3-jobName3------------
     *
     *  This test asserts that the same event is not raised twice when all clauses are satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": {
     *           "logicalGrouping": null,
     *           "and": [
     *             {
     *               "identifier": "agentName1-jobName1",
     *               "identifier": "agentName2-jobName2"
     *             }
     *           ],
     *           "or": [
     *             {
     *               "identifier": "agentName3-jobName3"
     *             }
     *           ],
     *           "not": null
     *         },
     *         "and": [
     *           {
     *             "identifier": "agentName4-jobName4"
     *           }
     *         ],
     *         "or": null,
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_or_outer_and_statement_fulfilled_assert_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || (c && d)) -> e:
     *
     *      agentName1-jobName1------
     *                               and
     *                               |
     *      agentName2-jobName2-------------
     *                                      |
     *                                      or--------------> agentName5-jobName5
     *                                      |
     *      agentName3-jobName3 ------------
     *                               |
     *                               and
     *      agentName4-jobName4 -----
     *
     *
     *
     *  This test asserts that the event is raised when the left hand side and is satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": null,
     *         "and": [
     *         ],
     *         "or": [
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName1-jobName1"
     *                 },
     *                 {
     *                   "identifier": "agentName2-jobName2"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           },
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName3-jobName3"
     *                 },
     *                 {
     *                   "identifier": "agentName4-jobName4"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           }
     *         ],
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_two_nested_and_with_outer_or_dependency_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-two-nested-and-with-outer-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || (c && d)) -> e:
     *
     *      agentName1-jobName1------
     *                               and
     *                               |
     *      agentName2-jobName2-------------
     *                                      |
     *                                      or--------------> agentName5-jobName5
     *                                      |
     *      agentName3-jobName3 ------------
     *                               |
     *                               and
     *      agentName4-jobName4 -----
     *
     *
     *
     *  This test asserts that the event is raised when the right hand side and is satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": null,
     *         "and": [
     *         ],
     *         "or": [
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName1-jobName1"
     *                 },
     *                 {
     *                   "identifier": "agentName2-jobName2"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           },
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName3-jobName3"
     *                 },
     *                 {
     *                   "identifier": "agentName4-jobName4"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           }
     *         ],
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_two_nested_and_with_outer_or_dependency_and_statement_fulfilled_other_side_of_or_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-two-nested-and-with-outer-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency ((a && b) || (c && d)) -> e:
     *
     *      agentName1-jobName1------
     *                               and
     *                               |
     *      agentName2-jobName2-------------
     *                                      |
     *                                      or--------------> agentName5-jobName5
     *                                      |
     *      agentName3-jobName3 ------------
     *                               |
     *                               and
     *      agentName4-jobName4 -----
     *
     *
     *
     *  This test asserts that the same event is not raised twice when all clauses satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": null,
     *         "and": [
     *         ],
     *         "or": [
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName1-jobName1"
     *                 },
     *                 {
     *                   "identifier": "agentName2-jobName2"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           },
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName3-jobName3"
     *                 },
     *                 {
     *                   "identifier": "agentName4-jobName4"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           }
     *         ],
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_two_nested_and_with_outer_or_dependency_and_statement_fulfilled_all_jobs_assert_only_one_event_created() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-two-nested-and-with-outer-or-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency ((a && b) and (c || d)) -> e:
     *
     *      agentName1-jobName1------
     *                               and
     *                               |
     *      agentName2-jobName2-------------
     *                                      |
     *                                      and--------------> agentName5-jobName5
     *                                      |
     *      agentName3-jobName3 ------------
     *                               |
     *                               or
     *      agentName4-jobName4 -----
     *
     *
     *
     *  This test asserts that the event raised when the left hand and it satisfied and one of the right hand side or is satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": null,
     *         "or": [
     *         ],
     *         "and": [
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName1-jobName1"
     *                 },
     *                 {
     *                   "identifier": "agentName2-jobName2"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           },
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "or": [
     *                 {
     *                   "identifier": "agentName3-jobName3"
     *                 },
     *                 {
     *                   "identifier": "agentName4-jobName4"
     *                 }
     *               ],
     *               "and": [],
     *               "not": null
     *             }
     *           }
     *         ],
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_two_nested_and_or_with_outer_and_dependency_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-outer-and-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    /**
     * This test evaluates a simple dependency (a && (c || d)) -> e:
     *
     *      agentName1-jobName1-------------
     *                                      |
     *                                      and--------------> agentName5-jobName5
     *                                      |
     *      agentName2-jobName2 ------------
     *                               |
     *                               or
     *      agentName3-jobName3 -----
     *
     *
     *
     *  This test asserts that the event raised when the left hand and it satisfied and one of the right hand side or is satisfied.
     *
     * {
     *   "name": "Context1",
     *   "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": {
     *           "logicalGrouping": null,
     *           "and": null,
     *           "or": [
     *             {
     *               "identifier": "agentName2-jobName2",
     *               "identifier": "agentName3-jobName3"
     *             }
     *           ],
     *           "not": null
     *         },
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           }
     *         ],
     *         "or": null,
     *         "not": null
     *       }
     *     }
     *   ],
     *   "contexts": [],
     *   "contextDependencies": [],
     *   "contextParameters": [
     *     {
     *       "name": "name",
     *       "type": "type",
     *       "value": null
     *     },
     *     {
     *       "name": "name",
     *       "type": "type",
     *       "value": null
     *     },
     *     {
     *       "name": "name",
     *       "type": "type",
     *       "value": null
     *     }
     *   ],
     *   "scheduledJobs": [
     *     {
     *       "identifier": "agentName1-jobName1",
     *       "agentName": "agentName1",
     *       "jobName": "jobName1",
     *       "held": false,
     *       "skip": false,
     *       "scheduledProcessEvent": null
     *     },
     *     {
     *       "identifier": "agentName2-jobName2",
     *       "agentName": "agentName2",
     *       "jobName": "jobName2",
     *       "held": false,
     *       "skip": false,
     *       "scheduledProcessEvent": null
     *     },
     *     {
     *       "identifier": "agentName3-jobName3",
     *       "agentName": "agentName3",
     *       "jobName": "jobName3",
     *       "held": false,
     *       "skip": false,
     *       "scheduledProcessEvent": null
     *     },
     *     {
     *       "identifier": "agentName4-jobName4",
     *       "agentName": "agentName4",
     *       "jobName": "jobName4",
     *       "held": false,
     *       "skip": false,
     *       "scheduledProcessEvent": null
     *     },
     *     {
     *       "identifier": "agentName5-jobName5",
     *       "agentName": "agentName5",
     *       "jobName": "jobName5",
     *       "held": false,
     *       "skip": false,
     *       "scheduledProcessEvent": null
     *     }
     *   ],
     *   "createdDateTime": 0,
     *   "updatedDateTime": 0,
     *   "startTime": 0,
     *   "endTime": 0,
     *   "timezone": null,
     *   "status": null
     * }
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_and_with_nested_or() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-or-with-and-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName1-jobName1-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency ((a && b) and (c || d)) -> e:
     *
     *      agentName1-jobName1------
     *                               and
     *                               |
     *      agentName2-jobName2-------------
     *                                      |
     *                                      and--------------> agentName5-jobName5
     *                                      |
     *      agentName3-jobName3 ------------
     *                               |
     *                               or
     *      agentName4-jobName4 -----
     *
     *
     *
     *  This test asserts that the same event is not raised twice when all clauses satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "logicalGrouping": null,
     *         "or": [
     *         ],
     *         "and": [
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "and": [
     *                 {
     *                   "identifier": "agentName1-jobName1"
     *                 },
     *                 {
     *                   "identifier": "agentName2-jobName2"
     *                 }
     *               ],
     *               "or": [],
     *               "not": null
     *             }
     *           },
     *           {
     *             "logicalGrouping": {
     *               "logicalGrouping": null,
     *               "or": [
     *                 {
     *                   "identifier": "agentName3-jobName3"
     *                 },
     *                 {
     *                   "identifier": "agentName4-jobName4"
     *                 }
     *               ],
     *               "and": [],
     *               "not": null
     *             }
     *           }
     *         ],
     *         "not": null
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_two_nested_and_or_with_outer_and_dependency_and_statement_fulfilled_assert_event_not_sent_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-outer-and-dependency.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency where a single job completion raises multiple events:
     *
     *                                      |---------------> agentName2-jobName2
     *                                      |
     *      agentName1-jobName1-------------|
     *                                      |
     *                                      |---------------> agentName5-jobName5
     *                                      |
     *                                      |---------------> agentName3-jobName3
     *                                      |
     *                                      |---------------> agentName4-jobName4
     *
     *
     *
     *  This test asserts that the same event is not raised twice when all clauses satisfied.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName1-jobName1"
     *     },
     *     {
     *       "jobIdentifier": "agentName2-jobName2",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           }
     *         ]
     *       }
     *     },
     *     {
     *       "jobIdentifier": "agentName3-jobName3",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           }
     *         ]
     *       }
     *     },
     *     {
     *       "jobIdentifier": "agentName4-jobName4",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           }
     *         ]
     *       }
     *     },
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           }
     *         ]
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_single_job_creates_multiple_events() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-single-job-produces-multiple-events.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(4, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a more complex set of dependencies:
     *
     *      agentName1-jobName1 ----------------> agentName5-jobName5 ----------> agentName6-jobName6 ---------> agentName8-jobName8
     *                               |     |                              |                               |
     *                              and    |                             and                             and
     *      agentName2-jobName2 -----      |      agentName4-jobName4 ----        agentName7-jobName7 ----
     *                                     |
     *                                    or
     *      agentName3-jobName3-------------
     *
     *
     *  This test asserts that all expected events are raised and that no events are raised twice.
     *
     * "jobDependencies": [
     *     {
     *       "jobIdentifier": "agentName1-jobName1"
     *     },
     *     {
     *       "jobIdentifier": "agentName2-jobName2"
     *     },
     *     {
     *       "jobIdentifier": "agentName3-jobName3"
     *     },
     *     {
     *       "jobIdentifier": "agentName4-jobName4"
     *     },
     *     {
     *       "jobIdentifier": "agentName5-jobName5",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName1-jobName1"
     *           },
     *           {
     *             "identifier": "agentName2-jobName2"
     *           }
     *         ],
     *         "or": [
     *           {
     *             "identifier": "agentName3-jobName3"
     *           }
     *         ]
     *       }
     *     },
     *     {
     *       "jobIdentifier": "agentName6-jobName6",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName5-jobName5"
     *           },
     *           {
     *             "identifier": "agentName4-jobName4"
     *           }
     *         ]
     *       }
     *     },
     *     {
     *       "jobIdentifier": "agentName7-jobName7"
     *     },
     *     {
     *       "jobIdentifier": "agentName8-jobName8",
     *       "logicalGrouping": {
     *         "and": [
     *           {
     *             "identifier": "agentName6-jobName6"
     *           },
     *           {
     *             "identifier": "agentName7-jobName7"
     *           }
     *         ]
     *       }
     *     }
     *   ]
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_chained_jobs() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-chained-jobs.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName6-jobName6-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName7-jobName7-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName8-jobName8-Context1", new InternalEventDrivenJobInstanceImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());


    }

    /**
     * The role of this test is to confirm that context parameters get set on their respective job
     * initiation events as expected.
     *
     * @throws IOException
     */
    @Test
    public void test_simple_context_chained_jobs_with_context_parameters() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-chained-jobs-with-context-parameters.json");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job5 = new InternalEventDrivenJobInstanceImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", job5);
        InternalEventDrivenJobInstanceImpl job6 = new InternalEventDrivenJobInstanceImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6-Context1", job6);
        internalEventDrivenJobs.put("agentName7-jobName7-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job8 = new InternalEventDrivenJobInstanceImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8-Context1", job8);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
        Assert.assertEquals(2, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());
        Assert.assertEquals(3, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());
        Assert.assertEquals(4, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_hack_hard_coding_context_params() throws IOException {

        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("AC_SCRIPT_Interface_SOII", true));
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("BusinessDate", "20220428", "ErrorSearch", "blah", "UseBusinessDate", "1"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);

        SchedulerContextParametersPropertiesProvider schedulerOverrider = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        ContextParametersFactory contextParametersFactory = new ContextParametersFactory(schedulerOverrider);
        ContextParametersInstanceService contextParametersInstanceService = new ContextParametersInstanceServiceImpl(contextParametersFactory);
        jobLogicMachine = new JobLogicMachine(new HashMap<>(), JobLockCacheImpl.instance(), contextParametersInstanceService);

        String json = loadDataFile("/data/logic/simple-context-chained-jobs-with-context-parameters.json");
        String replace = json.replace("\"name\": \"test1\"", "\"name\" : \"BusinessDate\"")
            .replace("\"name\": \"test2\"", "\"name\" : \"ErrorSearch\"")
            .replace("\"name\": \"test3\"", "\"name\" : \"UseBusinessDate\"")
            .replace("\"jobName\": \"jobName5\"", "\"jobName\" : \"AC_SCRIPT_Interface_SOII\"");

        ContextInstance context = this.contextService.getContextInstance(replace);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", new InternalEventDrivenJobInstanceImpl());
        internalEventDrivenJobs.put("agentName4-jobName4-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job5 = new InternalEventDrivenJobInstanceImpl();
        job5.setContextParameters(List.of(getContextParameter("BusinessDate", "String"), getContextParameter("ErrorSearch", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5-Context1", job5);
        InternalEventDrivenJobInstanceImpl job6 = new InternalEventDrivenJobInstanceImpl();
        job6.setContextParameters(List.of(getContextParameter("UseBusinessDate", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6-Context1", job6);
        internalEventDrivenJobs.put("agentName7-jobName7-Context1", new InternalEventDrivenJobInstanceImpl());
        InternalEventDrivenJobInstanceImpl job8 = new InternalEventDrivenJobInstanceImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8-Context1", job8);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("AC_SCRIPT_Interface_SOII", events.get(0).getJobName());
        Assert.assertEquals(2, events.get(0).getContextParameters().size());
        Assert.assertTrue(events.get(0).isSkipped());
        ContextParameterInstanceImpl param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(0);
        Assert.assertEquals("BusinessDate", param.getName());
        Assert.assertEquals("20220428", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(1);
        Assert.assertEquals("ErrorSearch", param.getName());
        Assert.assertEquals("blah", param.getValue());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), new HashMap<>(), context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());
        Assert.assertFalse(events.get(0).isSkipped());
        Assert.assertEquals(3, events.get(0).getContextParameters().size());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(0);
        Assert.assertEquals("UseBusinessDate", param.getName());
        Assert.assertEquals("1", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(1);
        Assert.assertEquals("test4", param.getName());
        Assert.assertEquals("test4", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(2);
        Assert.assertEquals("test5", param.getName());
        Assert.assertEquals("test5", param.getValue());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());
        Assert.assertFalse(events.get(0).isSkipped());
        Assert.assertEquals(4, events.get(0).getContextParameters().size());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(0);
        Assert.assertEquals("test4", param.getName());
        Assert.assertEquals("test4", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(1);
        Assert.assertEquals("test5", param.getName());
        Assert.assertEquals("test5", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(2);
        Assert.assertEquals("test6", param.getName());
        Assert.assertEquals("test6", param.getValue());
        param = (ContextParameterInstanceImpl) events.get(0).getContextParameters().get(3);
        Assert.assertEquals("test7", param.getName());
        Assert.assertEquals("test7", param.getValue());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs, context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_job_repeatable_success() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJobInstance
            = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJobInstance.setJobRepeatable(true);
        internalEventDrivenJobInstance.setJobName("jobName2");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", internalEventDrivenJobInstance);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());
    }

    @Test
    public void test_job_repeatable_not_repeatable() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJobInstance
            = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJobInstance.setJobRepeatable(false);

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2-Context1", internalEventDrivenJobInstance);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(0, events.size());
    }

    /**
     * This test evaluates a simple dependency where by jobName2 is a Global Event
     *      agentName1-jobName1 --> agentName2-jobName2 --> agentName3-jobName3
     */
    @Test
    public void test_global_event_trigger_single_context() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-chained-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        GlobalEventJobInstanceImpl globalEventJobInstance = new GlobalEventJobInstanceImpl();
        globalEventJobInstance.setAgentName("agentName2");
        globalEventJobInstance.setJobName("jobName2");

        HashMap<String, GlobalEventJobInstance> globalEventJobInstances = new HashMap<>();
        globalEventJobInstances.put("agentName2-jobName2-Context1", globalEventJobInstance);
        
        InternalEventDrivenJobInstanceImpl internalEventDrivenJobInstance
            = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJobInstance.setAgentName("agentName3");
        internalEventDrivenJobInstance.setJobName("jobName3");

        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName3-jobName3-Context1", internalEventDrivenJobInstance);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, globalEventJobInstances, internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        // Global Event Successful
        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);
        
        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, globalEventJobInstances, internalEventDrivenJobs
                , context.getContextParameters(), context, new MutableBoolean(false), true);

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());
    }

    private ContextInstance context(String filename) throws IOException {
        return this.contextService.getContextInstance(loadDataFile(filename));
    }
}
