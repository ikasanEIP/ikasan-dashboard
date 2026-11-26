package org.ikasan.testharness.it.ingest;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.module.FakeIkasanModule;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Personas;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data sent to the dashboard the way Ikasan modules send it, over REST ({@code PUT /rest/harvest/...}), arrives where
 * users look for it: wiretaps, errors, exclusions and replay events in search (and their dialogs), system events in
 * the audit view. A harvested replay event replays the exact bytes the module sent, and an exclusion links to the
 * error it was made for. The endpoints refuse malformed data, callers without a token and users without REST access.
 * <p>
 * Mutating: everything sent belongs to module {@code ingest-module}, whose documents are removed before and after.
 */
@HarnessTest
@Tag("ingest")
@Tag("search")
@Tag("mutating")
class IngestionIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration WAIT = Timeouts.seconds(30);
    private static final String MODULE = "ingest-module";
    private static final String FLOW = "Ingest Flow";
    private static final List<String> TYPES = List.of("wiretap", "error", "exclusion", "exclusionEventAction", "replay", "systemEvent");

    private static FakeIkasanModule target;

    @BeforeAll
    static void start(HarnessSession session)
    {
        removeModuleDocuments(session);
        HarnessConfig config = session.config();
        target = new FakeIkasanModule(config.scheduler().fakeAgentHost(), FakeIkasanModule.configuredPort(),
            config.dashboard().restUsername(), config.dashboard().restPassword()).start();
    }

    @AfterAll
    static void stop(HarnessSession session)
    {
        if (target != null)
        {
            target.close();
        }
        removeModuleDocuments(session);
    }

    @Test
    @Tag("smoke")
    void wiretapsSentByAModuleCanBeSearchedAndOpened(DashboardApi dashboard, SearchPage search)
    {
        long now = System.currentTimeMillis();
        harvest(dashboard, "wiretaps", List.of(
            wiretap(9100001, "ING-W-001", "ingestConsumer", "<order><id>ING-1</id></order>", now - 60_000),
            wiretap(9100002, "ING-W-002", "ingestProducer", "<order><id>ING-2</id></order>", now - 30_000)));

        Grid grid = searchModule(search, "wiretap", 2);
        assertThat(grid.rows()).extracting(r -> List.of(r.get("Module Name"), r.get("Flow Name"), r.get("Component Name"),
                r.get("Event Id / Error URI")))
            .containsExactlyInAnyOrder(List.of(MODULE, FLOW, "ingestConsumer", "ING-W-001"), List.of(MODULE, FLOW, "ingestProducer", "ING-W-002"));

        String timestamp = rowTimestamp(grid, "ING-W-001");
        Locator dialog = search.openRow(timestamp, "Wiretap ING-W-001");
        assertThat(Vaadin.field(dialog, "Component Name")).hasValue("ingestConsumer");
        assertThat(Vaadin.editorText(dialog)).as("the wiretapped event").isEqualTo("<order><id>ING-1</id></order>");
    }

    @Test
    void anErrorAndTheExclusionMadeForItCanBeSearchedAndOpened(DashboardApi dashboard, SearchPage search)
    {
        long now = System.currentTimeMillis();
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("uri", "ING-ERR-001");
        error.put("moduleName", MODULE);
        error.put("flowName", FLOW);
        error.put("flowElementName", "ingestProducer");
        error.put("errorDetail", "java.lang.IllegalArgumentException: ingest failure\n\tat org.ikasan.Ingest.run(Ingest.java:1)");
        error.put("errorMessage", "ingest failure");
        error.put("exceptionClass", "java.lang.IllegalArgumentException");
        error.put("eventLifeIdentifier", "ING-E-001");
        error.put("eventRelatedIdentifier", "ING-E-001");
        error.put("action", "ExcludeEvent");
        error.put("event", base64("<order><id>ING-E</id></order>"));
        error.put("eventAsString", "<order><id>ING-E</id></order>");
        error.put("timestamp", now - 60_000);
        error.put("expiry", now + Duration.ofDays(1).toMillis());
        harvest(dashboard, "errors", List.of(error));

        Map<String, Object> exclusion = new LinkedHashMap<>();
        exclusion.put("moduleName", MODULE);
        exclusion.put("flowName", FLOW);
        exclusion.put("identifier", "ING-E-001");
        exclusion.put("event", base64("<order><id>ING-E</id></order>"));
        exclusion.put("errorUri", "ING-ERR-001");
        exclusion.put("timestamp", now - 59_000);
        harvest(dashboard, "exclusions", List.of(exclusion));

        Grid errors = searchModule(search, "error", 1);
        assertThat(errors.rows().getFirst()).containsEntry("Component Name", "ingestProducer")
            .containsEntry("Event Id / Error URI", "ING-E-001 / ING-ERR-001");
        Locator errorDialog = search.openRow("ING-E-001 / ING-ERR-001", "Error ING-ERR-001");
        assertThat(Vaadin.field(errorDialog, "Error Action")).hasValue("ExcludeEvent");
        assertThat(Vaadin.field(errorDialog, "Exception Class")).hasValue("java.lang.IllegalArgumentException");
        assertThat(Vaadin.editorText(errorDialog)).as("the error's details").startsWith("java.lang.IllegalArgumentException: ingest failure");
        Vaadin.closeDialog(search.grid().root().page(), errorDialog);

        Grid exclusions = searchModule(search, "exclusion", 1);
        assertThat(exclusions.rows().getFirst()).containsEntry("Event Id / Error URI", "ING-E-001 / ING-ERR-001");
        // the exclusion dialog finds the error it was made for by its error uri
        Locator exclusionDialog = search.openRow("ING-E-001 / ING-ERR-001", "Exclusion ING-E-001");
        assertThat(Vaadin.field(exclusionDialog, "Error Action")).hasValue("ExcludeEvent");
        assertThat(Vaadin.editorText(exclusionDialog)).as("the excluded event").isEqualTo("<order><id>ING-E</id></order>");
    }

    @Test
    void aHarvestedReplayEventReplaysTheBytesTheModuleSent(DashboardApi dashboard, SearchPage search, Page page)
    {
        long now = System.currentTimeMillis();
        String payload = "<order><id>ING-R</id><note>café</note></order>";
        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("moduleName", MODULE);
        replay.put("flowName", FLOW);
        replay.put("eventId", "ING-R-001");
        replay.put("event", base64(payload));
        replay.put("eventAsString", payload);
        replay.put("timestamp", now - 60_000);
        replay.put("expiry", now + Duration.ofDays(1).toMillis());
        harvest(dashboard, "replay", List.of(replay));

        searchModule(search, "replay", 1);
        Locator dialog = search.openRow("ING-R-001", "Replay ING-R-001");
        assertThat(Vaadin.editorText(dialog)).as("the replay event").isEqualTo(payload);

        target.reset();
        Vaadin.button(dialog, "Replay").click();
        Locator form = Vaadin.dialog(page, "Target Module URL");
        HarnessConfig config = HarnessSession.current().config();
        Vaadin.fillField(Vaadin.field(form, "Target Module URL"), target.url(MODULE));
        Vaadin.fillField(Vaadin.field(form, "username"), config.dashboard().restUsername());
        Vaadin.fillField(Vaadin.field(form, "password"), config.dashboard().restPassword());
        Vaadin.fillField(Vaadin.field(form, "Comment"), "replaying a harvested event");
        Vaadin.button(form, "Replay").click();

        Vaadin.expectNotification(page, "replay complete");
        FakeIkasanModule.Call call = target.awaitCalls(c -> c.path().endsWith("/rest/replay"), 1, WAIT).getFirst();
        assertThat(new String(Base64.getDecoder().decode(String.valueOf(call.json().get("event"))), StandardCharsets.UTF_8))
            .as("the bytes replayed").isEqualTo(payload);
        assertThat(call.json()).containsEntry("moduleName", MODULE).containsEntry("flowName", FLOW);
    }

    @Test
    void systemEventsSentByAModuleAreAudited(DashboardApi dashboard, Page page)
    {
        harvest(dashboard, "systemevents", List.of(systemEvent(9100001, "modulebot")));
        assertThat(auditedBy(page, "modulebot")).singleElement().satisfies(r -> assertThat(r)
            .containsEntry("Action Performed By", "modulebot")
            .containsEntry("System Event Context", "Ingest Flow")
            .containsEntry("System Event", "Flow stopped by modulebot"));
    }

    /**
     * The audit view's actor filter finds actors whose names hold characters other than letters and digits, as
     * service accounts' and people's names often do (svc-orders, john.smith).
     */
    @Test
    void theAuditActorFilterFindsNamesWithAHyphen(DashboardApi dashboard, Page page)
    {
        harvest(dashboard, "systemevents", List.of(systemEvent(9100002, "ingest-bot")));
        assertThat(auditedBy(page, "ingest-bot")).singleElement().satisfies(r -> assertThat(r)
            .containsEntry("Action Performed By", "ingest-bot"));
    }

    @Test
    void harvestingRefusesMalformedDataAndCallersWithoutRestAccess(DashboardApi dashboard, HarnessSession session)
    {
        HttpResponse<String> malformed = dashboard.call("PUT", "/rest/harvest/wiretaps", "this is not json");
        assertThat(malformed.statusCode()).as("malformed wiretaps").isEqualTo(400);
        assertThat(malformed.body()).contains("An error has occurred attempting to perform a batch insert of WiretapEvents");

        for (String kind : List.of("wiretaps", "errors", "exclusions", "replay", "systemevents"))
        {
            assertThat(dashboard.callWithoutToken("PUT", "/rest/harvest/" + kind, "[]").statusCode())
                .as("PUT /rest/harvest/" + kind + " without a token").isEqualTo(401);
        }

        // a dashboard user without the ALL or WebServiceAdmin authority may not send data
        session.authenticatedState("access-reader");
        DashboardApi reader = new DashboardApi(session.config(), "access-reader", Personas.load().password());
        assertThat(reader.call("PUT", "/rest/harvest/wiretaps", "[]").statusCode())
            .as("PUT /rest/harvest/wiretaps as a user without REST access").isEqualTo(403);
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static void harvest(DashboardApi dashboard, String kind, List<Map<String, Object>> documents)
    {
        dashboard.request("PUT", "/rest/harvest/" + kind, JSON.writeValueAsString(documents));
    }

    private static Map<String, Object> wiretap(long id, String eventId, String component, String payload, long timestamp)
    {
        Map<String, Object> w = new LinkedHashMap<>();
        // the module's own (numeric) id of the wiretap
        w.put("id", String.valueOf(id));
        w.put("moduleName", MODULE);
        w.put("flowName", FLOW);
        w.put("componentName", component);
        w.put("eventId", eventId);
        w.put("relatedEventId", eventId);
        w.put("event", payload);
        w.put("timestamp", timestamp);
        w.put("expiry", timestamp + Duration.ofDays(1).toMillis());
        return w;
    }

    private static String base64(String text)
    {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Search the last hour for one type, narrowed to the module, until the expected number of rows has arrived. */
    private static Grid searchModule(SearchPage search, String type, int rows)
    {
        long now = System.currentTimeMillis();
        SearchInput input = new SearchInput(now - Duration.ofHours(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of(type), "", null, Map.of("moduleName", MODULE));
        search.open().searchUntil(input, s -> s.grid().rows().size() == rows, WAIT, rows + " " + type + " row(s) of " + MODULE);
        return search.grid();
    }

    private static String rowTimestamp(Grid grid, String eventId)
    {
        return grid.rows().stream().filter(r -> eventId.equals(r.get("Event Id / Error URI"))).map(r -> r.get("Timestamp"))
            .findFirst().orElseThrow();
    }

    private static void removeModuleDocuments(HarnessSession session)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            List<String> ids = seeder.dump(TYPES).stream().filter(d -> MODULE.equals(d.get("moduleName")))
                .map(d -> String.valueOf(d.get("id"))).toList();
            if (!ids.isEmpty())
            {
                seeder.delete(ids);
            }
        }
    }

    private static Map<String, Object> systemEvent(long id, String actor)
    {
        long now = System.currentTimeMillis();
        Map<String, Object> event = new LinkedHashMap<>();
        // the module's own id of the event: the dashboard keeps one event per module and id
        event.put("id", id);
        event.put("moduleName", MODULE);
        event.put("action", "Flow stopped by " + actor);
        event.put("actor", actor);
        event.put("subject", "Ingest Flow");
        event.put("timestamp", now - 60_000);
        event.put("expiry", now + Duration.ofDays(1).toMillis());
        return event;
    }

    /** Search today's system events filtered by actor, until some are found (or the wait runs out: none). */
    private static List<Map<String, String>> auditedBy(Page page, String actor)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (true)
        {
            Vaadin.gotoRoute(page, "adminSearchView");
            page.locator("#systemEventSearchFormSearchButton").click();
            Vaadin.waitForVaadin(page);
            Grid grid = new Grid(page.locator("#identifier vaadin-grid").first());
            grid.waitForLoaded();
            Vaadin.fillField(page.locator("vaadin-text-field#actor"), actor);
            Vaadin.waitForVaadin(page);
            List<Map<String, String>> rows = grid.rows();
            if (!rows.isEmpty() || System.nanoTime() > deadline)
            {
                return rows;
            }
            page.waitForTimeout(500);
        }
    }
}
