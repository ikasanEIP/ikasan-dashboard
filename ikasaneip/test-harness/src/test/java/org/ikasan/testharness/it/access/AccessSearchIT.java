package org.ikasan.testharness.it.access;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.LoginAs;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.support.Vaadin;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What users with narrower access than admin may search and do in the search view ({@code data/personas.yaml}):
 * <ul>
 *     <li>the entity types they may search (the type toggles) and the modules whose events they see</li>
 *     <li>the exclusion and replay actions they may take: bulk resubmit/ignore/replay and the dialogs' buttons</li>
 * </ul>
 * Read-only: no action is taken, only whether it is offered. The data is the baseline's (wiretaps and errors of
 * harness-module-a and -b, exclusions and replay events).
 */
@HarnessTest
@Tag("access")
// logging in as a persona writes its user and role to the store: run after the read-only classes (AdminGridsIT lists users)
@Tag("mutating")
@Tag("search")
class AccessSearchIT
{
    private static final List<String> TOGGLES = List.of("#wiretapCheckButton", "#errorCheckButton", "#hospitalCheckButton",
        "#replayCheckButton");
    private static final String RESUBMIT_ALL = "resubmit all selected events";
    private static final String IGNORE_ALL = "ignore all selected events";
    private static final String REPLAY_ALL = "replay all selected events";

    @Test
    @Tag("smoke")
    @LoginAs("access-reader")
    void aReaderSearchesEverythingButCannotActionExclusionsOrReplay(SearchPage search, Page page)
    {
        search.open();
        assertThat(shownToggles(page)).as("entity types the reader may search").containsExactlyElementsOf(TOGGLES);

        search.search(only("exclusion"));
        assertThat(search.totalResults()).as("exclusions found").isEqualTo(3);
        assertThat(Vaadin.isShown(search.bulkButton(RESUBMIT_ALL))).as("bulk resubmit offered").isFalse();
        assertThat(Vaadin.isShown(search.bulkButton(IGNORE_ALL))).as("bulk ignore offered").isFalse();
        Locator exclusion = search.openRow("EVT-ERR-001 / err-0001", "Exclusion EVT-ERR-001");
        assertThat(Vaadin.isShown(Vaadin.button(exclusion, "Resubmit"))).as("exclusion dialog's Resubmit offered").isFalse();
        assertThat(Vaadin.isShown(Vaadin.button(exclusion, "Ignore"))).as("exclusion dialog's Ignore offered").isFalse();
        Vaadin.closeDialog(page, exclusion);

        search.search(only("replay"));
        assertThat(search.totalResults()).as("replay events found").isEqualTo(4);
        assertThat(Vaadin.isShown(search.bulkButton(REPLAY_ALL))).as("bulk replay offered").isFalse();
        Locator replay = search.openRow("EVT-RPL-001", "Replay EVT-RPL-001");
        assertThat(Vaadin.isShown(Vaadin.button(replay, "Replay"))).as("replay dialog's Replay offered").isFalse();
    }

    @Test
    @LoginAs("access-operator")
    void anOperatorMayActionExclusionsAndReplay(SearchPage search, Page page)
    {
        search.open();
        assertThat(shownToggles(page)).as("entity types the operator may search").containsExactlyElementsOf(TOGGLES);

        search.search(only("exclusion"));
        assertThat(Vaadin.isShown(search.bulkButton(RESUBMIT_ALL))).as("bulk resubmit offered").isTrue();
        assertThat(Vaadin.isShown(search.bulkButton(IGNORE_ALL))).as("bulk ignore offered").isTrue();
        Locator exclusion = search.openRow("EVT-ERR-001 / err-0001", "Exclusion EVT-ERR-001");
        assertThat(Vaadin.isShown(Vaadin.button(exclusion, "Resubmit"))).as("exclusion dialog's Resubmit offered").isTrue();
        assertThat(Vaadin.isShown(Vaadin.button(exclusion, "Ignore"))).as("exclusion dialog's Ignore offered").isTrue();
        Vaadin.closeDialog(page, exclusion);

        search.search(only("replay"));
        assertThat(Vaadin.isShown(search.bulkButton(REPLAY_ALL))).as("bulk replay offered").isTrue();
        Locator replay = search.openRow("EVT-RPL-001", "Replay EVT-RPL-001");
        assertThat(Vaadin.isShown(Vaadin.button(replay, "Replay"))).as("replay dialog's Replay offered").isTrue();
    }

    @Test
    @LoginAs("access-wiretaps")
    void aWiretapSearcherSeesOnlyWiretaps(SearchPage search, Page page)
    {
        search.open();
        assertThat(shownToggles(page)).as("entity types the user may search").containsExactly("#wiretapCheckButton");

        search.search(everything());
        assertThat(search.totalResults()).as("events found (the baseline's 12 wiretaps)").isEqualTo(12);
        assertThat(search.visibleEntityTypes()).containsOnly("wiretap");
    }

    @Test
    @LoginAs("access-module-a")
    void aModuleScopedSearcherSeesOnlyItsModule(SearchPage search, Page page)
    {
        search.open();
        assertThat(shownToggles(page)).containsExactly("#wiretapCheckButton", "#errorCheckButton");

        search.search(everything());
        assertThat(search.totalResults()).as("events of harness-module-a (8 wiretaps, 3 errors)").isEqualTo(11);
        assertThat(search.grid().rows()).extracting(r -> r.get("Module Name")).containsOnly("harness-module-a");
    }

    @Test
    @LoginAs("access-no-modules")
    void aSearcherWithoutModulesFindsNothing(SearchPage search)
    {
        search.open().search(everything());
        assertThat(search.totalResults()).as("events found without any module assigned").isZero();
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private static List<String> shownToggles(Page page)
    {
        return TOGGLES.stream().filter(t -> Vaadin.isShown(page.locator(t))).toList();
    }

    /** The last day, every type the user may search (the form's defaults). */
    private static SearchInput everything()
    {
        long now = System.currentTimeMillis();
        return new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(), null, "", null, null);
    }

    private static SearchInput only(String type)
    {
        long now = System.currentTimeMillis();
        return new SearchInput(now - Duration.ofDays(1).toMillis(), now + Duration.ofHours(1).toMillis(), List.of(type), "",
            null, Map.of());
    }
}
