package org.ikasan.testharness.it.search;

import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.SearchPage;
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

/** The eventLifeId/{id} deep link view. Scenarios: expectations/search/event-life-id.yaml. */
@HarnessTest
@Tag("search")
class EventLifeIdIT
{
    record EventInput(String eventLifeId) {}

    record EventExpect(Integer total, Integer minTotal, List<String> onlyTypes, List<String> visibleText,
                       List<Map<String, Object>> rows, Boolean rowsExact) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load("search/event-life-id", "search");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void eventLifeId(Scenario scenario, SearchPage searchPage)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        EventInput in = scenario.input(EventInput.class);
        EventExpect expect = scenario.expect(EventExpect.class);

        searchPage.openEventLifeId(in.eventLifeId());

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
            assertThat(searchPage.grid().visibleText()).as("grid text").contains(expect.visibleText());
        }
        if (expect.rows() != null)
        {
            GridRows.assertRows(searchPage.grid().rows(), expect.rows(), Boolean.TRUE.equals(expect.rowsExact()),
                "event life id " + in.eventLifeId());
        }
    }
}
