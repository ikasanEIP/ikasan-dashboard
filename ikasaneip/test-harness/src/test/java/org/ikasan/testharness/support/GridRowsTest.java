package org.ikasan.testharness.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GridRowsTest
{
    private static final List<Map<String, String>> ACTUAL = List.of(
        Map.of("Name", "ADMIN", "Description", "Administrators"),
        Map.of("Name", "User", "Description", "Read only"));

    @Test
    void exactRowsInAnyOrderMatch()
    {
        assertThatCode(() -> GridRows.assertRows(ACTUAL, List.of(
            Map.of("Name", "User", "Description", "Read only"),
            Map.of("Name", "ADMIN", "Description", "Administrators")), true, "roles")).doesNotThrowAnyException();
    }

    @Test
    void regexValuesMatchTheWholeCell()
    {
        assertThatCode(() -> GridRows.assertRows(ACTUAL, List.of(
            Map.of("Name", "/AD.*/", "Description", "/Admin\\w+/")), false, "roles")).doesNotThrowAnyException();
        assertThatThrownBy(() -> GridRows.assertRows(ACTUAL, List.of(
            Map.of("Name", "/AD/", "Description", "Administrators")), false, "roles"))
            .hasMessageContaining("no row matches");
    }

    @Test
    void everyColumnMustBeExpected()
    {
        assertThatThrownBy(() -> GridRows.assertRows(ACTUAL, List.of(Map.of("Name", "ADMIN")), false, "roles"))
            .hasMessageContaining("does not give a value for column(s) [Description]");
    }

    @Test
    void exactRejectsUnexpectedRows()
    {
        assertThatThrownBy(() -> GridRows.assertRows(ACTUAL, List.of(
            Map.of("Name", "ADMIN", "Description", "Administrators")), true, "roles"))
            .hasMessageContaining("1 unexpected row(s)");
        assertThatCode(() -> GridRows.assertRows(ACTUAL, List.of(
            Map.of("Name", "ADMIN", "Description", "Administrators")), false, "roles")).doesNotThrowAnyException();
    }

    @Test
    void emptyExpectationOfAnEmptyGrid()
    {
        assertThatCode(() -> GridRows.assertRows(List.of(), List.of(), true, "groups")).doesNotThrowAnyException();
        assertThatThrownBy(() -> GridRows.assertRows(ACTUAL, List.of(), true, "groups"))
            .hasMessageContaining("2 unexpected row(s)");
    }
}
