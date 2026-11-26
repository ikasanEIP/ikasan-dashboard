package org.ikasan.testharness.it.scheduler;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.pages.scheduler.MonitoringPage;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Job plan instances started and ended by the plan's own time window, with nobody creating them: E2E-CRON's window
 * opens every minute and lasts 90 seconds, and it does not run concurrently. Takes about three minutes, so it is
 * tagged slow and only runs with {@code -Dharness.include=slow}.
 */
@HarnessTest
@Tag("scheduler")
@Tag("slow")
@Tag("mutating")
class CronStartIT
{
    private static final String PLAN = "E2E-CRON";
    private static final String TIMESTAMP = "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[.+ - .+\\]";

    @Test
    void timeWindowStartsAndEndsAnInstance(JobPlansPage jobPlans, SchedulerApi api, Page page)
    {
        jobPlans.open().ensureUploaded(PLAN, api);
        try
        {
            // the window opens at the next full minute
            String instanceId = awaitLiveInstance(api, Timeouts.seconds(90));
            assertThat(api.instanceStatus(instanceId)).as("status of the started instance").contains("WAITING");

            // ... and closes 90 seconds later, ending the instance
            api.waitForInstance(instanceId, "ENDED", Timeouts.seconds(150));

            Map<String, String> row = new MonitoringPage(page).open().completed().rows().stream()
                .filter(r -> Objects.equals(instanceId, r.get("Job Plan Instance Id"))).findFirst()
                .orElseThrow(() -> new AssertionError("ended instance " + instanceId + " is not in the completed grid"));
            assertThat(row).containsEntry("Job Plan Name", PLAN);
            assertThat(row.get("Start Date/Time")).matches(TIMESTAMP);
            assertThat(row.get("End Date/Time")).matches(TIMESTAMP);
        }
        finally
        {
            // stop the plan starting an instance every minute for the rest of the run
            jobPlans.open().setEnabled(PLAN, false);
        }
    }

    private static String awaitLiveInstance(SchedulerApi api, Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline)
        {
            Optional<SchedulerApi.Instance> live = api.instances(false).stream()
                .filter(i -> PLAN.equals(i.contextName())).findFirst();
            if (live.isPresent())
            {
                return live.get().contextInstanceId();
            }
            Instances.sleep(1000);
        }
        throw new AssertionError("no " + PLAN + " instance was started by its time window within " + timeout);
    }
}
