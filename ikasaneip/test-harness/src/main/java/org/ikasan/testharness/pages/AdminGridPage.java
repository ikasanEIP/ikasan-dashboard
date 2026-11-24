package org.ikasan.testharness.pages;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The administration list views that are a single grid: userManagement, groupManagement, roleManagement,
 * policyManagement, quartzSchedulerView, sessionsView, jobLockView.
 */
public class AdminGridPage
{
    private final Page page;

    public AdminGridPage(Page page)
    {
        this.page = page;
    }

    /** Open the view by route and wait for its grid to finish loading. */
    public Grid open(String route)
    {
        Vaadin.gotoRoute(page, route);
        Grid grid = new Grid(page.locator("vaadin-grid").first());
        assertThat(grid.root()).isVisible();
        grid.waitForLoaded();
        return grid;
    }
}
