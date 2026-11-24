package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Every Administration menu item opens its view. Scenarios: expectations/admin/navigation.yaml. */
@HarnessTest
@Tag("admin")
class AdminNavigationIT
{
    record NavInput(String menuItem, String parent) {}

    record NavExpect(String route, String heading) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load("admin/navigation", "admin");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void menuItemOpensView(Scenario scenario, Page page, HarnessConfig config)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        NavInput in = scenario.input(NavInput.class);
        NavExpect expect = scenario.expect(NavExpect.class);

        Vaadin.gotoRoute(page, "");
        Vaadin.openMenuItem(page, in.menuItem(), in.parent());

        assertThat(page).hasURL(config.dashboard().url().replaceAll("/$", "") + "/" + expect.route());
        assertThat(page.getByText(expect.heading(), new Page.GetByTextOptions().setExact(true)).first()).isVisible();
    }
}
