package org.ikasan.testharness.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Vaadin;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** LoginView (route 'login'): a vaadin-login-form with id 'loginForm'. */
public class LoginPage
{
    private final Page page;
    private final Locator form;

    public LoginPage(Page page)
    {
        this.page = page;
        this.form = page.locator("#loginForm");
    }

    public Locator form()
    {
        return form;
    }

    public Locator languageSelect()
    {
        return page.locator("#loginLanguageSelect");
    }

    public LoginPage open()
    {
        page.navigate("/login");
        assertThat(form).isVisible();
        return this;
    }

    public void login(String username, String password)
    {
        form.locator("input[name='username']").fill(username);
        Locator pwd = form.locator("input[name='password']");
        pwd.fill(password);
        pwd.press("Enter");
        Vaadin.waitForVaadin(page);
    }

    /** vaadin-login-form shows its error message when authentication fails. */
    public boolean hasError()
    {
        page.waitForFunction("f => !!f.error", form.elementHandle());
        return Boolean.TRUE.equals(form.evaluate("f => !!f.error"));
    }
}
