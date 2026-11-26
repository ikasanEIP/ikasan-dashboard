package org.ikasan.testharness.it.access;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.LoginAs;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.pages.scheduler.SchedulerPage;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which job plan controls users with narrower access than admin are offered: a scheduler reader may look at plans
 * and start instances but not upload, create, enable/disable or delete them; a scheduler writer may also upload,
 * create and enable/disable, but deleting a plan needs scheduler admin. Uploads a plan as admin if it is missing.
 */
@HarnessTest
@Tag("access")
@Tag("scheduler")
@Tag("mutating")
class AccessSchedulerIT
{
    private static final String PLAN = "E2E-CHAIN";

    @BeforeEach
    void planExists(HarnessSession session, SchedulerApi api)
    {
        try (BrowserContext admin = session.newContext(session.authenticatedState()))
        {
            new JobPlansPage(admin.newPage()).open().ensureUploaded(PLAN, api);
        }
    }

    @Test
    @Tag("smoke")
    @LoginAs("access-reader")
    void aSchedulerReaderMayOnlyLookAndStartInstances(JobPlansPage plans, Page page)
    {
        plans.open().grid().waitForText(PLAN);
        assertThat(Vaadin.isShown(page.locator("#uploadJobPlanButton"))).as("upload offered").isFalse();
        assertThat(Vaadin.isShown(page.locator("#newJobPlanButton"))).as("new job plan offered").isFalse();
        assertThat(Vaadin.isShown(rowIcon(plans, JobPlansPage.DELETE))).as("delete offered").isFalse();
        assertThat(Vaadin.isShown(rowIcon(plans, JobPlansPage.CREATE_INSTANCE))).as("new instance offered").isTrue();
        assertThat(Vaadin.isShown(rowIcon(plans, JobPlansPage.EXPORT))).as("export offered").isTrue();
        assertThat(enableButtonsUsable(plans)).as("enable/disable buttons usable").isFalse();
    }

    @Test
    @LoginAs("access-operator")
    void aSchedulerWriterMayChangePlansButNotDeleteThem(JobPlansPage plans, Page page)
    {
        plans.open().grid().waitForText(PLAN);
        assertThat(Vaadin.isShown(page.locator("#uploadJobPlanButton"))).as("upload offered").isTrue();
        assertThat(Vaadin.isShown(page.locator("#newJobPlanButton"))).as("new job plan offered").isTrue();
        assertThat(Vaadin.isShown(rowIcon(plans, JobPlansPage.DELETE))).as("delete offered").isFalse();
        assertThat(Vaadin.isShown(rowIcon(plans, JobPlansPage.CREATE_INSTANCE))).as("new instance offered").isTrue();
        assertThat(enableButtonsUsable(plans)).as("enable/disable buttons usable").isTrue();
    }

    @Test
    @LoginAs("access-reader")
    void aSchedulerReaderCannotSynchroniseAnAgentOrDownloadItsLog(SchedulerPage scheduler, HarnessConfig config)
    {
        Locator dialog = scheduler.open().openAgent(config.scheduler().agentName());
        assertThat(Vaadin.isShown(Vaadin.button(dialog, "Synchronise All Enabled Agent Jobs"))).as("synchronise offered").isFalse();
        assertThat(Vaadin.isShown(Vaadin.button(dialog, "Download Log File"))).as("download log offered").isFalse();
    }

    @Test
    @LoginAs("access-operator")
    void aSchedulerWriterMaySynchroniseAnAgentAndDownloadItsLog(SchedulerPage scheduler, HarnessConfig config)
    {
        Locator dialog = scheduler.open().openAgent(config.scheduler().agentName());
        assertThat(Vaadin.isShown(Vaadin.button(dialog, "Synchronise All Enabled Agent Jobs"))).as("synchronise offered").isTrue();
        assertThat(Vaadin.isShown(Vaadin.button(dialog, "Download Log File"))).as("download log offered").isTrue();
    }

    /**
     * The job plan and instance pages are reached from the scheduler, but also by link: a user without scheduler
     * access who follows one lands on their dashboard instead.
     */
    @Test
    @LoginAs("access-wiretaps")
    void schedulerPagesTurnAwayUsersWithoutSchedulerAccess(Page page)
    {
        for (String route : java.util.List.of("contextTemplateManagement/" + PLAN, "jobTemplateManagement/" + PLAN + "/" + PLAN + "-1",
            "contextInstance/no-such-instance", "contextInstanceMonitoring", "schedulerJobLogFile/no-such-log"))
        {
            Vaadin.gotoRoute(page, route);
            try
            {
                com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(
                    page.getByText("Flow Status", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
            }
            catch (org.opentest4j.AssertionFailedError e)
            {
                throw new AssertionError("The link to '" + route + "' did not land on the dashboard", e);
            }
        }
    }

    /** A row icon of the job plan grid, by title, in any row. */
    private static Locator rowIcon(JobPlansPage plans, String title)
    {
        return plans.grid().root().locator("vaadin-icon[title='" + title + "']");
    }

    /**
     * Whether the plan can be enabled or disabled: one of its Enabled / Disabled buttons can be pressed (the button
     * of the current state never can; Vaadin marks a disabled button with 'disabled').
     */
    private static boolean enableButtonsUsable(JobPlansPage plans)
    {
        Vaadin.waitForVaadin(plans.page());
        return java.util.stream.Stream.of("Enabled", "Disabled")
            .map(text -> plans.grid().inRowButton(PLAN, text).getAttribute("disabled"))
            .anyMatch(java.util.Objects::isNull);
    }
}
