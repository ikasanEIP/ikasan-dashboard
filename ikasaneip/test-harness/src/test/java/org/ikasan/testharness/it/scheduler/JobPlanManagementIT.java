package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.scheduler.JobPlansPage;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Job plan template management (route contextTemplateManagement/&lt;plan&gt;). Scenarios: expectations/scheduler/management.yaml. */
@HarnessTest
@Tag("scheduler")
class JobPlanManagementIT
{
    private static void openManagement(Page page, JobPlansPage jobPlans, SchedulerApi api, String plan)
    {
        if (!api.planExists(plan))
        {
            jobPlans.open().upload(plan);
        }
        Vaadin.gotoRoute(page, "contextTemplateManagement/" + plan);
    }

    @Test
    @SuppressWarnings("unchecked")
    void managementViewShowsAndValidatesThePlan(Page page, JobPlansPage jobPlans, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/management", "management view shows the plan");
        openManagement(page, jobPlans, api, String.valueOf(s.input().get("plan")));

        assertThat(page.getByText(String.valueOf(s.expect().get("heading")), new Page.GetByTextOptions().setExact(true)))
            .isVisible();
        assertThat(page.locator("vaadin-tab").allTextContents().stream().map(String::trim).toList())
            .containsExactlyElementsOf((List<String>) s.expect().get("tabs"));

        page.locator("#validateJobPlanButton").click();
        Vaadin.expectNotification(page, String.valueOf(s.expect().get("validation")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void jobTemplatesGridListsThePlansJobs(Page page, JobPlansPage jobPlans, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/management", "job templates grid");
        openManagement(page, jobPlans, api, String.valueOf(s.input().get("plan")));
        page.locator("#jobPlanJobTemplatesTab").click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#jobTemplateGrid"));
        grid.waitForLoaded();

        assertThat(grid.headerTexts()).containsSubsequence((List<String>) s.expect().get("headers"));
        GridRows.assertRows(grid.rows(), GridRows.expectedRows(s.expect()), Boolean.TRUE.equals(s.expect().get("rowsExact")),
            "job templates of " + s.input().get("plan"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rawJsonTabShowsThePlan(Page page, JobPlansPage jobPlans, SchedulerApi api)
    {
        Scenario s = Scenarios.one("scheduler/management", "raw json shows the plan");
        openManagement(page, jobPlans, api, String.valueOf(s.input().get("plan")));
        page.locator("#jobPlanRawContextTab").click();
        Vaadin.waitForVaadin(page);

        String json = String.valueOf(page.locator("lit-ace").first().evaluate(
            "e => e.value || (e.editor && e.editor.getValue && e.editor.getValue())"
                + " || (e.shadowRoot && e.shadowRoot.textContent) || e.textContent"));
        assertThat(json).contains((List<String>) s.expect().get("contains"));
    }

    /** Download one of the bundles offered by a management view Actions menu item. */
    private static java.nio.file.Path download(Page page, String menuItem, String link)
    {
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Actions").setExact(true)).click();
        page.getByRole(com.microsoft.playwright.options.AriaRole.MENUITEM,
            new Page.GetByRoleOptions().setName(menuItem).setExact(true)).click();
        com.microsoft.playwright.Locator dialog = Vaadin.dialog(page, link);
        com.microsoft.playwright.Download download = page.waitForDownload(() ->
            dialog.getByText(link, new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true)).first().click());
        page.keyboard().press("Escape");
        Vaadin.waitForVaadin(page);
        return download.path();
    }

    /** The plain export keeps the agent names; "With Tokens" replaces them with the agent token. */
    @Test
    @SuppressWarnings("unchecked")
    void exportsWithAndWithoutTokens(Page page, JobPlansPage jobPlans, SchedulerApi api) throws java.io.IOException
    {
        Scenario s = Scenarios.one("scheduler/management", "exports with and without tokens");
        java.util.Map<String, Object> menu = (java.util.Map<String, Object>) s.input().get("menu");
        String agent = String.valueOf(s.expect().get("agent"));
        String token = String.valueOf(s.expect().get("agentToken"));
        openManagement(page, jobPlans, api, String.valueOf(s.input().get("plan")));

        for (String link : (List<String>) s.input().get("links"))
        {
            assertThat(org.ikasan.testharness.scheduler.JobPlans.jobAgentNames(download(page, String.valueOf(menu.get("plain")), link)))
                .as(menu.get("plain") + " / " + link).containsExactly(agent);
            assertThat(org.ikasan.testharness.scheduler.JobPlans.jobAgentNames(download(page, String.valueOf(menu.get("withTokens")), link)))
                .as(menu.get("withTokens") + " / " + link).containsExactly(token);
        }
    }

    /** The Job Plans grid's "with tokens" export does the same (the reference the management view must match). */
    @Test
    @SuppressWarnings("unchecked")
    void gridExportWithTokens(JobPlansPage jobPlans, SchedulerApi api) throws java.io.IOException
    {
        Scenario s = Scenarios.one("scheduler/management", "exports with and without tokens");
        String plan = String.valueOf(s.input().get("plan"));
        jobPlans.open().ensureUploaded(plan, api);

        for (String link : (List<String>) s.input().get("links"))
        {
            assertThat(org.ikasan.testharness.scheduler.JobPlans.jobAgentNames(jobPlans.export(plan, true, link).path()))
                .as("grid export with tokens / " + link).containsExactly(String.valueOf(s.expect().get("agentToken")));
        }
    }
}
