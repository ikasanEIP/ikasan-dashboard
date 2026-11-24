package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Policy management is read only in the dashboard: list, filter and open a policy's profile.
 * Scenarios: expectations/admin/policies.yaml.
 */
@HarnessTest
@Tag("admin")
@Tag("policies")
class PoliciesIT
{
    @Test
    void filterPoliciesByName(AdminGridPage admin)
    {
        Scenario s = Scenarios.one("admin/policies", "filter policies by name");
        Grid grid = admin.open("policyManagement");

        grid.filterBy(0, String.valueOf(s.input().get("filter")));

        GridRows.assertRows(grid.rows(), GridRows.expectedRows(s.expect()), Boolean.TRUE.equals(s.expect().get("rowsExact")),
            "policies filtered by '" + s.input().get("filter") + "'");
    }

    @Test
    void openAPolicyProfile(AdminGridPage admin, Page page)
    {
        Scenario s = Scenarios.one("admin/policies", "open a policy profile");
        String policy = String.valueOf(s.input().get("policy"));
        Grid grid = admin.open("policyManagement");
        grid.filterBy(0, policy);
        grid.waitForText(policy);

        grid.doubleClickRow(policy);

        Locator dialog = Vaadin.dialog(page, String.valueOf(s.expect().get("title")));
        Vaadin.closeDialog(page, dialog);
        assertThat(dialog).isHidden();
    }
}
