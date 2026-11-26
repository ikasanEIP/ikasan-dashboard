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
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The replay lifecycle from the search view: open a replay event and replay it - alone from its dialog or in bulk
 * from the results - to a target module, then check what the target received, what the user was shown (the bulk
 * replay manifest) and the replay audit written to the store (no screen shows replay audits).
 * <p>
 * The target is a {@link FakeIkasanModule}; replay sends the event's raw payload, base64 encoded, with the
 * credentials typed into the replay form. Replaying leaves the replay events unchanged.
 */
@HarnessTest
@Tag("search")
@Tag("replay")
@Tag("mutating")
class ReplayLifecycleIT
{
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration WAIT = Timeouts.seconds(30);
    private static final String ORDER_MODULE = "harness-module-a";
    private static final String REPLAY_1 = ORDER_MODULE + "-replay-EVT-RPL-001";
    private static final String REPLAY_2 = ORDER_MODULE + "-replay-EVT-RPL-002";
    private static final String REPLAY_4 = "harness-module-b-replay-EVT-RPL-004";

    private static FakeIkasanModule target;
    private static String username;
    private static String password;

    @BeforeAll
    static void startTarget(HarnessSession session)
    {
        HarnessConfig config = session.config();
        username = config.dashboard().restUsername();
        password = config.dashboard().restPassword();
        target = new FakeIkasanModule(config.scheduler().fakeAgentHost(), FakeIkasanModule.configuredPort(), username, password)
            .start();
    }

    @AfterAll
    static void stopTarget()
    {
        if (target != null)
        {
            target.close();
        }
    }

    @BeforeEach
    void searchReplayEvents(SearchPage search)
    {
        target.reset();
        search.open().search(allReplayEvents());
    }

    @Test
    @Tag("smoke")
    void replayAnEventFromItsDialog(SearchPage search, Page page, HarnessSession session)
    {
        Set<String> auditsBefore = auditIds(session);
        Locator dialog = search.openRow("EVT-RPL-001", "Replay EVT-RPL-001");
        assertThat(dialog.getByText("Replay Event Details")).isVisible();
        assertThat(Vaadin.field(dialog, "Module Name")).hasValue(ORDER_MODULE);
        assertThat(Vaadin.field(dialog, "Flow Name")).hasValue("Order Flow");
        assertThat(Vaadin.field(dialog, "Event Id")).hasValue("EVT-RPL-001");

        Vaadin.button(dialog, "Replay").click();
        Locator form = Vaadin.dialog(page, "Target Module URL");
        // every field is required
        Vaadin.button(form, "Replay").click();
        for (String message : List.of("Target module URL is required!", "please enter a username", "please enter a password",
            "A comment is required!"))
        {
            assertThat(form.getByText(message)).isVisible();
        }
        fill(form, target.url(ORDER_MODULE), username, password, "replaying EVT-RPL-001");
        Vaadin.button(form, "Replay").click();

        Vaadin.expectNotification(page, "replay complete");
        FakeIkasanModule.Call call = target.awaitCalls(c -> c.method().equals("PUT")
            && c.path().equals("/" + ORDER_MODULE + "/rest/replay"), 1, WAIT).getFirst();
        assertThat(call.json()).as("replay request")
            .containsEntry("moduleName", ORDER_MODULE)
            .containsEntry("flowName", "Order Flow")
            .containsEntry("userName", "admin");
        assertThat(decode(call.json().get("event"))).as("replayed event (the raw payload)")
            .isEqualTo("<order><id>ORDER-701</id><replay>true</replay></order>");
        assertThat(call.basicCredentials()).as("credentials sent to the target").isEqualTo(username + ":" + password);

        Map<String, Object> audit = awaitNewAudits(session, auditsBefore, 1).getFirst();
        assertThat(audit).containsEntry("id", REPLAY_1).containsEntry("success", true)
            .containsEntry("resultMessage", "successfully replayed event [" + REPLAY_1 + "]");
        assertThat(replayAudit(audit)).containsEntry("user", "admin").containsEntry("replayReason", "replaying EVT-RPL-001")
            .containsEntry("targetServer", target.url(ORDER_MODULE));
    }

    @Test
    void aReplayTheTargetRejectsIsReportedAndAudited(SearchPage search, Page page, HarnessSession session)
    {
        Set<String> auditsBefore = auditIds(session);
        Locator dialog = search.openRow("EVT-RPL-001", "Replay EVT-RPL-001");
        Vaadin.button(dialog, "Replay").click();
        Locator form = Vaadin.dialog(page, "Target Module URL");
        fill(form, target.url(ORDER_MODULE), username, "wrong-password", "replaying with a bad password");
        Vaadin.button(form, "Replay").click();

        Vaadin.expectNotification(page, "Issue replaying event");
        assertThat(target.awaitCalls(c -> c.path().endsWith("/rest/replay"), 1, WAIT)).hasSize(1);
        Map<String, Object> audit = awaitNewAudits(session, auditsBefore, 1).getFirst();
        assertThat(audit).containsEntry("id", REPLAY_1).containsEntry("success", false);
        assertThat(String.valueOf(audit.get("resultMessage"))).startsWith("failed to replay event [" + REPLAY_1 + "]");
    }

