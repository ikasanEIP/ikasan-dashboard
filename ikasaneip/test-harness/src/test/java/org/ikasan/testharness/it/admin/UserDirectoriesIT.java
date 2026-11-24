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

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * LDAP user directories: add, disable/enable and delete a directory, and form validation.
 * None of these contact the LDAP server (only Test and Synchronise do, which need a real server).
 * Scenarios: expectations/admin/directories.yaml.
 */
@HarnessTest
@Tag("admin")
@Tag("ldap")
@Tag("mutating")
class UserDirectoriesIT
{
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    private static Grid openDirectories(Page page)
    {
        Vaadin.gotoRoute(page, "userDirectories");
        Grid grid = new Grid(page.locator("vaadin-grid").first());
        grid.waitForLoaded();
        return grid;
    }

    private static Locator openNewDirectoryDialog(Page page)
    {
        Vaadin.button(page.locator("body"), "Add Directory").click();
        return Vaadin.dialog(page, "Configure LDAP");
    }

    @Test
    void directoryLifecycle(Page page)
    {
        Scenario s = Scenarios.one("admin/directories", "directory lifecycle");
        Map<String, Object> in = s.input();
        Map<String, Object> expect = s.expect();
        String name = in.get("directoryName") + "-" + SUFFIX;
        Grid grid = openDirectories(page);

        Locator dialog = openNewDirectoryDialog(page);
        Vaadin.fillField(Vaadin.field(dialog, "Directory Name"), name);
        Vaadin.fillField(Vaadin.field(dialog, "LDAP Server URL"), String.valueOf(in.get("serverUrl")));
        Vaadin.fillField(Vaadin.field(dialog, "Username"), String.valueOf(in.get("username")));
        Vaadin.fillField(Vaadin.field(dialog, "Password"), String.valueOf(in.get("password")));
        Vaadin.fillField(Vaadin.field(dialog, "User DN"), String.valueOf(in.get("userDn")));
        Vaadin.fillField(Vaadin.field(dialog, "Group DN"), String.valueOf(in.get("groupDn")));
        // fills every LDAP attribute field with the defaults
        dialog.getByText("Populate default attributes", new Locator.GetByTextOptions().setExact(true)).click();
        Vaadin.waitForVaadin(page);
        Vaadin.button(dialog, "Save").click();

        Vaadin.expectNotification(page, String.valueOf(expect.get("saved")));
        assertThat(dialog).isHidden();
        try
        {
            grid.waitForText(name);

            grid.inRowButton(name, "Disable").click();
            Vaadin.expectNotification(page, String.valueOf(expect.get("disabled")));
            grid.inRowButton(name, "Enable").click();
            Vaadin.expectNotification(page, String.valueOf(expect.get("enabled")));
        }
        finally
        {
            grid.inRowButton(name, "Delete").click();
            Vaadin.expectNotification(page, String.valueOf(expect.get("deleted")));
            grid.waitForNoText(name);
        }
    }

    @Test
    void directoryFormIsValidated(Page page)
    {
        Scenario s = Scenarios.one("admin/directories", "directory validation");
        openDirectories(page);
        Locator dialog = openNewDirectoryDialog(page);

        Vaadin.button(dialog, "Save").click();

        Vaadin.expectNotification(page, String.valueOf(s.expect().get("notification")));
        assertThat(Vaadin.button(dialog, "Save")).isVisible();
        Vaadin.button(dialog, "Cancel").click();
        assertThat(dialog).isHidden();
    }
}
