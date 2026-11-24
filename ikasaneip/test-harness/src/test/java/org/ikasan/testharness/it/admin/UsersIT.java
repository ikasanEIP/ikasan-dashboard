package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * User management: create a user, field validation, default role, adding and removing roles.
 * Scenarios: expectations/admin/users.yaml. Users cannot be edited or deleted in the dashboard, so each run
 * creates uniquely named users (the baseline is re-seeded on the next run).
 */
@HarnessTest
@Tag("admin")
@Tag("users")
@Tag("mutating")
class UsersIT
{
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    /** Create a user through the New User dialog and wait for it in the grid; returns the grid. */
    static Grid createUser(AdminGridPage admin, Page page, Map<String, Object> in, String username)
    {
        Grid grid = admin.open("userManagement");
        page.locator("vaadin-button:has(vaadin-icon[icon='vaadin:plus'])").first().click();
        Locator dialog = Vaadin.dialog(page, "New User");
        Vaadin.fillField(Vaadin.field(dialog, "Username"), username);
        Vaadin.fillField(Vaadin.field(dialog, "First Name"), String.valueOf(in.get("firstName")));
        Vaadin.fillField(Vaadin.field(dialog, "Surname"), String.valueOf(in.get("surname")));
        Vaadin.fillField(Vaadin.field(dialog, "Email"), String.valueOf(in.get("email")));
        Vaadin.fillField(Vaadin.field(dialog, "Password"), String.valueOf(in.get("password")));
        Vaadin.button(dialog, "Save").click();
        Vaadin.waitForVaadin(page);
        assertThat(dialog).isHidden();

        grid.filterBy(0, username);
        grid.waitForText(username);
        return grid;
    }

    /** Open the user's profile and expand its roles panel; returns the roles grid. */
    static Grid openRoles(Page page, Grid users, String username)
    {
        users.doubleClickRow(username);
        Locator profile = Vaadin.dialog(page, "User Profile - " + username);
        Vaadin.expandPanel(profile, "Ikasan Roles");
        return new Grid(profile.locator("vaadin-grid").first());
    }

    @Test
    void createUser(AdminGridPage admin, Page page)
    {
        Scenario s = Scenarios.one("admin/users", "create user");
        String username = s.input().get("username") + "-" + SUFFIX;

        Grid users = createUser(admin, page, s.input(), username);

        Grid roles = openRoles(page, users, username);
        @SuppressWarnings("unchecked")
        List<String> defaultRoles = (List<String>) s.expect().get("defaultRoles");
        defaultRoles.forEach(roles::waitForText);
    }

    @Test
    void newUserFieldsAreValidated(Page page, AdminGridPage admin)
    {
        Scenario s = Scenarios.one("admin/users", "new user validation");
        admin.open("userManagement");
        page.locator("vaadin-button:has(vaadin-icon[icon='vaadin:plus'])").first().click();
        Locator dialog = Vaadin.dialog(page, "New User");

        Vaadin.button(dialog, "Save").click();
        Vaadin.waitForVaadin(page);

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) s.expect().get("errors");
        for (String error : errors)
        {
            assertThat(dialog.getByText(error).first()).isVisible();
        }
        Vaadin.button(dialog, "Cancel").click();
        assertThat(dialog).isHidden();
    }

    @Test
    void addAndRemoveARole(AdminGridPage admin, Page page)
    {
        Scenario create = Scenarios.one("admin/users", "create user");
        String role = String.valueOf(Scenarios.one("admin/users", "add and remove a role").input().get("role"));
        String username = create.input().get("username") + "-roles-" + SUFFIX;
        Grid users = createUser(admin, page, create.input(), username);
        Grid roles = openRoles(page, users, username);

        Locator profile = Vaadin.dialog(page, "User Profile - " + username);
        Vaadin.button(profile, "Add Role").click();
        Locator select = Vaadin.dialog(page, "Select role");
        Grid available = new Grid(select.locator("vaadin-grid").first());
        available.filterBy(0, role);
        available.waitForText(role);
        available.doubleClickRow(role);
        Vaadin.closeDialog(page, select);

        roles.waitForText(role);

        roles.inRow(role, "vaadin-icon[icon='vaadin:trash']").click();
        Vaadin.waitForVaadin(page);
        roles.waitForNoText(role);
    }

    /**
     * Regression: on Solr the users grid filter used to query name:*text* on a tokenised field, so a username
     * containing a separator ('-', '@', ...) never matched its full name (fixed in SolrUserDaoImpl.containsClause).
     */
    @Test
    void filterUsersByFullUsername(AdminGridPage admin, Page page)
    {
        Scenario s = Scenarios.one("admin/users", "filter users by full username");
        String username = s.input().get("username") + "-" + SUFFIX;
        createUser(admin, page, Scenarios.one("admin/users", "create user").input(), username);

        Grid grid = admin.open("userManagement");
        grid.filterBy(0, username);
        grid.waitForText(username);
    }

    /** Creating a user with an existing username must fail and leave the existing user untouched. */
    @Test
    void duplicateUsernameIsRejected(AdminGridPage admin, Page page)
    {
        Scenario s = Scenarios.one("admin/users", "duplicate username is rejected");
        String username = s.input().get("username") + "-" + SUFFIX;
        Map<String, Object> original = new java.util.HashMap<>(Scenarios.one("admin/users", "create user").input());
        original.put("firstName", s.input().get("firstName"));
        createUser(admin, page, original, username);

        page.locator("vaadin-button:has(vaadin-icon[icon='vaadin:plus'])").first().click();
        Locator dialog = Vaadin.dialog(page, "New User");
        Vaadin.fillField(Vaadin.field(dialog, "Username"), username);
        Vaadin.fillField(Vaadin.field(dialog, "First Name"), String.valueOf(s.input().get("duplicateFirstName")));
        Vaadin.fillField(Vaadin.field(dialog, "Surname"), String.valueOf(original.get("surname")));
        Vaadin.fillField(Vaadin.field(dialog, "Email"), String.valueOf(original.get("email")));
        Vaadin.fillField(Vaadin.field(dialog, "Password"), String.valueOf(original.get("password")));
        Vaadin.button(dialog, "Save").click();

        Vaadin.expectNotification(page, String.valueOf(s.expect().get("notification")));
        Vaadin.button(dialog, "Cancel").click();

        Grid users = admin.open("userManagement");
        users.filterBy(0, username);
        users.waitForText(username);
        org.assertj.core.api.Assertions.assertThat(users.visibleText())
            .as("the existing user must keep its first name")
            .contains(String.valueOf(s.input().get("firstName")))
            .doesNotContain(String.valueOf(s.input().get("duplicateFirstName")));
    }
}
