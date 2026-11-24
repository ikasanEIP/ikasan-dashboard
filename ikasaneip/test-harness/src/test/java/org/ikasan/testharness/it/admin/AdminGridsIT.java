package org.ikasan.testharness.it.admin;

import org.ikasan.testharness.junit.HarnessTest;
import org.ikasan.testharness.junit.Scenarios;
import org.ikasan.testharness.pages.AdminGridPage;
import org.ikasan.testharness.support.Grid;
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

/** Administration list views: content and columns. Scenarios: expectations/admin/grids.yaml. */
@HarnessTest
@Tag("admin")
class AdminGridsIT
{
    record GridInput(String route) {}

    record GridExpect(Integer size, Integer minSize, List<String> visibleText, List<String> headers,
                      List<Map<String, Object>> rows, Boolean rowsExact) {}

    static Stream<Scenario> scenarios()
    {
        return Scenarios.load("admin/grids", "admin");
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("scenarios")
    void grid(Scenario scenario, AdminGridPage adminPage)
    {
        Assumptions.assumeTrue(scenario.skip() == null, scenario.skip());
        GridInput in = scenario.input(GridInput.class);
        GridExpect expect = scenario.expect(GridExpect.class);

        Grid grid = adminPage.open(in.route());

        if (expect.size() != null)
        {
            assertThat(grid.size()).as("rows in " + in.route()).isEqualTo(expect.size());
        }
        if (expect.minSize() != null)
        {
            assertThat(grid.size()).as("rows in " + in.route()).isGreaterThanOrEqualTo(expect.minSize());
        }
        if (expect.visibleText() != null)
        {
            assertThat(grid.visibleText()).as("text of " + in.route()).contains(expect.visibleText());
        }
        if (expect.headers() != null)
        {
            assertThat(grid.headerTexts()).as("column headers of " + in.route()).containsSubsequence(expect.headers());
        }
        if (expect.rows() != null)
        {
            GridRows.assertRows(grid.rows(), expect.rows(), Boolean.TRUE.equals(expect.rowsExact()), in.route());
        }
    }
}
