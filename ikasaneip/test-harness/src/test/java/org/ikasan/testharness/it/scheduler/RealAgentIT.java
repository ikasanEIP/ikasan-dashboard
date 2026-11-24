package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Locator;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.pages.scheduler.ContextInstancePage;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.ikasan.testharness.it.scheduler.Instances.WAIT;
import static org.ikasan.testharness.it.scheduler.Instances.row;
import static org.ikasan.testharness.it.scheduler.Instances.start;
import static org.ikasan.testharness.it.scheduler.Instances.waitForJob;

/**
 * Job plan instances run by the real Ikasan scheduler agent (docker/compose.yml service 'scheduler-agent'): the
 * agent fires the quartz trigger when the operator submits it, runs the command lines in its container, and its
 * job logs hold their real output. Runs only with {@code -Dharness.scheduler.agent=real}.
 */
@HarnessTest
@Tag("scheduler")
@Tag("real-agent")
@Tag("mutating")
class RealAgentIT
{
    /** Submit the scheduled trigger: the real agent fires it, which starts the plan's command jobs. */
    private static void submitTrigger(ContextInstancePage instance)
    {
        instance.jobAction("trigger", ContextInstancePage.SUBMIT);
        Vaadin.expectNotification(instance.page(), "Job submitted successfully!");
    }

    @Test
    @Tag("smoke")
    void chainRunsOnTheAgentAndLogsItsOutput(JobPlansPage jobPlans, SchedulerApi api)
    {
        ContextInstancePage instance = start(jobPlans, api, "E2E-CHAIN");

        submitTrigger(instance);
        api.waitForInstance(instance.id(), "ENDED", WAIT);

        for (String job : java.util.List.of("trigger", "okA", "okB"))
        {
            org.assertj.core.api.Assertions.assertThat(row(instance, job)).as(job).containsEntry("Status", "COMPLETE");
        }
        assertThat(instance.viewLog("okA")).containsText("alpha");
    }

    /** "echo boom; exit 3": the job is in ERROR with its output in the log, and its downstream job does not run. */
    @Test
    void failingCommandPutsTheJobInError(JobPlansPage jobPlans, SchedulerApi api)
    {
        ContextInstancePage instance = start(jobPlans, api, "E2E-FAILURE");

        submitTrigger(instance);
        waitForJob(api, instance, "E2E-FAILURE-1", "failJob", "ERROR");

        org.assertj.core.api.Assertions.assertThat(row(instance, "after")).containsEntry("Status", "WAITING");
        assertThat(instance.viewLog("failJob")).containsText("boom");
    }

    /** The plan parameter GREETING=hello is in the job's environment: "echo $GREETING" logs it. */
    @Test
    void planParameterReachesTheCommand(JobPlansPage jobPlans, SchedulerApi api)
    {
        ContextInstancePage instance = start(jobPlans, api, "E2E-PARAMS");

        submitTrigger(instance);
        api.waitForInstance(instance.id(), "ENDED", WAIT);

        assertThat(instance.viewLog("paramJob")).containsText("hello");
    }

    /** "sleep 30" runs on the agent; ending the instance by hand kills it. */
    @Test
    void endingTheInstanceKillsARunningJob(JobPlansPage jobPlans, SchedulerApi api)
    {
        ContextInstancePage instance = start(jobPlans, api, "E2E-SLOW");

        submitTrigger(instance);
        waitForJob(api, instance, "E2E-SLOW-1", "slowJob", "RUNNING");

        instance.instanceAction("ignoreContextInstanceEndButton");
        instance.instanceAction("contextInstanceEndButton");
        Vaadin.expectNotification(instance.page(), "This job plan instance has ended successfully");
        api.waitForInstance(instance.id(), "ENDED", WAIT);

        org.assertj.core.api.Assertions.assertThat(row(instance, "slowJob").get("Status")).isEqualTo("KILLED");
        org.assertj.core.api.Assertions.assertThat(row(instance, "next").get("Status")).isEqualTo("WAITING");
    }
}