    @Test
    void replaySelectedEventsInBulk(SearchPage search, Page page, HarnessSession session)
    {
        Set<String> auditsBefore = auditIds(session);
        search.select(REPLAY_2, REPLAY_4);
        search.bulkButton("replay all selected events").click();
        Locator form = Vaadin.dialog(page, "Target Module URL");
        fill(form, target.url(ORDER_MODULE), username, password, "bulk replay");
        Vaadin.button(form, "Replay").click();

        Vaadin.expectNotification(page, "replay complete");
        Locator manifest = Vaadin.dialog(page, "Replay Manifest");
        assertThat(manifest).containsText("All replay events were successful.");
        List<FakeIkasanModule.Call> calls = target.awaitCalls(c -> c.path().endsWith("/rest/replay"), 2, WAIT);
        assertThat(calls).extracting(c -> decode(c.json().get("event"))).containsExactlyInAnyOrder(
            "<order><id>ORDER-702</id><replay>true</replay></order>", "<order><id>ORDER-704</id><replay>true</replay></order>");
        assertThat(calls).extracting(c -> c.json().get("moduleName")).containsExactlyInAnyOrder(ORDER_MODULE, "harness-module-b");

        List<Map<String, Object>> audits = awaitNewAudits(session, auditsBefore, 2);
        assertThat(audits).extracting(a -> a.get("id")).containsExactlyInAnyOrder(REPLAY_2, REPLAY_4);
        assertThat(audits).as("bulk replay audits").allSatisfy(a -> assertThat(a).containsEntry("success", true));
    }

    @Test
    void aBulkReplayWithFailuresShowsAnErrorManifest(SearchPage search, Page page, HarnessSession session)
    {
        search.select(REPLAY_2);
        search.bulkButton("replay all selected events").click();
        Locator form = Vaadin.dialog(page, "Target Module URL");
        fill(form, target.url(ORDER_MODULE), username, "wrong-password", "bulk replay with a bad password");
        Vaadin.button(form, "Replay").click();

        Locator manifest = Vaadin.dialog(page, "Replay Manifest");
        assertThat(manifest).containsText("One or more replay events failed to replay successfully.");
        assertThat(target.awaitCalls(c -> c.path().endsWith("/rest/replay"), 1, WAIT)).hasSize(1);
    }

    @Test
    void aBulkReplayNeedsASelection(SearchPage search, Page page)
    {
        search.bulkButton("replay all selected events").click();
        Vaadin.expectNotification(page, "at least one record must be selected!");
        assertThat(target.calls()).as("requests to the target").isEmpty();
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static SearchInput allReplayEvents()
    {
        long now = System.currentTimeMillis();
        return new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(),
            List.of("replay"), "", null, null);
    }

    private static void fill(Locator form, String url, String user, String pass, String comment)
    {
        Vaadin.fillField(Vaadin.field(form, "Target Module URL"), url);
        Vaadin.fillField(Vaadin.field(form, "username"), user);
        Vaadin.fillField(Vaadin.field(form, "password"), pass);
        Vaadin.fillField(Vaadin.field(form, "Comment"), comment);
    }

    private static String decode(Object base64)
    {
        return new String(Base64.getDecoder().decode(String.valueOf(base64)), StandardCharsets.UTF_8);
    }

    private static List<Map<String, Object>> audits(HarnessSession session)
    {
        try (Seeder seeder = Seeder.create(session.config()))
        {
            return seeder.dump(List.of("replay_audit"));
        }
    }

    private static Set<String> auditIds(HarnessSession session)
    {
        return audits(session).stream().map(a -> String.valueOf(a.get("id"))).collect(Collectors.toSet());
    }

    /**
     * Wait for replay audits written since {@code before}, and return their payloads: the audited replay event
     * ({@code id, success, resultMessage, replayAudit: {user, replayReason, targetServer}}).
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> awaitNewAudits(HarnessSession session, Set<String> before, int count)
    {
        long deadline = System.nanoTime() + WAIT.toNanos();
        List<Map<String, Object>> added;
        do
        {
            added = audits(session).stream().filter(a -> !before.contains(String.valueOf(a.get("id"))))
                .map(a -> (Map<String, Object>) JSON.readValue(String.valueOf(a.get("payload")), Map.class)).toList();
            if (added.size() >= count)
            {
                return added;
            }
            sleep();
        }
        while (System.nanoTime() < deadline);
        throw new AssertionError("Expected " + count + " new replay audits, found " + added.size() + ": " + added);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> replayAudit(Map<String, Object> audit)
    {
        return (Map<String, Object>) audit.get("replayAudit");
    }

    private static void sleep()
    {
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
