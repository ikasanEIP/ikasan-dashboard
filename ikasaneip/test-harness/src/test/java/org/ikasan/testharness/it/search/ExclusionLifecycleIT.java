package org.ikasan.testharness.it.search;

import org.ikasan.testharness.support.Timeouts;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.junit.HarnessSession;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.module.FakeIkasanModule;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.seed.Dataset;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The exclusion (hospital) lifecycle from the search view: open an exclusion, resubmit or ignore it - alone from its
 * dialog or in bulk from the results - and check that the module was asked to act, the action was recorded and the
 * exclusion has left the search results.
 * <p>
 * The modules are {@link FakeIkasanModule}s registered as the baseline's {@code harness-module-a/b}. Actioning an
 * exclusion replaces it in the store with an {@code exclusionEventAction} of the same id, so every test first
 * restores the baseline exclusions.
 */
@HarnessTest
@Tag("search")
@Tag("exclusion")
@Tag("mutating")
class ExclusionLifecycleIT
{
    private static final Duration WAIT = Timeouts.seconds(30);
    private static final String ORDER_MODULE = "harness-module-a";
    private static final String PAYMENT_MODULE = "harness-module-b";
    private static final String ORDER_EXCLUSION_1 = ORDER_MODULE + ":exclusion:err-0001";
    private static final String ORDER_EXCLUSION_3 = ORDER_MODULE + ":exclusion:err-0003";
    private static final String PAYMENT_EXCLUSION_5 = PAYMENT_MODULE + ":exclusion:err-0005";
    /** An exclusion whose error is not in the store (expired or removed); added by the test that uses it. */
    private static final String ORPHAN_EXCLUSION = ORDER_MODULE + ":exclusion:err-0099";
    /** An exclusion of a module the dashboard has no module metadata for; added by the test that uses it. */
    private static final String UNKNOWN_MODULE = "harness-module-unknown";
    private static final String UNKNOWN_MODULE_EXCLUSION = UNKNOWN_MODULE + ":exclusion:err-0001";

    /** The excluded event of each exclusion, which its action must keep. */
    private static final Map<String, String> PAYLOADS = Map.of(
        ORDER_EXCLUSION_1, "<order><id>ORDER-901</id></order>",
        ORDER_EXCLUSION_3, "<order><id>ORDER-903</id></order>",
        PAYMENT_EXCLUSION_5, "<order><id>ORDER-905</id></order>",
        ORPHAN_EXCLUSION, "<order><id>ORDER-999</id></order>");

    private static FakeIkasanModule module;

    @BeforeAll
    static void startModules(HarnessSession session)
    {
        HarnessConfig config = session.config();
        module = new FakeIkasanModule(config.scheduler().fakeAgentHost(), FakeIkasanModule.configuredPort(),
            config.dashboard().restUsername(), config.dashboard().restPassword()).start();
        DashboardApi dashboard = new DashboardApi(config);
        module.register(dashboard, ORDER_MODULE);
        module.register(dashboard, PAYMENT_MODULE);
    }

    @AfterAll
    static void stopModules()
    {
        if (module != null)
        {
            module.close();
        }
    }

