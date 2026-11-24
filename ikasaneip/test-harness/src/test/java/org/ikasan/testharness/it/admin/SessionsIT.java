package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.pages.LoginPage;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Session management: a second, separate browser logs in; the admin opens its session details and ends it,
 * and the second browser is then logged out. Scenarios: expectations/admin/sessions.yaml.
 * <p>
 * Every other test shares one logged-in session, so this test only ever ends the session it created.
 */
@HarnessTest
@Tag("admin")
@Tag("sessions")
@Tag("mutating")
class SessionsIT
{
    /** A second, separate browser login; returns its http session id (JSESSIONID). */
    private static String loginElsewhere(BrowserContext other, HarnessConfig config)
    {
        Page otherPage = other.newPage();
        new LoginPage(otherPage).open().login(config.dashboard().username(), config.dashboard().password());
        otherPage.waitForURL(url -> !url.contains("/login"));
        return other.cookies().stream().filter(c -> c.name.equals("JSESSIONID"))
            .map((Cookie c) -> c.value).findFirst().orElseThrow();
    }

    private static Grid sessionsFilteredTo(AdminGridPage admin, String sessionId)
    {
        Grid grid = admin.open("sessionsView");
        grid.filterBy(0, sessionId);
        grid.waitForText(sessionId);
        return grid;
    }

    @Test
    void endAnotherUserSession(Page page, AdminGridPage admin, HarnessSession session, HarnessConfig config)
    {
        Scenario s = Scenarios.one("admin/sessions", "end another user session");
        try (BrowserContext other = session.newContext(null))
        {
            String sessionId = loginElsewhere(other, config);
            Grid grid = sessionsFilteredTo(admin, sessionId);

            grid.inRowButton(sessionId, "End Session").click();
            Vaadin.confirm(page);
            Vaadin.expectNotification(page, String.valueOf(s.expect().get("ended")));

            // '/' is permitAll at the http level; Vaadin reroutes an unauthenticated UI to the login view in place,
            // so the URL can stay '/' - check that the login form is shown instead
            Page otherPage = other.pages().get(0);
            otherPage.reload();
            assertThat(new LoginPage(otherPage).form()).isVisible();
        }
    }

    /**
     * The details of another user's session, including its locale. Vaadin 25 only lets a session read its own locale,
     * so the dashboard records it in the HTTP session for others to read (SessionLocale).
     */
    @Test
    void showAnotherUserSessionDetails(Page page, AdminGridPage admin, HarnessSession session, HarnessConfig config)
    {
        Scenario s = Scenarios.one("admin/sessions", "end another user session");
        try (BrowserContext other = session.newContext(null))
        {
            String sessionId = loginElsewhere(other, config);
            Grid grid = sessionsFilteredTo(admin, sessionId);

            // the session id cell is a button that opens the session details
            grid.inRowButton(sessionId, sessionId).click();
            Locator details = Vaadin.dialog(page, String.valueOf(s.expect().get("detailsLabel")));
            @SuppressWarnings("unchecked")
            java.util.List<String> values = (java.util.List<String>) details.locator("vaadin-text-field input")
                .evaluateAll("els => els.map(e => e.value)");
            org.assertj.core.api.Assertions.assertThat(values).as("session details fields")
                .contains(sessionId, String.valueOf(s.expect().get("locale")));
            Vaadin.closeDialog(page, details);
        }
    }
}
