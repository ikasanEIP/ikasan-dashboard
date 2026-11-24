package org.ikasan.testharness.it.search;

import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.SearchPage;
import org.ikasan.testharness.pages.SearchPage.SearchInput;
import org.ikasan.testharness.support.GridRows;
import org.ikasan.testharness.support.Scenario;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event search (wiretap, error, exclusion, replay) against the baseline.
 * Scenarios: expectations/search/{wiretap,error,combined}.yaml.
 */
@HarnessTest
@Tag("search")
class SearchIT
{
    /**
     * @param total       exact total reported by the search
     * @param minTotal    minimum total (for environments where exact counts vary)
     * @param onlyTypes   every rendered row must be one of these entity types
     * @param visibleText these values must appear in the rendered grid rows
     */
    record SearchExpect(Integer total, Integer minTotal, List<String> onlyTypes, List<String> visibleText,
                        List<Map<String, Object>> rows, Boolean rowsExact) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load(List.of("search/wiretap", "search/error", "search/combined"), "search");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void search(Scenario scenario, SearchPage searchPage)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        SearchInput in = scenario.input(SearchInput.class);
        SearchExpect expect = scenario.expect(SearchExpect.class);

        searchPage.open().search(in);

        if (expect.total() != null)
        {
            assertThat(searchPage.totalResults()).as("total results").isEqualTo(expect.total());
        }
        if (expect.minTotal() != null)
        {
            assertThat(searchPage.totalResults()).as("total results").isGreaterThanOrEqualTo(expect.minTotal());
        }
        if (expect.onlyTypes() != null)
        {
            assertThat(searchPage.visibleEntityTypes()).as("entity types of the rows").isNotEmpty()
                .allMatch(expect.onlyTypes()::contains);
        }
        if (expect.visibleText() != null)
        {
            String text = searchPage.grid().visibleText();
            assertThat(text).as("grid text").contains(expect.visibleText());
        }
        if (expect.rows() != null)
        {
            GridRows.assertRows(searchPage.grid().rows(), expect.rows(), Boolean.TRUE.equals(expect.rowsExact()),
                "search results of '" + scenario.name() + "'");
        }
    }
}
