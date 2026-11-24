package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.pages.scheduler.ContextInstancePage;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.pages.scheduler.MonitoringPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Grid;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Job plan instance monitoring (route contextInstanceMonitoring). The active grid is built from the live context
 * machines; each row has a count button per job status (columns without header text). The completed grid lists
 * ended instances with their start and end.
 */
@HarnessTest
@Tag("scheduler")
@Tag("mutating")
class ContextInstanceMonitoringIT
{
    private static final String TIMESTAMP = "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[.+ - .+\\]";

    private static Map<String, String> rowOf(List<Map<String, String>> rows, String instanceId)
    {
        return rows.stream().filter(r -> Objects.equals(instanceId, r.get("Job Plan Instance Id"))).findFirst()
            .orElseThrow(() -> new AssertionError("no row for instance " + instanceId + " in " + rows));
    }

    /** E2E-FAILURE after failJob failed: trigger COMPLETE, failJob ERROR, after WAITING. */
    private static ContextInstancePage failedInstance(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent)
    {
        ContextInstancePage instance = Instances.start(jobPlans, api, "E2E-FAILURE");
        agent.fireJob("E2E-FAILURE", instance.id(), "trigger", List.of("E2E-FAILURE-1"), FakeSchedulerAgent.Outcome.succeed());
        Instances.waitForJob(api, instance, "E2E-FAILURE-1", "failJob", "ERROR");
        return instance;
    }

    @Test
    @Tag("smoke")
    void activeGridShowsTheLiveInstanceWithItsJobCounts(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent,
                                                        Page page)
    {
        ContextInstancePage instance = failedInstance(jobPlans, api, agent);

        Grid active = new MonitoringPage(page).open().active();
        assertThat(active.headerTexts()).containsSubsequence("Job Plan Name", "Job Plan Instance Id", "Job Status Counts",
            "Repeating Jobs");
        assertThat(rowOf(active.rows(), instance.id())).isEqualTo(Map.of("Job Plan Name", "E2E-FAILURE",
            "Job Plan Instance Id", instance.id(), "Job Status Counts", "1 WAITING", "Repeating Jobs", "not applicable"));
        // the row's buttons are the instance id link, icon-only buttons and one "<count> <STATUS>" button per status
        assertThat(active.attributesInRow(instance.id(), "vaadin-button", "textContent").stream()
            .filter(t -> t.matches("\\d+ .+")).toList())
            .as("status counts of instance " + instance.id())
            .containsExactly("1 WAITING", "1 COMPLETE", "0 RUNNING", "0 QUEUED", "0 ON HOLD", "0 SKIPPED", "0 DISABLED", "1 ERROR", "0 ACKED");
    }

    @Test
    void errorFilterShowsOnlyInstancesWithErrors(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent, Page page)
    {
        ContextInstancePage failed = failedInstance(jobPlans, api, agent);
        ContextInstancePage waiting = Instances.start(jobPlans, api, "E2E-CHAIN");

        MonitoringPage monitoring = new MonitoringPage(page).open();
        monitoring.active();
        monitoring.toggleStatusFilter("errorFilterButton");
        List<String> ids = new Grid(page.locator("#contextInstanceAggregateJobStatusGrid")).rows().stream()
            .map(r -> r.get("Job Plan Instance Id")).toList();

        assertThat(ids).contains(failed.id()).doesNotContain(waiting.id());
    }

    @Test
    void completedGridListsEndedInstances(JobPlansPage jobPlans, SchedulerApi api, FakeSchedulerAgent agent, Page page)
    {
        ContextInstancePage instance = Instances.start(jobPlans, api, "E2E-CHAIN");
        agent.fireJob("E2E-CHAIN", instance.id(), "trigger", List.of("E2E-CHAIN-1"), FakeSchedulerAgent.Outcome.succeed());
        api.waitForInstance(instance.id(), "ENDED", Instances.WAIT);

        MonitoringPage monitoring = new MonitoringPage(page).open();
        assertThat(monitoring.active().rows()).noneMatch(r -> instance.id().equals(r.get("Job Plan Instance Id")));
        Grid completed = monitoring.completed();
        assertThat(completed.headerTexts()).containsSubsequence("Job Plan Name", "Job Plan Instance Id", "Start Date/Time",
            "End Date/Time");
        Map<String, String> row = rowOf(completed.rows(), instance.id());
        assertThat(row).containsEntry("Job Plan Name", "E2E-CHAIN");
        assertThat(row.get("Start Date/Time")).matches(TIMESTAMP);
        assertThat(row.get("End Date/Time")).matches(TIMESTAMP);
    }
}
