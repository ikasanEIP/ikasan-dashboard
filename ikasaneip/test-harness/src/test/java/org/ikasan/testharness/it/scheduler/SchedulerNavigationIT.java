package org.ikasan.testharness.it.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.scheduler.SchedulerPage;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The Scheduler view: menu, tabs and the registered scheduler agent. Scenarios: expectations/scheduler/navigation.yaml. */
@HarnessTest
@Tag("scheduler")
class SchedulerNavigationIT
{
    @Test
    @Tag("smoke")
    void schedulerMenuOpensTheSchedulerView(Page page, HarnessConfig config)
    {
        Vaadin.gotoRoute(page, "");
        Vaadin.openMenuItem(page, "schedulerMenuItem");

        assertThat(page).hasURL(config.dashboard().url().replaceAll("/$", "") + "/scheduler");
        assertThat(page.locator("#schedulerDashboardTab")).isVisible();
        assertThat(page.locator("#contextTemplateTab")).isVisible();
    }

    @Test
    @Tag("smoke")
    void registeredAgentIsListed(SchedulerPage scheduler, HarnessConfig config)
    {
        Assumptions.assumeTrue(config.scheduler().fake(), "the agent's description is only known for the fake agent");
        Scenario s = Scenarios.one("scheduler/navigation", "registered agent is listed");

        GridRows.assertRows(scheduler.open().agents().rows(), GridRows.expectedRows(s.expect()),
            Boolean.TRUE.equals(s.expect().get("rowsExact")), "scheduler agents");
    }

    @Test
    void agentManagementDialogShowsTheAgent(SchedulerPage scheduler, Page page, HarnessConfig config)
    {
        Locator dialog = scheduler.open().openAgent(config.scheduler().agentName());

        assertThat(dialog.locator("#agentName input")).hasValue(config.scheduler().agentName());
        Vaadin.closeDialog(page, dialog);
    }
}
