package org.ikasan.testharness.it.admin;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Support downloads: each widget either downloads a file or reports why it cannot (missing log directory,
 * no module selected). Which happens depends on the environment, so it is set in expectations/admin/support.yaml.
 */
@HarnessTest
@Tag("admin")
@Tag("support")
class SupportIT
{
    /** @param widget the widget's heading */
    record SupportInput(String widget) {}

    /**
     * @param download     regex of the downloaded file name
     * @param notification text of the notification shown instead of a download
     * @param fieldError   validation message shown on the widget's field
     */
    record SupportExpect(String download, String notification, String fieldError) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load("admin/support", "admin", "support");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void download(Scenario scenario, Page page)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        SupportInput in = scenario.input(SupportInput.class);
        SupportExpect expect = scenario.expect(SupportExpect.class);
        Vaadin.gotoRoute(page, "adminSupportView");
        Locator widget = page.locator("vaadin-dashboard-widget").filter(new Locator.FilterOptions().setHasText(in.widget()));
        Locator button = Vaadin.button(widget, "Download");

        if (expect.download() != null)
        {
            Download download = page.waitForDownload(button::click);
            assertThat(download.suggestedFilename()).matches(expect.download());
        }
        else
        {
            button.click();
            Vaadin.waitForVaadin(page);
            if (expect.notification() != null)
            {
                Vaadin.expectNotification(page, expect.notification());
            }
            if (expect.fieldError() != null)
            {
                assertThat(widget.getByText(expect.fieldError()).first()).isVisible();
            }
        }
    }
}
