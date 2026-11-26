package org.ikasan.testharness.it.search;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Locator;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An event whose payload is JSON is laid out by the dialog's JSON format, and shown as stored again with None.
 * Mutating: the baseline's events are XML, so the test adds a JSON wiretap (and removes it).
 */
@HarnessTest
@Tag("search")
@Tag("wiretap")
@Tag("mutating")
class JsonEventDetailsIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ID = "harness-module-a-wiretap-EVT-JSON-001";
    private static final String PAYLOAD = "{\"order\":{\"id\":\"ORDER-JSON-1\",\"lines\":[{\"sku\":\"A1\",\"qty\":2}]}}";

    @Test
    void aJsonEventIsLaidOutAsJson(SearchPage search, HarnessSession session)
    {
        long now = System.currentTimeMillis();
        Map<String, Object> wiretap = new LinkedHashMap<>();
        wiretap.put("id", ID);
        wiretap.put("type", "wiretap");
        wiretap.put("moduleName", "harness-module-a");
        wiretap.put("flowName", "Order Flow");
        wiretap.put("componentName", "orderConsumer");
        wiretap.put("event", "EVT-JSON-001");
        wiretap.put("relatedEventId", "EVT-JSON-001");
        wiretap.put("payload", PAYLOAD);
        wiretap.put("timestamp", now - Duration.ofMinutes(2).toMillis());
        wiretap.put("harvestReceivedTimestamp", now - Duration.ofMinutes(2).toMillis());
        wiretap.put("expiry", now + Duration.ofDays(30).toMillis());
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.load(List.of(wiretap));
        }
        try
        {
            SearchInput wiretaps = new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
                List.of("wiretap"), "", null, null);
            search.open().searchUntil(wiretaps, s -> s.grid().visibleText().contains("EVT-JSON-001"), Timeouts.seconds(30),
                "the JSON wiretap");
            Locator dialog = openJsonWiretap(search);
            assertThat(Vaadin.editorText(dialog)).isEqualTo(PAYLOAD);

            Locator format = dialog.locator("vaadin-select").first();
            Vaadin.selectOption(format, "JSON");
            String formatted = Vaadin.editorText(dialog);
            assertThat(formatted).as("the event laid out as JSON").contains("\n").isNotEqualTo(PAYLOAD);
            assertThat(JSON.readTree(formatted)).as("the same JSON").isEqualTo(JSON.readTree(PAYLOAD));

            Vaadin.selectOption(format, "None");
            assertThat(Vaadin.editorText(dialog)).as("the event as stored").isEqualTo(PAYLOAD);
        }
        finally
        {
            try (Seeder seeder = Seeder.create(session.config()))
            {
                seeder.delete(List.of(ID));
            }
        }
    }

    /** Double-click the JSON wiretap's row on its timestamp: the event id cell is a link. */
    private static Locator openJsonWiretap(SearchPage search)
    {
        String timestamp = search.grid().rows().stream()
            .filter(r -> String.valueOf(r.get("Event Id / Error URI")).contains("EVT-JSON-001"))
            .map(r -> r.get("Timestamp")).findFirst().orElseThrow(() -> new AssertionError("no row for the JSON wiretap"));
        return search.openRow(timestamp, "Wiretap EVT-JSON-001");
    }
}
