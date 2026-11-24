package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.pages.scheduler.ContextInstancePage;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent.Outcome;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ikasan.testharness.it.scheduler.Instances.WAIT;
import static org.ikasan.testharness.it.scheduler.Instances.assertNotInitiated;
import static org.ikasan.testharness.it.scheduler.Instances.initiated;
import static org.ikasan.testharness.it.scheduler.Instances.row;
import static org.ikasan.testharness.it.scheduler.Instances.start;
import static org.ikasan.testharness.it.scheduler.Instances.waitForJob;

/**
 * Operator actions on the jobs of a live job plan instance, through the instance view's Job Instances grid and its
 * Actions popup. Each test runs its own instance; statuses are checked on the context machine (REST) and in the grid.
 */
@HarnessTest
@Tag("scheduler")
@Tag("mutating")
class JobInstanceActionsIT
{
    private static final String CHAIN = "E2E-CHAIN";
    private static final String CHAIN_1 = "E2E-CHAIN-1";

    @AfterEach
    void clearScripts(FakeSchedulerAgent agent)
    {
        agent.clearScripts();
    }

    private static void fireTrigger(FakeSchedulerAgent agent, ContextInstancePage instance, String plan, String child)
    {
        agent.fireJob(plan, instance.id(), "trigger", List.of(child), Outcome.succeed());
    }

    @Test
    @Tag("smoke")
    void heldJobWaitsUntilReleased(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        instance.jobAction("okA", ContextInstancePage.HOLD);
        waitForJob(api, instance, CHAIN_1, "okA", "ON_HOLD");
        // the grid shows statuses with spaces
        assertThat(row(instance, "okA").get("Status")).isEqualTo("ON HOLD");

        fireTrigger(agent, instance, CHAIN, CHAIN_1);
        waitForJob(api, instance, CHAIN_1, "trigger", "COMPLETE");
        assertNotInitiated(agent, instance, "okA");
        assertThat(api.jobStatus(instance.id(), CHAIN_1, Instances.job("okA"))).contains("ON_HOLD");

        instance.jobAction("okA", ContextInstancePage.RELEASE);
        api.waitForInstance(instance.id(), "ENDED", WAIT);
        assertThat(initiated(agent, instance)).isEqualTo(List.of("okA", "okB"));
    }

    @Test
    void skippedJobIsNotRunAndTheChainContinues(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        instance.jobAction("okA", ContextInstancePage.SKIP);
        waitForJob(api, instance, CHAIN_1, "okA", "SKIPPED");

        fireTrigger(agent, instance, CHAIN, CHAIN_1);
        api.waitForInstance(instance.id(), "ENDED", WAIT);
        // a skipped job is still sent to the agent, flagged skipped, so that the agent reports it without running it
        assertThat(agent.initiations(instance.id())).extracting(i -> i.get("jobName") + (Boolean.TRUE.equals(i.get("skipped")) ? " (skipped)" : ""))
            .isEqualTo(List.of("okA (skipped)", "okB"));
        assertThat(row(instance, "okA").get("Status")).startsWith("SKIPPED");
    }

    @Test
    void disabledJobCanBeReinstated(JobPlansPage jobPlans, SchedulerApi api)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        instance.jobAction("okB", ContextInstancePage.DISABLE);
        waitForJob(api, instance, CHAIN_1, "okB", "DISABLED");
        assertThat(row(instance, "okB").get("Status")).isEqualTo("DISABLED");
        assertThat(instance.visibleJobActions("okB")).contains(ContextInstancePage.REINSTATE);

