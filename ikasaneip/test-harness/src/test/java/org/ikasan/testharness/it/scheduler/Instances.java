package org.ikasan.testharness.it.scheduler;

import org.ikasan.testharness.support.Timeouts;
import org.ikasan.testharness.pages.scheduler.ContextInstancePage;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.SchedulerApi;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** Shared steps for tests that run job plan instances. */
final class Instances
{
    static final Duration WAIT = Timeouts.seconds(60);
    static final String AGENT = "scheduler-agent";

    private Instances() {}

    /** Upload the plan if needed, create an instance from the Job Plans grid and wait until it is WAITING. */
    static ContextInstancePage start(JobPlansPage jobPlans, SchedulerApi api, String plan)
    {
        jobPlans.open().ensureUploaded(plan, api);
        ContextInstancePage instance = jobPlans.createInstance(plan, false);
        api.waitForInstance(instance.id(), "WAITING", WAIT);
        return instance;
    }

    static String job(String jobName)
    {
        return AGENT + "-" + jobName;
    }

    static void waitForJob(SchedulerApi api, ContextInstancePage instance, String child, String jobName, String status)
    {
        api.waitForJob(instance.id(), child, job(jobName), status, WAIT);
    }

    /** Names of the jobs the dashboard sent to the agent for the instance, in order. */
    static List<String> initiated(FakeSchedulerAgent agent, ContextInstancePage instance)
    {
        return agent.initiations(instance.id()).stream().map(i -> String.valueOf(i.get("jobName"))).toList();
    }

    /** Assert a job is not sent to the agent within a short grace period (it is held, skipped, waiting, ...). */
    static void assertNotInitiated(FakeSchedulerAgent agent, ContextInstancePage instance, String jobName)
    {
        sleep(3000);
        assertThat(initiated(agent, instance)).as("jobs sent to the agent").doesNotContain(jobName);
    }

    static Map<String, String> row(ContextInstancePage instance, String jobName)
    {
        return instance.jobRows().stream().filter(r -> Objects.equals(jobName, r.get("Job Name"))).findFirst()
            .orElseThrow(() -> new AssertionError("no row for job " + jobName));
    }

    static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
