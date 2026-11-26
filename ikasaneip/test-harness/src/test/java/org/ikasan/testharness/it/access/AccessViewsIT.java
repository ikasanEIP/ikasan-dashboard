package org.ikasan.testharness.it.access;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.LoginAs;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which views users with narrower access than admin may open, and whether the side menu offers exactly those.
 * For every persona ({@code data/personas.yaml}) and every view that checks authorities on entry
 * ({@code expectations/access/views.yaml}):
 * <ul>
 *     <li>the view's menu item is shown if and only if the persona may open the view</li>
 *     <li>a deep link to the view opens it if the persona may, and otherwise lands elsewhere</li>
 * </ul>
 * Every mismatch of a persona is reported together.
 */
@HarnessTest
@Tag("access")
// logging in as a persona writes its user and role to the store: run after the read-only classes (AdminGridsIT lists users)
@Tag("mutating")
class AccessViewsIT
{
    record Marker(String text, String selector) {}

    record View(String route, String menuItem, Marker marker) {}

    record Spec(List<View> views) {}

    private static final Spec SPEC = load();

    private static Spec load()
    {
        try
        {
            return YAMLMapper.builder().configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build().readValue(Files.readString(ConfigLoader.resolve("expectations/access/views.yaml")), Spec.class);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @Tag("smoke")
    @LoginAs("access-reader")
    void aReaderOpensSearchAndTheSchedulerButNoAdministration(Page page)
    {
        check(page, "access-reader");
    }

    @Test
    @LoginAs("access-operator")
    void anOperatorOpensSearchAndTheSchedulerButNoAdministration(Page page)
    {
        check(page, "access-operator");
    }

    @Test
    @LoginAs("access-wiretaps")
    void aWiretapSearcherOpensOnlyTheDashboardAndSearch(Page page)
    {
        check(page, "access-wiretaps");
    }

    @Test
    @LoginAs("access-module-a")
    void aModuleScopedSearcherOpensOnlyTheDashboardAndSearch(Page page)
    {
        check(page, "access-module-a");
    }

    @Test
    @LoginAs("access-admin-reader")
    void anAdministrationReaderOpensTheAdministrationViewsButNotSearchOrTheScheduler(Page page)
    {
        check(page, "access-admin-reader");
    }

    @Test
    @LoginAs("access-search-writer")
    void aSearchWriterIsOfferedSearchAndMayOpenIt(Page page)
    {
        check(page, "access-search-writer");
    }

    /** A user with neither the dashboard nor the scheduler to land on is told so and may log out. */
    @Test
    @LoginAs("access-nothing")
    void aUserWithoutAnyLandingPageIsToldAndMayLogOut(Page page)
    {
        Vaadin.gotoRoute(page, "");
        assertThat(page.locator("#noAccessMessage")).containsText("You are logged in as [access-nothing]");
        page.locator("#noAccessLogoutButton").click();
        assertThat(new org.ikasan.testharness.pages.LoginPage(page).form()).isVisible();
    }

    private static void check(Page page, String persona)
    {
        @SuppressWarnings("unchecked")
        List<String> allowed = (List<String>) Scenarios.one("access/views", persona).expect().get("views");
        List<String> faults = new ArrayList<>();

        Vaadin.gotoRoute(page, "");
        for (View view : SPEC.views())
        {
            if (view.menuItem() == null)
            {
                continue;
            }
            boolean shown = Vaadin.isShown(page.locator("#" + view.menuItem()));
            boolean mayOpen = allowed.contains(view.route());
            if (shown != mayOpen)
            {
                faults.add("menu item " + view.menuItem() + (shown ? " is shown, but " + persona + " may not open '"
                    + view.route() + "'" : " is hidden, but " + persona + " may open '" + view.route() + "'"));
            }
        }

        for (View view : SPEC.views())
        {
            Vaadin.gotoRoute(page, view.route());
            boolean mayOpen = allowed.contains(view.route());
            try
            {
                if (mayOpen)
                {
                    assertThat(marker(page, view)).isVisible();
                }
                else
                {
                    assertThat(marker(page, view)).isHidden();
                }
            }
            catch (AssertionFailedError e)
            {
                faults.add("deep link '/" + view.route() + "' " + (mayOpen ? "did not open the view" : "opened the view, but "
                    + persona + " may not"));
            }
        }

        assertThat(faults).as("access faults for " + persona).isEmpty();
    }

    /** The view's marker, among visible elements only (a menu item may carry the same text as a heading). */
    private static Locator marker(Page page, View view)
    {
        Locator marker = view.marker().selector() != null ? page.locator(view.marker().selector())
            : page.getByText(view.marker().text(), new Page.GetByTextOptions().setExact(true));
        return marker.and(page.locator(":visible")).first();
    }
}
