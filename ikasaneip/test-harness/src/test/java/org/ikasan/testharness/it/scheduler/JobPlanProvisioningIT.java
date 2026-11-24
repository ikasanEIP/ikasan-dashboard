package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.JobPlans;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Uploading and exporting job plans. Scenarios: expectations/scheduler/job-plans.yaml. */
@HarnessTest
@Tag("scheduler")
@Tag("mutating")
class JobPlanProvisioningIT
{
    @Test
    @SuppressWarnings("unchecked")
    void uploadEveryFixture(JobPlansPage jobPlans, SchedulerApi api, HarnessConfig config)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "upload every fixture");
        jobPlans.open();
        for (String plan : (List<String>) s.input().get("plans"))
        {
            if (!api.planExists(plan))
            {
                jobPlans.upload(plan);
            }
        }
        jobPlans.refresh();

        GridRows.assertRows(jobPlans.grid().rows(), GridRows.expectedRows(s.expect()),
            Boolean.TRUE.equals(s.expect().get("rowsExact")), "job plans grid");
    }

    @Test
    void uploadedCommandJobsAreProvisionedToTheAgent(JobPlansPage jobPlans, SchedulerApi api, HarnessSession session)
    {
        FakeSchedulerAgent agent = session.fakeAgent().orElse(null);
        Assumptions.assumeTrue(agent != null, "needs the fake agent to see what the dashboard provisions");
        jobPlans.open();
        if (!api.planExists("E2E-LOGIC"))
        {
            jobPlans.upload("E2E-LOGIC");
        }

        String provisioned = String.join("\n", agent.calls("PUT", "/rest/jobProvision").stream()
            .map(FakeSchedulerAgent.Call::body).toList());
        assertThat(provisioned).as("jobs the dashboard provisioned on the agent").contains("\"a\"", "\"d\"", "\"e\"");
    }

    @Test
    void uploadWithoutAFileIsRejected(JobPlansPage jobPlans, Page page)
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "upload without a file");
        Locator dialog = jobPlans.open().openUploadDialog();

        page.locator("#jobPlanUploadSaveButton").click();

        Vaadin.expectNotification(page, String.valueOf(s.expect().get("notification")));
        page.locator("#jobPlanUploadCancelButton").click();
    }

    @Test
    void uploadingAFileThatIsNotAJobPlanFails(JobPlansPage jobPlans) throws IOException
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "upload a file that is not a job plan");
        Path notAZip = Files.createTempFile("not-a-job-plan", ".zip");
        Files.writeString(notAZip, "this is not a job plan");

        jobPlans.open().upload(notAZip, String.valueOf(s.expect().get("notification")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportedBundlesContainThePlanAndItsJobs(JobPlansPage jobPlans, SchedulerApi api) throws IOException
    {
        Scenario s = Scenarios.one("scheduler/job-plans", "export round trip");
        String plan = String.valueOf(s.input().get("plan"));
        jobPlans.open().ensureUploaded(plan, api);

        Set<String> fixtureJobs;
        try (Stream<Path> files = Files.walk(JobPlans.fixture(plan).resolve("jobs")))
        {
            fixtureJobs = files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        }
        for (String link : (List<String>) s.input().get("links"))
        {
            Download download = jobPlans.export(plan, false, link);
            assertThat(download.suggestedFilename()).as(link).isEqualTo(s.expect().get("file"));

            Set<String> entries = new TreeSet<>();
            try (InputStream in = Files.newInputStream(download.path()); ZipInputStream zip = new ZipInputStream(in))
            {
                for (ZipEntry e; (e = zip.getNextEntry()) != null; )
                {
                    entries.add(e.getName());
                }
            }
            assertThat(entries).as(link + " contains the plan").anyMatch(e -> e.endsWith("/context/" + plan + ".json")
                || e.endsWith(plan + ".json"));
            Set<String> exportedJobs = entries.stream().filter(e -> e.contains("/jobs/") && e.endsWith(".json"))
                .map(e -> e.substring(e.lastIndexOf('/') + 1)).collect(Collectors.toCollection(TreeSet::new));
            assertThat(exportedJobs).as(link + " contains the plan's jobs").containsAll(fixtureJobs);
        }
    }
}
