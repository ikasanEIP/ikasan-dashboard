package org.ikasan.testharness.it.scheduler;

import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.scheduler.ContextInstancePage;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Running job plan instances end to end: create an instance through the UI, fire its root job, and follow the
 * dashboard sending command jobs to the agent and the agent reporting back, to completion.
 * Scenarios: expectations/scheduler/instance-run.yaml.
 */
@HarnessTest
@Tag("scheduler")
@Tag("mutating")
class JobPlanInstanceRunIT
{
    private static final Duration WAIT = Duration.ofSeconds(60);

    @Test
    @Tag("smoke")
    @SuppressWarnings("unchecked")
    void chainRunsToCompletion(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        Scenario s = Scenarios.one("scheduler/instance-run", "chain runs to completion");
        String plan = String.valueOf(s.input().get("plan"));
        List<String> children = List.of(String.valueOf(s.input().get("childContext")));
        Map<String, Object> expect = s.expect();

        jobPlans.open().ensureUploaded(plan, api);
        ContextInstancePage instance = jobPlans.createInstance(plan, false);
        api.waitForInstance(instance.id(), "WAITING", WAIT);
        GridRows.assertRows(instance.jobRows(), GridRows.expectedRows((Map<String, Object>) expect.get("waiting")),
            true, "jobs of a new " + plan + " instance");

        agent.fireJob(plan, instance.id(), String.valueOf(s.input().get("rootJob")), children, FakeSchedulerAgent.Outcome.succeed());
        api.waitForInstance(instance.id(), String.valueOf(expect.get("finalInstanceStatus")), WAIT);

        assertThat(agent.initiations(instance.id()).stream().map(i -> i.get("jobName")).toList())
            .as("jobs the dashboard sent to the agent, in order")
            .isEqualTo(expect.get("initiationOrder"));
        GridRows.assertRows(instance.jobRows(), GridRows.expectedRows((Map<String, Object>) expect.get("completed")),
            true, "jobs of the completed " + plan + " instance");
    }

    /** d = AND(a, b), e = OR(b, c): with b held running, e runs (via c) but d waits until b completes. */
    @Test
    void andOrDependencies(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-LOGIC";
        String child = "E2E-LOGIC-1";
        agent.script("b", FakeSchedulerAgent.Outcome.runUntilCompleted());
        try
        {
            ContextInstancePage instance = Instances.start(jobPlans, api, plan);
            agent.fireJob(plan, instance.id(), "trigger", List.of(child), FakeSchedulerAgent.Outcome.succeed());

            Instances.waitForJob(api, instance, child, "e", "COMPLETE");
            Instances.waitForJob(api, instance, child, "a", "COMPLETE");
            assertThat(api.jobStatus(instance.id(), child, Instances.job("b"))).contains("RUNNING");
            Instances.assertNotInitiated(agent, instance, "d");

            agent.complete(instance.id(), "b", FakeSchedulerAgent.Outcome.succeed());
            Instances.waitForJob(api, instance, child, "d", "COMPLETE");
            List<String> order = Instances.initiated(agent, instance);
            assertThat(order).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
            assertThat(order.indexOf("d")).as("d after a and b").isGreaterThan(Math.max(order.indexOf("a"), order.indexOf("b")));
        }
        finally
        {
            agent.clearScripts();
        }
    }

    /** The plan's parameter (GREETING=hello) goes to the agent with the job that uses it. */
    @Test
    void planParametersAreSentWithTheJob(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-PARAMS";
        ContextInstancePage instance = Instances.start(jobPlans, api, plan);
        agent.fireJob(plan, instance.id(), "trigger", List.of("E2E-PARAMS-1"), FakeSchedulerAgent.Outcome.succeed());

        Map<String, Object> initiation = agent.awaitInitiation(instance.id(), "paramJob", WAIT);
        assertThat(initiation.toString()).as("initiation of paramJob").contains("GREETING").contains("hello");
        api.waitForInstance(instance.id(), "ENDED", WAIT);
    }

    /** Child plans joined by a terminal and a start job run in order, and the plan ends. */
    @Test
    void nestedChildPlansRunInOrder(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-NESTED";
        ContextInstancePage instance = Instances.start(jobPlans, api, plan);
        agent.fireJob(plan, instance.id(), "trigger", List.of("E2E-NESTED-STEP-1"), FakeSchedulerAgent.Outcome.succeed());

        api.waitForInstance(instance.id(), "ENDED", WAIT);
        assertThat(Instances.initiated(agent, instance)).isEqualTo(List.of("step1Job", "step2Job"));
    }

    /** A global event reaches every live instance waiting on it. */
    @Test
    void globalEventStartsEveryWaitingInstance(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-GLOBAL";
        ContextInstancePage first = Instances.start(jobPlans, api, plan);
        ContextInstancePage second = Instances.start(jobPlans, api, plan);

        api.raiseGlobalEvent("e2e-go");

        for (ContextInstancePage instance : List.of(first, second))
        {
            Instances.waitForJob(api, instance, "E2E-GLOBAL-1", "afterGlobal", "COMPLETE");
            assertThat(Instances.initiated(agent, instance)).as(instance.id()).isEqualTo(List.of("afterGlobal"));
        }
    }
}
