package org.ikasan.testharness.it.admin;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit: system event search, filtering, the event details dialog and CSV export.
 * The harness's own logins create system events, so today's search always has data.
 * Scenarios: expectations/admin/audit.yaml.
 */
@HarnessTest
@Tag("admin")
@Tag("audit")
class AuditIT
{
    private static final Pattern RECORDS = Pattern.compile("Number of records returned: (\\d+)");

    /**
     * Open the audit view and run the default search (today 00:00 to tomorrow 00:00); returns the grid. The run's own
     * login is today's first system event and is indexed shortly after it is written, so the search is repeated until
     * it finds something.
     */
    private static Grid searchToday(Page page)
    {
        Vaadin.gotoRoute(page, "adminSearchView");
        long deadline = System.nanoTime() + Timeouts.seconds(30).toNanos();
        do
        {
            page.locator("#systemEventSearchFormSearchButton").click();
            Vaadin.waitForVaadin(page);
            assertThat(page.getByText(RECORDS).first()).isVisible();
            if (!page.getByText(RECORDS).first().innerText().matches(".*returned: 0 .*"))
            {
                break;
            }
            page.waitForTimeout(500);
        }
        while (System.nanoTime() < deadline);
        Grid grid = new Grid(page.locator("#identifier vaadin-grid").first());
        grid.waitForLoaded();
        return grid;
    }

    @Test
    @Tag("smoke")
    void searchAndFilterTodaysSystemEvents(Page page)
    {
        Scenario s = Scenarios.one("admin/audit", "search today's system events");
        Grid grid = searchToday(page);

        Matcher m = RECORDS.matcher(page.getByText(RECORDS).first().textContent());
        assertThat(m.find()).isTrue();
        assertThat(Integer.parseInt(m.group(1))).isGreaterThanOrEqualTo((Integer) s.expect().get("minRecords"));

        Vaadin.fillField(page.locator("vaadin-text-field#actor"), String.valueOf(s.expect().get("actorFilter")));
        Vaadin.waitForVaadin(page);
        GridRows.assertRows(grid.rows(), GridRows.expectedRows(s.expect()), Boolean.TRUE.equals(s.expect().get("rowsExact")),
            "system events filtered by actor '" + s.expect().get("actorFilter") + "'");
    }

    @Test
    void openSystemEventDetails(Page page)
    {
        Scenario s = Scenarios.one("admin/audit", "system event details");
        Grid grid = searchToday(page);

        grid.doubleClickRow("admin");

        Locator dialog = Vaadin.dialog(page, String.valueOf(s.expect().get("dialogTitle")));
        Vaadin.closeDialog(page, dialog);
    }

    @Test
    void exportSystemEventsToCsv(Page page)
    {
        Scenario s = Scenarios.one("admin/audit", "export system events to csv");
        searchToday(page);

        Download download = page.waitForDownload(() -> page.locator("vaadin-button[title='export to csv file']").click());

        assertThat(download.suggestedFilename()).matches(String.valueOf(s.expect().get("downloadName")));
    }
}
