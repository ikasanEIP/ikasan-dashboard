package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ScheduledProcessEventInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JobLogicMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private JobLogicMachine jobLogicMachine = new JobLogicMachine();

    @Test
    public void test_simple_context_and_single_dependency_relevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName2", events.get(0).getAgentName());
        Assert.assertEquals("jobName2", events.get(0).getJobName());

        // todo make sure context has been updated
    }

    @Test
    public void test_simple_context_and_single_dependency_irrelevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-single-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("irrelevantJobName1", "irrelevantAgentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_simple_context_and_multiple_dependency_relevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-multiple-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_and_multiple_dependency_irrelevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-multiple-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        System.out.println(context);
        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("irrelevantJobName1", "irrelevantAgentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_simple_context_or_dependency_relevant_event() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-or-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_or_dependency_relevant_event_make_sure_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-or-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName3", events.get(0).getAgentName());
        Assert.assertEquals("jobName3", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);


        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_simple_context_and_or_dependency_relevant_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_and_or_dependency_relevant_or_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_and_or_dependency_relevant_or_statement_fulfilled_assert_initiation_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-and-or-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName4", events.get(0).getAgentName());
        Assert.assertEquals("jobName4", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());
    }

    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_and_outer_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_or_outer_and_statement_fulfilled() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());
    }

    @Test
    public void test_simple_context_nested_and_or_with_and_dependency_relevant_inner_or_outer_and_statement_fulfilled_assert_event_not_raised_twice() throws IOException {
        ContextInstance context = context("/data/logic/simple-context-nested-and-or-with-and-dependency.json");

        ScheduledProcessEventInstance eventInstance
            = scheduledProcessEventInstance("jobName3", "agentName3", true);

        List<SchedulerJobInitiationEvent> events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName4", "agentName4", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("agentName5", events.get(0).getAgentName());
        Assert.assertEquals("jobName5", events.get(0).getJobName());

        eventInstance
            = scheduledProcessEventInstance("jobName1", "agentName1", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());

        eventInstance
            = scheduledProcessEventInstance("jobName2", "agentName2", true);

        events =  jobLogicMachine
            .getJobInitiationEvents(eventInstance, context.getScheduledJobsMap(), context.getJobDependencies());

        Assert.assertEquals(0, events.size());
    }

    private ScheduledProcessEventInstance scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful) {
        ScheduledProcessEventInstance eventInstance = new ScheduledProcessEventInstance();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        return eventInstance;
    }

    private ContextInstance context(String filename) throws IOException {
        return this.contextService.getContextInstance(loadDataFile(filename));
    }
}
