package org.ikasan.testharness.pages.scheduler;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Job plan instance monitoring (route 'contextInstanceMonitoring', ContextInstanceDashboardWidget): active, prepared
 * and completed instances. Per-row controls repeat the same id on every row (e.g. waitingStatusButton), so they are
 * always located within a row.
 */
public class MonitoringPage
{
    private final Page page;

    public MonitoringPage(Page page)
    {
        this.page = page;
    }

    public MonitoringPage open()
    {
        Vaadin.gotoRoute(page, "contextInstanceMonitoring");
        assertThat(page.locator("#contextInstancesTab")).isVisible();
        return this;
    }

    /** Active instances (in-memory, built from live context machines); refreshed. */
    public Grid active()
    {
        page.locator("#activeJobPlanInstancesTab").click();
        Vaadin.waitForVaadin(page);
        page.locator("#refresh-button").click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#contextInstanceAggregateJobStatusGrid"));
        grid.waitForLoaded();
        return grid;
    }

    public Grid prepared()
    {
        page.locator("#preparedFutureJobPlanInstancesTab").click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#preparedFutureContextInstanceGrid"));
        grid.waitForLoaded();
        return grid;
    }

    public Grid completed()
    {
        page.locator("#completedJobPlanInstances").click();
        Vaadin.waitForVaadin(page);
        Grid grid = new Grid(page.locator("#completedContextInstanceGrid"));
        grid.waitForLoaded();
        return grid;
    }

    /** Toggle one of the active tab's status filters, e.g. errorFilterButton, waitingFilterButton. */
    public void toggleStatusFilter(String buttonId)
    {
        page.locator("#" + buttonId).click();
        Vaadin.waitForVaadin(page);
    }

    /** A status count button (e.g. waitingStatusButton) in the row of an instance. */
    public Locator countButton(String contextInstanceId, String buttonId)
    {
        return active().inRow(contextInstanceId, "#" + buttonId);
    }
}
