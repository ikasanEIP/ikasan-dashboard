package org.ikasan.testharness.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.ikasan.testharness.support.Grid;
import org.ikasan.testharness.support.Vaadin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** SearchView (route 'Search'): SearchForm + SearchResults. */
public class SearchPage
{
    /**
     * @param from    epoch millis (use {{now-1d}} style tokens in expectations); null keeps the form default
     * @param to      epoch millis; null keeps the form default
     * @param types   entity types to search: wiretap, error, exclusion, replay; null keeps the defaults (all)
     * @param term    search term; null or empty = everything
     * @param negate  negate the search term
     * @param filters grid column key (moduleName, flowName, componentName, event) to filter text
     */
    public record SearchInput(Long from, Long to, List<String> types, String term, Boolean negate,
                              Map<String, String> filters) {}

    private static final Map<String, String> TYPE_BUTTONS = Map.of(
        "wiretap", "#wiretapCheckButton",
        "error", "#errorCheckButton",
        "exclusion", "#hospitalCheckButton",
        "replay", "#replayCheckButton");

    private final Page page;
    private final Grid grid;

    public SearchPage(Page page)
    {
        this.page = page;
        this.grid = new Grid(page.locator("vaadin-grid#searchResultsGrid"));
    }

    public Grid grid()
    {
        return grid;
    }

    public SearchPage open()
    {
        Vaadin.gotoRoute(page, "Search");
        assertThat(page.locator("#searchFormSearchButton")).isVisible();
        return this;
    }

    /** Open the event life id deep link view, which searches wiretaps, errors and exclusions for the id. */
    public SearchPage openEventLifeId(String eventLifeId)
    {
        Vaadin.gotoRoute(page, "eventLifeId/" + eventLifeId);
        assertThat(page.locator("#searchResultsCountLabel")).hasAttribute("data-result-count", Pattern.compile("\\d+"));
        grid.waitForLoaded();
        return this;
    }

    public void search(SearchInput input)
    {
        ZoneId zone = ZoneId.systemDefault();
        if (input.from() != null)
        {
            LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(input.from()), zone);
            Vaadin.setDatePicker(page.locator("#searchFormStartDate"), t.toLocalDate().toString());
            Vaadin.setTimePicker(page.locator("#searchFormStartTime"), quarter(t, false));
        }
        if (input.to() != null)
        {
            LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(input.to()), zone);
            Vaadin.setDatePicker(page.locator("#searchFormEndDate"), t.toLocalDate().toString());
            Vaadin.setTimePicker(page.locator("#searchFormEndTime"), quarter(t, true));
        }
        if (input.types() != null)
        {
            TYPE_BUTTONS.forEach((type, selector) -> {
                Locator button = page.locator(selector);
                if (button.isVisible())
                {
                    Vaadin.setPressed(button, input.types().contains(type));
                }
            });
        }
        Vaadin.fillField(page.locator("#searchFormSearchText"), input.term() == null ? "" : input.term());
        if (input.negate() != null)
        {
            Vaadin.setChecked(page.locator("#searchFormNegateCheckbox"), input.negate());
        }
        page.locator("#searchFormSearchButton").click();
        Vaadin.waitForVaadin(page);
        assertThat(page.locator("#searchResultsCountLabel")).hasAttribute("data-result-count", Pattern.compile("\\d+"));
        if (input.filters() != null)
        {
            input.filters().forEach((column, text) -> {
                Vaadin.fillField(grid.filter(column), text);
                Vaadin.waitForVaadin(page);
            });
        }
        grid.waitForLoaded();
    }

    /** The time pickers step in 15 minutes: round down for 'from' and up for 'to', so the window holds the data. */
    private static String quarter(LocalDateTime t, boolean up)
    {
        int minutes = t.getHour() * 60 + t.getMinute();
        int q = up ? Math.min((int) Math.ceil((minutes + 1) / 15.0) * 15, 23 * 60 + 45) : (minutes / 15) * 15;
        return String.format("%02d:%02d", q / 60, q % 60);
    }

    /** Total results reported by the search (before grid column filters). */
    public int totalResults()
    {
        return Integer.parseInt(page.locator("#searchResultsCountLabel").getAttribute("data-result-count"));
    }

    /** Entity types (wiretap, error, ...) of the rendered rows. */
    @SuppressWarnings("unchecked")
    public List<String> visibleEntityTypes()
    {
        return (List<String>) page.locator("vaadin-grid#searchResultsGrid [data-entity-type]")
            .evaluateAll("els => els.map(e => e.getAttribute('data-entity-type'))");
    }
}
