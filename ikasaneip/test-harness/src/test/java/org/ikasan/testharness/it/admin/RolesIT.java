package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Role management: create, reject duplicates, add policies and users, delete.
 * Scenarios: expectations/admin/roles.yaml. Each test creates its own uniquely named role and deletes it.
 */
@HarnessTest
@Tag("admin")
@Tag("roles")
@Tag("mutating")
class RolesIT
{
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);
    private static final String TRASH = "vaadin-icon[icon='vaadin:trash']";

    private static Grid openRoles(Page page)
    {
        Vaadin.gotoRoute(page, "roleManagement");
        Grid grid = new Grid(page.locator("#roleGrid"));
        grid.waitForLoaded();
        return grid;
    }

    /** Create a role through NewRoleDialog and wait for it in the grid. */
    private static void createRole(Page page, Grid roles, String name, String description)
    {
        page.locator("#addRoleButton").click();
        Locator dialog = Vaadin.dialog(page, "Create a New Role");
        Vaadin.fillField(page.locator("#nameTf"), name);
        Vaadin.fillField(page.locator("#descriptionTf"), description);
        page.locator("#saveButton").click();
        Vaadin.waitForVaadin(page);
        assertThat(dialog).isHidden();
        roles.filterBy(0, name);
        roles.waitForText(name);
    }

    private static void deleteRole(Page page, Grid roles, String name)
    {
        roles.filterBy(0, name);
        roles.inRow(name, TRASH).click();
        Vaadin.waitForVaadin(page);
        roles.waitForNoText(name);
    }

    private static String uniqueName(String scenario)
    {
        return Scenarios.one("admin/roles", scenario).input().getOrDefault("name", "harness-role") + "-" + SUFFIX
            + "-" + Long.toString(System.nanoTime() % 100_000, 36);
    }

    @Test
    void createAndDeleteARole(Page page)
    {
        Scenario s = Scenarios.one("admin/roles", "create role");
        String name = uniqueName("create role");
        Grid roles = openRoles(page);

        createRole(page, roles, name, String.valueOf(s.input().get("description")));
        deleteRole(page, roles, name);
    }

    /**
     * Creating a role with an existing name must fail and leave the existing role untouched.
     * Uses its own role: an overwrite of a baseline role (e.g. ADMIN) would lock the admin user out.
     */
    @Test
    void duplicateRoleNameIsRejected(Page page)
    {
        Scenario s = Scenarios.one("admin/roles", "duplicate role name is rejected");
        String name = uniqueName("duplicate role name is rejected");
        String original = String.valueOf(s.input().get("description"));
        Grid roles = openRoles(page);
        createRole(page, roles, name, original);
        try
        {
            page.locator("#addRoleButton").click();
            Locator dialog = Vaadin.dialog(page, "Create a New Role");
            Vaadin.fillField(page.locator("#nameTf"), name);
            Vaadin.fillField(page.locator("#descriptionTf"), String.valueOf(s.input().get("duplicateDescription")));
            page.locator("#saveButton").click();
            Vaadin.waitForVaadin(page);
            boolean rejected = page.locator("vaadin-notification-card").filter(new Locator.FilterOptions()
                .setHasText(String.valueOf(s.expect().get("notification")))).count() > 0;
            if (dialog.count() > 0 && Vaadin.button(dialog, "Cancel").isVisible())
            {
                Vaadin.button(dialog, "Cancel").click();
            }

            Grid reloaded = openRoles(page);
            reloaded.filterBy(0, name);
            reloaded.waitForText(name);
            org.assertj.core.api.Assertions.assertThat(reloaded.visibleText())
                .as("the existing role must keep its description").contains(original);
            org.assertj.core.api.Assertions.assertThat(rejected)
                .as("a duplicate role name must be rejected with '" + s.expect().get("notification") + "'").isTrue();
        }
        finally
        {
            deleteRole(page, openRoles(page), name);
        }
    }

    @Test
    void addAPolicyToARole(Page page)
    {
        String policy = String.valueOf(Scenarios.one("admin/roles", "add a policy to a role").input().get("policy"));
        String name = uniqueName("create role");
        Grid roles = openRoles(page);
        createRole(page, roles, name, "policy test");
        try
        {
            roles.doubleClickRow(name);
            Locator profile = Vaadin.dialog(page, "Role Profile - " + name);
            Vaadin.expandPanel(profile, "Associated Policies");
            page.locator("#addPolicyButton").click();
            Locator select = Vaadin.dialog(page, "Select Policy");
            Grid available = new Grid(page.locator("#selectPolicyGrid"));
            available.filterBy(0, policy);
            available.waitForText(policy);
            available.doubleClickRow(policy);
            Vaadin.closeDialog(page, select);

            Grid assigned = new Grid(page.locator("#policyGrid"));
            assigned.waitForText(policy);

            assigned.inRow(policy, TRASH).click();
            Vaadin.waitForVaadin(page);
            assigned.waitForNoText(policy);
            Vaadin.closeDialog(page, profile);
        }
        finally
        {
            deleteRole(page, openRoles(page), name);
        }
    }

    @Test
    void addAUserToARole(Page page)
    {
        String user = String.valueOf(Scenarios.one("admin/roles", "add a user to a role").input().get("user"));
        String name = uniqueName("create role");
        Grid roles = openRoles(page);
        createRole(page, roles, name, "user test");
        try
        {
            roles.doubleClickRow(name);
            Locator profile = Vaadin.dialog(page, "Role Profile - " + name);
            Vaadin.expandPanel(profile, "Associated Users");
            Vaadin.button(profile, "Add User").click();
            Locator select = Vaadin.dialog(page, "Select User");
            Grid available = new Grid(select.locator("vaadin-grid").first());
            available.filterBy(0, user);
            available.waitForText(user);
            available.doubleClickRow(user);
            Vaadin.closeDialog(page, select);

            Grid assigned = new Grid(profile.locator("vaadin-grid").first());
            assigned.waitForText(user);

            assigned.inRow(user, TRASH).click();
            Vaadin.waitForVaadin(page);
            assigned.waitForNoText(user);
            Vaadin.closeDialog(page, profile);
        }
        finally
        {
            deleteRole(page, openRoles(page), name);
        }
    }
}
