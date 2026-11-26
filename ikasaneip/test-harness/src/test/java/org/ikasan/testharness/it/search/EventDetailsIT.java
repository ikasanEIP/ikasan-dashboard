package org.ikasan.testharness.it.search;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.support.Scenario;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The details of events found by search, and the search's exports:
 * <ul>
 *     <li>the wiretap and error dialogs: their fields, the event (and an error's details), formatting it as XML and
 *     back, downloading it and opening it in its own window; a wiretap's event id links to its event life</li>
 *     <li>exporting the results as CSV (one row per event) and as a ZIP (one JSON file per event), and the message
 *     when there is nothing to export</li>
 * </ul>
 * Read-only, against the baseline (scenarios: expectations/search/event-details.yaml).
 */
@HarnessTest
@Tag("search")
class EventDetailsIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    /** The baseline's wiretaps (EVT-A-001..008, EVT-B-001..004) and errors (EVT-ERR-001..005). */
    private static final List<String> WIRETAP_AND_ERROR_EVENT_IDS = new ArrayList<>();

    static
    {
        IntStream.rangeClosed(1, 8).forEach(i -> WIRETAP_AND_ERROR_EVENT_IDS.add(String.format("EVT-A-%03d", i)));
        IntStream.rangeClosed(1, 4).forEach(i -> WIRETAP_AND_ERROR_EVENT_IDS.add(String.format("EVT-B-%03d", i)));
        IntStream.rangeClosed(1, 5).forEach(i -> WIRETAP_AND_ERROR_EVENT_IDS.add(String.format("EVT-ERR-%03d", i)));
    }

    @Test
    @Tag("smoke")
    @SuppressWarnings("unchecked")
    void aWiretapShowsItsDetailsAndEvent(SearchPage search, Page page)
    {
        Scenario s = Scenarios.one("search/event-details", "wiretap dialog");
        Locator dialog = open(search, s);
        assertThat(dialog.getByText("Wiretap Event Details")).isVisible();
        assertFields(dialog, (Map<String, Object>) s.expect().get("fields"));
        String eventId = String.valueOf(s.expect().get("eventLifeIdLink"));
        assertThat(dialog.locator("vaadin-text-field a").filter(new Locator.FilterOptions().setHasText(eventId)).first())
            .hasAttribute("href", java.util.regex.Pattern.compile(".*eventLifeId/" + eventId + "$"));

        String payload = String.valueOf(s.expect().get("payload"));
        assertThat(Vaadin.editorText(dialog)).as("the wiretap event").isEqualTo(payload);
        assertFormatting(dialog, payload);
        assertThat(download(page, dialog, "download the wiretap event", "wiretap.txt")).as("downloaded event").isEqualTo(payload);
        assertOpensInItsOwnWindow(page, dialog, "Wiretap " + eventId, payload);
    }

    @Test
    @Tag("smoke")
    @SuppressWarnings("unchecked")
    void anErrorShowsItsDetailsAndEvent(SearchPage search, Page page)
    {
        Scenario s = Scenarios.one("search/event-details", "error dialog");
        Locator dialog = open(search, s);
        assertThat(dialog.getByText("Error Event Details")).isVisible();
        assertFields(dialog, (Map<String, Object>) s.expect().get("fields"));

        String detail = String.valueOf(s.expect().get("errorDetail"));
        String payload = String.valueOf(s.expect().get("payload"));
        assertThat(Vaadin.editorText(dialog)).as("the error's details, shown first").isEqualTo(detail);
        assertThat(download(page, dialog, "download the error event", "error.txt")).as("downloaded error details").isEqualTo(detail);

        tab(dialog, "Error Event Payload");
        assertThat(Vaadin.editorText(dialog)).as("the event that failed").isEqualTo(payload);
        assertFormatting(dialog, payload);
        tab(dialog, "Error Details");
        assertThat(Vaadin.editorText(dialog)).as("the error's details again").isEqualTo(detail);
    }

    /**
     * An exclusion shows the excluded event, and on its own tab the details of the error it was made for; each tab's
     * content is what the format selector's None goes back to.
     */
    @Test
    void anExclusionShowsItsEventAndTheErrorItWasMadeFor(SearchPage search)
    {
        long now = System.currentTimeMillis();
        search.open().search(new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of("exclusion"), "", null, null));
        Locator dialog = search.openRow("EVT-ERR-001 / err-0001", "Exclusion EVT-ERR-001");
        String payload = "<order><id>ORDER-901</id></order>";
        String detail = "java.lang.IllegalStateException: harness failure 1\n\tat org.ikasan.Harness.run(Harness.java:1)";
        assertThat(Vaadin.editorText(dialog)).as("the excluded event, shown first").isEqualTo(payload);

        tab(dialog, "Error Details");
        assertThat(Vaadin.editorText(dialog)).as("the error's details").isEqualTo(detail);
        Locator format = dialog.locator("vaadin-select").first();
        Vaadin.selectOption(format, "XML");
        Vaadin.selectOption(format, "None");
        assertThat(Vaadin.editorText(dialog)).as("the error's details, as stored").isEqualTo(detail);

        tab(dialog, "Exclusion");
        assertThat(Vaadin.editorText(dialog)).as("the excluded event again").isEqualTo(payload);
        assertFormatting(dialog, payload);
    }

    @Test
    void searchResultsExportAsCsv(SearchPage search, Page page)
    {
        search.open().search(wiretapsAndErrors());
        String csv = download(page, page.locator("body"), "export to csv file", null);
        List<List<String>> rows = parseCsv(csv);

        assertThat(rows.getFirst()).as("CSV header").containsExactly("ModuleName", "FlowName", "ComponentName", "Type",
            "ErrorMessage", "Payload", "Event Id", "Timestamp");
        List<List<String>> records = rows.subList(1, rows.size());
        assertThat(records).extracting(r -> r.get(6)).as("event ids exported").containsExactlyInAnyOrderElementsOf(WIRETAP_AND_ERROR_EVENT_IDS);
        assertThat(records).filteredOn(r -> r.get(6).equals("EVT-ERR-001")).singleElement().satisfies(r -> assertThat(r.subList(0, 7))
            .containsExactly("harness-module-a", "Order Flow", "orderProducer", "error", "harness failure 1",
                "<order><id>ORDER-901</id></order>", "EVT-ERR-001"));
        assertThat(records).filteredOn(r -> r.get(6).equals("EVT-B-001")).singleElement().satisfies(r -> assertThat(r.subList(0, 5))
            .containsExactly("harness-module-b", "Payment Flow", "paymentConsumer", "wiretap", ""));
    }

    @Test
    void searchResultsExportAsAZipOfOneJsonFilePerEvent(SearchPage search, Page page)
    {
        search.open().search(wiretapsAndErrors());
        Map<String, String> files = unzip(downloadBytes(page, page.locator("body"), "download grid items as zip file", null));

        assertThat(files).as("files in the ZIP").hasSize(WIRETAP_AND_ERROR_EVENT_IDS.size());
        for (String eventId : WIRETAP_AND_ERROR_EVENT_IDS)
        {
            assertThat(files.keySet()).as("a file named after " + eventId)
                .anyMatch(name -> name.matches(".*_(wiretap|error)_" + eventId + "_\\d+\\.json"));
        }
        String name = files.keySet().stream().filter(n -> n.contains("_error_EVT-ERR-001_")).findFirst().orElseThrow();
        assertThat(name).startsWith("harness-module-a_Order Flow_orderProducer_error_EVT-ERR-001_");
        Map<?, ?> json = JSON.readValue(files.get(name), Map.class);
        assertThat(json.values().stream().map(String::valueOf).toList()).as("the exported error").contains("harness-module-a", "Order Flow", "orderProducer",
            "<order><id>ORDER-901</id></order>", "err-0001");
    }

    @Test
    void anEmptySearchHasNothingToExport(SearchPage search, Page page)
    {
        long now = System.currentTimeMillis();
        search.open().search(new SearchInput(now - Duration.ofDays(3).toMillis(), now - Duration.ofDays(2).toMillis(),
            List.of("wiretap", "error"), "", null, null));
        assertThat(search.totalResults()).isZero();
        page.locator("[title='download grid items as zip file']").first().click();
        Vaadin.expectNotification(page, "There are no records to download!");
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static Locator open(SearchPage search, Scenario s)
    {
        long now = System.currentTimeMillis();
        search.open().search(new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of(String.valueOf(s.input().get("type"))), "", null, null));
        return search.openRow(String.valueOf(s.input().get("row")), String.valueOf(s.input().get("title")));
    }

    private static SearchInput wiretapsAndErrors()
    {
        long now = System.currentTimeMillis();
        return new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of("wiretap", "error"), "", null, null);
    }

    private static void assertFields(Locator dialog, Map<String, Object> fields)
    {
        fields.forEach((label, value) -> assertThat(Vaadin.field(dialog, label)).hasValue(String.valueOf(value)));
    }

    /** XML formats the event; JSON leaves a non-JSON event as it is; None shows it as stored again. */
    private static void assertFormatting(Locator dialog, String payload)
    {
        Locator format = dialog.locator("vaadin-select").first();
        Vaadin.selectOption(format, "XML");
        String xml = Vaadin.editorText(dialog);
        assertThat(xml).as("the event formatted as XML").contains("\n").isNotEqualTo(payload);
        assertThat(xml.replaceAll(">\\s+<", "><").replaceAll("^<\\?xml[^>]*\\?>", "").trim()).as("the same XML, laid out")
            .isEqualTo(payload);
        Vaadin.selectOption(format, "JSON");
        assertThat(Vaadin.editorText(dialog)).as("JSON formatting of an XML event").isEqualTo(xml);
        Vaadin.selectOption(format, "None");
        assertThat(Vaadin.editorText(dialog)).as("the event as stored").isEqualTo(payload);
    }

    private static void assertOpensInItsOwnWindow(Page page, Locator dialog, String title, String content)
    {
        dialog.locator("vaadin-button:has(vaadin-icon[icon='vaadin:external-link'])").first().click();
        Locator window = Vaadin.dialog(page, title);
        assertThat(Vaadin.editorText(window)).as("the event in its own window").isEqualTo(content);
    }

    private static void tab(Locator dialog, String name)
    {
        dialog.getByRole(com.microsoft.playwright.options.AriaRole.TAB, new Locator.GetByRoleOptions().setName(name)).click();
        Vaadin.waitForVaadin(dialog.page());
    }

    /** Download by the control with the title and return the file as text; check its name when given. */
    private static String download(Page page, Locator scope, String title, String fileName)
    {
        return new String(downloadBytes(page, scope, title, fileName), StandardCharsets.UTF_8);
    }

    private static byte[] downloadBytes(Page page, Locator scope, String title, String fileName)
    {
        Download download = page.waitForDownload(() -> scope.locator("[title='" + title + "']").first().click());
        if (fileName != null)
        {
            assertThat(download.suggestedFilename()).isEqualTo(fileName);
        }
        try
        {
            return Files.readAllBytes(download.path());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> unzip(byte[] zip)
    {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new java.io.ByteArrayInputStream(zip)))
        {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry())
            {
                files.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    /** RFC 4180 CSV: quoted fields may hold commas, quotes ("") and line breaks. */
    private static List<List<String>> parseCsv(String csv)
    {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++)
        {
            char c = csv.charAt(i);
            if (quoted)
            {
                if (c == '"' && i + 1 < csv.length() && csv.charAt(i + 1) == '"')
                {
                    field.append('"');
                    i++;
                }
                else if (c == '"')
                {
                    quoted = false;
                }
                else
                {
                    field.append(c);
                }
            }
            else if (c == '"')
            {
                quoted = true;
            }
            else if (c == ',')
            {
                row.add(field.toString());
                field.setLength(0);
            }
            else if (c == '\n')
            {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            }
            else if (c != '\r')
            {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty())
        {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