    @BeforeEach
    void restoreExclusions(HarnessSession session, SearchPage search)
    {
        module.reset();
        List<Map<String, Object>> exclusions = Dataset.load(session.config().seed().dataset(), session.clock()).documents()
            .stream().filter(d -> "exclusion".equals(d.get("type"))).toList();
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.load(exclusions);
        }
        search.open().searchUntil(allExclusions(), s -> s.totalResults() == exclusions.size(), WAIT,
            "the " + exclusions.size() + " restored exclusions");
    }

    @Test
    @Tag("smoke")
    void resubmitAnExclusionFromItsDialog(SearchPage search, Page page, HarnessSession session)
    {
        Locator dialog = search.openRow("EVT-ERR-005 / err-0005", "Exclusion EVT-ERR-005");
        assertThat(dialog.getByText("Hospital Event Details")).isVisible();
        assertThat(Vaadin.field(dialog, "Module Name")).hasValue(PAYMENT_MODULE);
        assertThat(Vaadin.field(dialog, "Flow Name")).hasValue("Payment Flow");
        assertThat(Vaadin.field(dialog, "Event Id")).hasValue("EVT-ERR-005");
        assertThat(Vaadin.field(dialog, "Error URI")).hasValue("err-0005");
        // the error action comes from the error the exclusion was made for
        assertThat(Vaadin.field(dialog, "Error Action")).hasValue("ExcludeEvent");
        dialog.getByRole(com.microsoft.playwright.options.AriaRole.TAB,
            new Locator.GetByRoleOptions().setName("Error Details")).click();
        assertThat(dialog).containsText("harness failure 5");

        Vaadin.button(dialog, "Resubmit").click();
        Locator comments = Vaadin.dialog(page, "resubmit hospital events");
        // a comment is required
        comments.locator("#hospitalCommentsDialogActionButton").click();
        assertThat(comments.getByText("A comment is required!")).isVisible();
        Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "resubmitting EVT-ERR-005");
        comments.locator("#hospitalCommentsDialogActionButton").click();

        Vaadin.expectNotification(page, "the hospital event was successfully resubmitted");
        assertModuleAsked(PAYMENT_MODULE, "Payment Flow", "err-0005", "resubmit");
        assertActionRecorded(session, PAYMENT_EXCLUSION_5, "re-submitted", "resubmitting EVT-ERR-005");
        assertGoneFromSearch(search, "EVT-ERR-005 / err-0005");
    }

    @Test
    void ignoreAnExclusionFromItsDialog(SearchPage search, Page page, HarnessSession session)
    {
        Locator dialog = search.openRow("EVT-ERR-001 / err-0001", "Exclusion EVT-ERR-001");
        Vaadin.button(dialog, "Ignore").click();
        Locator comments = Vaadin.dialog(page, "ignore hospital events");
        Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "ignoring EVT-ERR-001");
        comments.locator("#hospitalCommentsDialogActionButton").click();

        Vaadin.expectNotification(page, "the hospital event was successfully ignored");
        assertModuleAsked(ORDER_MODULE, "Order Flow", "err-0001", "ignore");
        assertActionRecorded(session, ORDER_EXCLUSION_1, "ignored", "ignoring EVT-ERR-001");
        assertGoneFromSearch(search, "EVT-ERR-001 / err-0001");
    }

    @Test
    void aResubmissionTheModuleRejectsLeavesTheExclusion(SearchPage search, Page page, HarnessSession session)
    {
        module.failWith("/rest/resubmission", 500);

        Locator dialog = search.openRow("EVT-ERR-003 / err-0003", "Exclusion EVT-ERR-003");
        Vaadin.button(dialog, "Resubmit").click();
        Locator comments = Vaadin.dialog(page, "resubmit hospital events");
        Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "resubmitting EVT-ERR-003");
        comments.locator("#hospitalCommentsDialogActionButton").click();

        Vaadin.expectNotification(page, "An error has occurred resubmitting the hospital event. Please contact Ikasan support.");
        assertModuleAsked(ORDER_MODULE, "Order Flow", "err-0003", "resubmit");
        assertThat(actions(session)).as("exclusion actions recorded").doesNotContainKey(ORDER_EXCLUSION_3);
        Vaadin.closeDialog(page, dialog);
        search.search(allExclusions());
        assertThat(search.grid().visibleText()).as("exclusions after the failed resubmission").contains("EVT-ERR-003 / err-0003");
    }

    @Test
    void resubmitSelectedExclusionsInBulk(SearchPage search, Page page, HarnessSession session)
    {
        search.select(ORDER_EXCLUSION_1, ORDER_EXCLUSION_3);
        search.bulkButton("resubmit all selected events").click();
        Locator comments = Vaadin.dialog(page, "resubmit hospital events");
        Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "bulk resubmission");
        comments.locator("#hospitalCommentsDialogActionButton").click();

        Vaadin.expectNotification(page, "successfully resubmitted 2 exclusions");
        List<FakeIkasanModule.Call> calls = module.awaitCalls(c -> c.path().endsWith("/rest/resubmission"), 2, WAIT);
        assertThat(calls).extracting(c -> c.json().get("errorUri")).containsExactlyInAnyOrder("err-0001", "err-0003");
        assertThat(calls).extracting(c -> c.json().get("action")).containsOnly("resubmit");
        assertActionRecorded(session, ORDER_EXCLUSION_1, "re-submitted", "bulk resubmission");
        assertActionRecorded(session, ORDER_EXCLUSION_3, "re-submitted", "bulk resubmission");
        assertGoneFromSearch(search, "EVT-ERR-001 / err-0001");
        assertThat(search.grid().visibleText()).as("exclusions after the bulk resubmission")
            .doesNotContain("EVT-ERR-003 / err-0003").contains("EVT-ERR-005 / err-0005");
    }

    @Test
    void ignoreSelectedExclusionsInBulk(SearchPage search, Page page, HarnessSession session)
    {
        search.select(PAYMENT_EXCLUSION_5);
        search.bulkButton("ignore all selected events").click();
        Locator comments = Vaadin.dialog(page, "ignore hospital events");
        Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "bulk ignore");
        comments.locator("#hospitalCommentsDialogActionButton").click();

        Vaadin.expectNotification(page, "successfully ignored 1 exclusions");
        FakeIkasanModule.Call call = module.awaitCalls(c -> c.path().endsWith("/rest/resubmission"), 1, WAIT).getFirst();
        assertThat(call.json()).containsEntry("errorUri", "err-0005").containsEntry("action", "ignore");
        assertActionRecorded(session, PAYMENT_EXCLUSION_5, "ignored", "bulk ignore");
        assertGoneFromSearch(search, "EVT-ERR-005 / err-0005");
    }

    @Test
    void aBulkActionNeedsASelection(SearchPage search, Page page)
    {
        search.bulkButton("resubmit all selected events").click();
        Vaadin.expectNotification(page, "at least one record must be selected!");
        assertThat(module.calls()).as("requests to the modules").isEmpty();
    }

    @Test
    void anExclusionWhoseErrorHasGoneCanStillBeActioned(SearchPage search, Page page, HarnessSession session)
    {
        addExclusion(session, ORPHAN_EXCLUSION, ORDER_MODULE, "Order Flow", "err-0099", "EVT-ERR-099", PAYLOADS.get(ORPHAN_EXCLUSION));
        try
        {
            search.searchUntil(allExclusions(), s -> s.grid().visibleText().contains("EVT-ERR-099 / err-0099"), WAIT,
                "the exclusion without an error");
            Locator dialog = search.openRow("EVT-ERR-099 / err-0099", "Exclusion EVT-ERR-099");
            assertThat(Vaadin.field(dialog, "Error URI")).hasValue("err-0099");
            assertThat(Vaadin.field(dialog, "Error Action")).hasValue("");

            Vaadin.button(dialog, "Resubmit").click();
            Locator comments = Vaadin.dialog(page, "resubmit hospital events");
            Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "resubmitting without its error");
            comments.locator("#hospitalCommentsDialogActionButton").click();

            Vaadin.expectNotification(page, "the hospital event was successfully resubmitted");
            assertModuleAsked(ORDER_MODULE, "Order Flow", "err-0099", "resubmit");
            assertActionRecorded(session, ORPHAN_EXCLUSION, "re-submitted", "resubmitting without its error");
        }
        finally
        {
            removeDocuments(session, ORPHAN_EXCLUSION);
        }
    }

    @Test
    void anExclusionOfAModuleTheDashboardDoesNotKnowIsLeftAlone(SearchPage search, Page page, HarnessSession session)
    {
        addExclusion(session, UNKNOWN_MODULE_EXCLUSION, UNKNOWN_MODULE, "Unknown Flow", "err-0001", "EVT-ERR-001",
            "<order><id>ORDER-901</id></order>");
        try
        {
            search.searchUntil(allExclusions(), s -> s.grid().visibleText().contains(UNKNOWN_MODULE), WAIT,
                "the exclusion of the unknown module");
            Locator dialog = search.openRow(UNKNOWN_MODULE, "Exclusion EVT-ERR-001");
            Vaadin.button(dialog, "Resubmit").click();
            Locator comments = Vaadin.dialog(page, "resubmit hospital events");
            Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "resubmitting to an unknown module");
            comments.locator("#hospitalCommentsDialogActionButton").click();

            Vaadin.expectNotification(page, "The dashboard has no address for module [" + UNKNOWN_MODULE + "]");
            // the progress dialog closed, the exclusion dialog is usable again
            assertThat(Vaadin.button(dialog, "Resubmit")).isVisible();
            assertThat(module.calls()).as("requests to the modules").isEmpty();
            assertThat(actions(session)).as("exclusion actions recorded").doesNotContainKey(UNKNOWN_MODULE_EXCLUSION);
        }
        finally
        {
            removeDocuments(session, UNKNOWN_MODULE_EXCLUSION);
        }
    }

    @Test
    void aBulkActionStopsAtAModuleTheDashboardDoesNotKnow(SearchPage search, Page page, HarnessSession session)
    {
        addExclusion(session, UNKNOWN_MODULE_EXCLUSION, UNKNOWN_MODULE, "Unknown Flow", "err-0001", "EVT-ERR-001",
            "<order><id>ORDER-901</id></order>");
        try
        {
            search.searchUntil(allExclusions(), s -> s.grid().visibleText().contains(UNKNOWN_MODULE), WAIT,
                "the exclusion of the unknown module");
            search.select(UNKNOWN_MODULE_EXCLUSION);
            search.bulkButton("ignore all selected events").click();
            Locator comments = Vaadin.dialog(page, "ignore hospital events");
            Vaadin.fillField(comments.locator("#hospitalCommentsDialogCommentTf"), "bulk ignore to an unknown module");
            comments.locator("#hospitalCommentsDialogActionButton").click();

            Vaadin.expectNotification(page, "The dashboard has no address for module [" + UNKNOWN_MODULE + "]");
            assertThat(module.calls()).as("requests to the modules").isEmpty();
            assertThat(actions(session)).as("exclusion actions recorded").doesNotContainKey(UNKNOWN_MODULE_EXCLUSION);
        }
        finally
        {
            removeDocuments(session, UNKNOWN_MODULE_EXCLUSION);
        }
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static SearchInput allExclusions()
    {
        long now = System.currentTimeMillis();
        return new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of("exclusion"), "", null, null);
    }

    /** The module was asked (PUT <module url>/rest/resubmission, with the dashboard's module credentials) to act. */
    private static void assertModuleAsked(String moduleName, String flowName, String errorUri, String action)
    {
        FakeIkasanModule.Call call = module.awaitCalls(c -> c.method().equals("PUT")
            && c.path().equals("/" + moduleName + "/rest/resubmission"), 1, WAIT).getFirst();
        assertThat(call.json()).as("resubmission request to " + moduleName)
            .containsEntry("moduleName", moduleName)
            .containsEntry("flowName", flowName)
            .containsEntry("errorUri", errorUri)
            .containsEntry("action", action)
            .containsEntry("userName", "admin");
        assertThat(call.basicCredentials()).as("credentials sent to the module").isEqualTo("admin:admin");
    }

    /** The exclusion actions in the store, by id (the id of the exclusion they replaced). */
    private static Map<String, Map<String, Object>> actions(HarnessSession session)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            return seeder.dump(List.of("exclusionEventAction")).stream()
                .collect(java.util.stream.Collectors.toMap(d -> String.valueOf(d.get("id")), d -> d));
        }
    }

    /** The exclusion was replaced by an action naming the action, the comment and the user. */
    private static void assertActionRecorded(HarnessSession session, String exclusionId, String action, String comment)
    {
        Map<String, Object> recorded = actions(session).get(exclusionId);
        assertThat(recorded).as("exclusion action recorded for " + exclusionId).isNotNull();
        assertThat(String.valueOf(recorded.get("hospitalEventAction"))).as("recorded action of " + exclusionId)
            .contains("comment[" + comment + "]", "action[" + action + "]", "actioned by[admin]");
        assertThat(Objects.toString(recorded.get("type"))).isEqualTo("exclusionEventAction");
        // the action keeps the excluded event (the bulk actions used to overwrite it with the action itself)
        assertThat(String.valueOf(recorded.get("payload"))).as("event recorded with the action of " + exclusionId)
            .isEqualTo(PAYLOADS.get(exclusionId));
    }

    /** Search the exclusions again until the actioned one has gone (the index may lag the write). */
    private static void assertGoneFromSearch(SearchPage search, String rowText)
    {
        search.searchUntil(allExclusions(), s -> !s.grid().visibleText().contains(rowText), WAIT,
            "no exclusion '" + rowText + "'");
    }

    /** Add an exclusion that is not in the baseline (remove it with {@link #removeDocuments}). */
    private static void addExclusion(HarnessSession session, String id, String moduleName, String flowName, String errorUri,
                                     String eventId, String payload)
    {
        long now = System.currentTimeMillis();
        Map<String, Object> exclusion = new java.util.LinkedHashMap<>();
        exclusion.put("id", id);
        exclusion.put("type", "exclusion");
        exclusion.put("moduleName", moduleName);
        exclusion.put("flowName", flowName);
        exclusion.put("errorUri", errorUri);
        exclusion.put("event", eventId);
        exclusion.put("payload", payload);
        exclusion.put("timestamp", now - Duration.ofMinutes(3).toMillis());
        exclusion.put("harvestReceivedTimestamp", now - Duration.ofMinutes(3).toMillis());
        exclusion.put("expiry", now + Duration.ofDays(30).toMillis());
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.load(List.of(exclusion));
        }
    }

    /** Remove documents a test added (an exclusion, or the action that replaced it: they share the id). */
    private static void removeDocuments(HarnessSession session, String... ids)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            seeder.delete(List.of(ids));
        }
    }
}
