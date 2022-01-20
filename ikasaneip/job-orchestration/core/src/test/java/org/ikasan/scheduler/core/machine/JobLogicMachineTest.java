package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.context.validation.ContextTemplateValidator;
import org.ikasan.scheduler.context.validation.InvalidContextTemplateException;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.context.ContextParameterImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.core.model.job.InternalEventDrivenJobImpl;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JobLogicMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private JobLogicMachine jobLogicMachine = new JobLogicMachine();

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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
    public void test_simple_context_and_single_dependency_relevant_event_not_successful() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", false);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

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
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

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
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("irrelevantJobName1", "irrelevantAgentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);


        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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
        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(4, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null,internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName5-jobName5", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName6-jobName6", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName8-jobName8", new InternalEventDrivenJobImpl());

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

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

        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();
        internalEventDrivenJobs.put("agentName2-jobName2", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName3-jobName3", new InternalEventDrivenJobImpl());
        internalEventDrivenJobs.put("agentName4-jobName4", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job5 = new InternalEventDrivenJobImpl();
        job5.setContextParameters(List.of(getContextParameter("test1", "String"), getContextParameter("test2", "String")));
        internalEventDrivenJobs.put("agentName5-jobName5", job5);
        InternalEventDrivenJobImpl job6 = new InternalEventDrivenJobImpl();
        job6.setContextParameters(List.of(getContextParameter("test3", "String")
            , getContextParameter("test4", "String")
            , getContextParameter("test5", "String")));
        internalEventDrivenJobs.put("agentName6-jobName6", job6);
        internalEventDrivenJobs.put("agentName7-jobName7", new InternalEventDrivenJobImpl());
        InternalEventDrivenJobImpl job8 = new InternalEventDrivenJobImpl();
        job8.setContextParameters(List.of(getContextParameter("test4", "String")
            , getContextParameter("test5", "String")
            , getContextParameter("test6", "String")
            , getContextParameter("test7", "String")));
        internalEventDrivenJobs.put("agentName8-jobName8", job8);

        ContextualisedScheduledProcessEventImpl eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
        Assert.assertEquals(2, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, new HashMap<>(), context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName5", "agentName5", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName6", events.get(0).getAgentName());
        Assert.assertEquals("jobName6", events.get(0).getJobName());
        Assert.assertEquals(3, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName6", "agentName6", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName7", "agentName7", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName8", events.get(0).getAgentName());
        Assert.assertEquals("jobName8", events.get(0).getJobName());
        Assert.assertEquals(4, events.get(0).getContextParameters().size());

        eventInstance
            = scheduledProcessEventInstance("jobName8", "agentName8", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context, null, internalEventDrivenJobs, context.getContextParameters());

        Assert.assertEquals(0, events.size());
    }

    private ContextInstance context(String filename) throws IOException {
        return this.contextService.getContextInstance(loadDataFile(filename));
    }

    private ContextTemplate contextTemplate(String filename) throws IOException {
        return this.contextService.getContext(loadDataFile(filename));
    }
}
