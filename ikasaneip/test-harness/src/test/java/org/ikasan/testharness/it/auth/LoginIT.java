package org.ikasan.testharness.it.auth;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.junit.Unauthenticated;
import org.ikasan.testharness.pages.AppLayout;
import org.ikasan.testharness.pages.LoginPage;
import org.ikasan.testharness.support.Scenario;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/** Login, logout and deep links. Scenarios: expectations/auth/login.yaml. */
@HarnessTest
@Tag("auth")
@Unauthenticated
class LoginIT
{
    /** 'configured' in the expectations means: the dashboard credentials from the harness config. */
    record LoginInput(String username, String password) {}

    /** @param landingRoute route the user lands on, e.g. "" (dashboard) */
    record LoginExpect(boolean success, String landingRoute) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load("auth/login", "auth");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void login(Scenario scenario, LoginPage loginPage, Page page, HarnessConfig config)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        LoginInput in = scenario.input(LoginInput.class);
        LoginExpect expect = scenario.expect(LoginExpect.class);
        String username = "configured".equals(in.username()) ? config.dashboard().username() : in.username();
        String password = "configured".equals(in.password()) ? config.dashboard().password() : in.password();

        loginPage.open().login(username, password);

        if (expect.success())
        {
            page.waitForURL(url -> !url.contains("/login"));
            if (expect.landingRoute() != null)
            {
                assertThat(page).hasURL(config.dashboard().url().replaceAll("/$", "") + "/" + expect.landingRoute());
            }
        }
        else
        {
            assertThat(loginPage.hasError()).isTrue();
            assertThat(page).hasURL(Pattern.compile(".*/login.*"));
        }
    }

    @Test
    void logoutReturnsToTheLoginPage(LoginPage loginPage, AppLayout layout, Page page, HarnessConfig config)
    {
        loginPage.open().login(config.dashboard().username(), config.dashboard().password());
        page.waitForURL(url -> !url.contains("/login"));

        layout.logout();

        assertThat(loginPage.form()).isVisible();
    }

    @Test
    @Tag("smoke")
    void deepLinkToAParameterisedRouteIsRestoredAfterLogin(LoginPage loginPage, Page page, HarnessConfig config)
    {
        page.navigate("/eventLifeId/EVT-A-001");
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));

        loginPage.login(config.dashboard().username(), config.dashboard().password());

        assertThat(page).hasURL(Pattern.compile(".*/eventLifeId/EVT-A-001$"));
    }

    @Test
    @Tag("smoke")
    void deepLinkToAPlainRouteIsRestoredAfterLogin(LoginPage loginPage, Page page, HarnessConfig config)
    {
        page.navigate("/Search");
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));

        loginPage.login(config.dashboard().username(), config.dashboard().password());

        assertThat(page).hasURL(Pattern.compile(".*/Search$"));
    }
}
