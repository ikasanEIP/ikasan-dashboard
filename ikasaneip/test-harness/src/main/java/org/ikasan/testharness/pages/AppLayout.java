package org.ikasan.testharness.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Vaadin;

import java.util.regex.Pattern;

/** The IkasanAppLayout shell: header buttons and the side menu. */
public class AppLayout
{
    private final Page page;

    public AppLayout(Page page)
    {
        this.page = page;
    }

    public Locator logoutButton()
    {
        return page.locator("#logoutButton");
    }

    public Locator aboutButton()
    {
        return page.locator("#aboutButton");
    }

    /** Menu item ids are set in IkasanAppLayout, e.g. searchMenuItem, schedulerMenuItem, userManagementMenuItem. */
    public void navigate(String menuItemId)
    {
        Vaadin.openMenuItem(page, menuItemId);
    }

    public void logout()
    {
        logoutButton().click();
        page.waitForURL(Pattern.compile(".*/login.*"));
        Vaadin.waitForVaadin(page);
    }
}
