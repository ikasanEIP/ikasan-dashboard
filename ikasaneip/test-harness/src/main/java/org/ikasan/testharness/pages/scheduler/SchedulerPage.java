package org.ikasan.testharness.pages.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The Scheduler view (route 'scheduler'): its tabs, the Scheduler Dashboard widgets and the global job search. */
public class SchedulerPage
{
    private final Page page;

    public SchedulerPage(Page page)
    {
        this.page = page;
    }

    public SchedulerPage open()
    {
        Vaadin.gotoRoute(page, "scheduler");
        assertThat(page.locator("#schedulerViewTabs")).isVisible();
        return this;
    }

    public Locator dashboardTab()
    {
        return page.locator("#schedulerDashboardTab");
    }

    public Locator jobPlansTab()
    {
        return page.locator("#contextTemplateTab");
    }

    /** The Scheduler Agents widget's grid (module metadata of type SCHEDULER_AGENT). */
    public Grid agents()
    {
        dashboardTab().click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#scheduledAgentsFilteringGrid"));
        grid.waitForLoaded();
        return grid;
    }

    /** Double-click an agent to open the Scheduler Agent Management dialog. */
    public Locator openAgent(String agentName)
    {
        agents().doubleClickRow(agentName);
        return Vaadin.dialog(page, "Scheduler Agent Management");
    }
}