        instance.jobAction("okB", ContextInstancePage.REINSTATE);
        waitForJob(api, instance, CHAIN_1, "okB", "WAITING");
        assertThat(row(instance, "okB").get("Status")).isEqualTo("WAITING");
    }

    /** failJob exits 3 -> ERROR, its downstream waits; acknowledge, reset (WAITING), resubmit -> the plan completes. */
    @Test
    void failedJobIsAcknowledgedAndReset(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-FAILURE";
        String child = "E2E-FAILURE-1";
        ContextInstancePage instance = start(jobPlans, api, plan);

        fireTrigger(agent, instance, plan, child);
        waitForJob(api, instance, child, "failJob", "ERROR");
        assertNotInitiated(agent, instance, "after");
        assertThat(row(instance, "failJob").get("Status")).isEqualTo("ERROR");
        assertThat(instance.visibleJobActions("failJob"))
            .contains(ContextInstancePage.ACKNOWLEDGE_ERROR, ContextInstancePage.RESET);

        instance.acknowledgeError("failJob", "E2E-1", "Acknowledged by the test harness");
        assertThat(instance.visibleJobActions("failJob")).doesNotContain(ContextInstancePage.ACKNOWLEDGE_ERROR);

        // reset puts the job back to WAITING; it does not run it again by itself
        instance.jobAction("failJob", ContextInstancePage.RESET);
        waitForJob(api, instance, child, "failJob", "WAITING");
        assertThat(row(instance, "failJob").get("Status")).isEqualTo("WAITING");

        agent.script("failJob", Outcome.succeed());
        instance.submitCommandJob("failJob");
        waitForJob(api, instance, child, "after", "COMPLETE");
        assertThat(initiated(agent, instance)).isEqualTo(List.of("failJob", "failJob", "after"));
        assertThat(api.instanceStatus(instance.id())).contains("COMPLETE");
    }

    /**
     * A waiting command job submitted by hand runs straight away, and the grid records who submitted it (recorded
     * before the job is sent, so the context machine's own saves of the row keep it: ManualJobSubmission).
     */
    @Test
    void commandJobCanBeSubmittedByHand(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        // the fake agent completes the job within milliseconds, racing the dialog: the hard case for the record
        instance.submitCommandJob("okB");
        agent.awaitInitiation(instance.id(), "okB", WAIT);
        waitForJob(api, instance, CHAIN_1, "okB", "COMPLETE");
        assertThat(row(instance, "okB")).containsEntry("Status", "COMPLETE").containsEntry("Manually Submitted By", "admin");
        assertThat(initiated(agent, instance)).as("only the submitted job ran").isEqualTo(List.of("okB"));
    }

    /** Submitting the quartz trigger asks the agent to fire it now, for this instance. */
    @Test
    void scheduledJobSubmitAsksTheAgentToFireIt(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        instance.jobAction("trigger", ContextInstancePage.SUBMIT);
        Vaadin.expectNotification(instance.page(), "Job submitted successfully!");
        agent.awaitFireNow("trigger", instance.id(), WAIT);
    }

    /** A running job shows RUNNING with a start time and no end time until the agent reports it done. */
    @Test
    void runningJobIsShownUntilItCompletes(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-SLOW";
        String child = "E2E-SLOW-1";
        agent.script("slowJob", Outcome.runUntilCompleted());
        ContextInstancePage instance = start(jobPlans, api, plan);

        fireTrigger(agent, instance, plan, child);
        waitForJob(api, instance, child, "slowJob", "RUNNING");
        assertThat(row(instance, "slowJob")).containsEntry("Status", "RUNNING").containsEntry("End Time", "N/A");
        assertThat(agent.runningJobs()).contains(instance.id() + "/slowJob");

        agent.complete(instance.id(), "slowJob", Outcome.succeed());
        waitForJob(api, instance, child, "next", "COMPLETE");
    }

    @Test
    void holdAllAndReleaseAllCommandJobs(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        String plan = "E2E-LOGIC";
        String child = "E2E-LOGIC-1";
        ContextInstancePage instance = start(jobPlans, api, plan);

        instance.instanceAction("holdContextButton");
        for (String job : List.of("a", "b", "c", "d", "e"))
        {
            waitForJob(api, instance, child, job, "ON_HOLD");
        }
        assertThat(api.jobStatus(instance.id(), child, Instances.job("trigger"))).as("scheduled job not held")
            .contains("WAITING");

        instance.instanceAction("releaseContextButton");
        for (String job : List.of("a", "b", "c", "d", "e"))
        {
            waitForJob(api, instance, child, job, "WAITING");
        }
        fireTrigger(agent, instance, plan, child);
        api.waitForInstance(instance.id(), "COMPLETE", WAIT);
    }

    @Test
    void scheduledJobsOfAnInstanceCanBeDisabledAndEnabled(JobPlansPage jobPlans, SchedulerApi api, Page page)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        page = instance.page();
        instance.instanceAction("disableQuartzScheduledJobsButton");
        Vaadin.expectNotification(page, "All scheduled jobs have been successfully disabled.");
        waitForJob(api, instance, CHAIN_1, "trigger", "DISABLED");

        instance.instanceAction("enableQuartzScheduledJobsButton");
        Vaadin.expectNotification(page, "All scheduled jobs have been successfully enabled.");
        waitForJob(api, instance, CHAIN_1, "trigger", "WAITING");
    }

    /** Ignore the plan's duration, then end the instance by hand. */
    @Test
    void instanceCanBeEndedByHand(JobPlansPage jobPlans, SchedulerApi api, Page page)
    {
        ContextInstancePage instance = start(jobPlans, api, CHAIN);

        page = instance.page();
        instance.instanceAction("ignoreContextInstanceEndButton");
        Vaadin.expectNotification(page, "The job plan duration has been successfully ignored");

        instance.instanceAction("contextInstanceEndButton");
        Vaadin.expectNotification(page, "This job plan instance has ended successfully");
        api.waitForInstance(instance.id(), "ENDED", WAIT);
    }
}
