package org.ikasan.testharness.it.access;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.LoginAs;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A user who may read, but not change, users, roles and policies ({@code access-admin-reader}) sees the
 * administration grids without the controls that change them. Read-only.
 */
@HarnessTest
@Tag("access")
// logging in as a persona writes its user and role to the store: run after the read-only classes (AdminGridsIT lists users)
@Tag("mutating")
@Tag("admin")
@LoginAs("access-admin-reader")
class AccessAdminIT
{
    private static final String PLUS = "vaadin-button:has(vaadin-icon[icon='vaadin:plus'])";
    private static final String TRASH = "vaadin-icon[icon='vaadin:trash']";

    @Test
    @Tag("smoke")
    void anAdministrationReaderCannotAddUsers(AdminGridPage admin, Page page)
    {
        Grid users = admin.open("userManagement");
        users.waitForText("admin");
        assertThat(Vaadin.isShown(page.locator(PLUS))).as("add user offered").isFalse();
    }

    @Test
    void anAdministrationReaderCannotAddOrDeleteRoles(AdminGridPage admin, Page page)
    {
        Grid roles = admin.open("roleManagement");
        roles.waitForText("ADMIN");
        assertThat(Vaadin.isShown(page.locator("#addRoleButton"))).as("add role offered").isFalse();
        assertThat(Vaadin.isShown(roles.root().locator(TRASH))).as("delete role offered").isFalse();
    }
}
