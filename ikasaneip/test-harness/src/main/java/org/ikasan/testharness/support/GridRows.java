package org.ikasan.testharness.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compares a grid's rows with expected rows from an expectations file:
 * <pre>
 * expect:
 *   rows:                         # every expected row, with a value for every column of the grid
 *     - { Module Name: harness-module-a, Flow Name: Order Flow, Timestamp: "{{now-5m|table}}" }
 *   rowsExact: true               # the grid holds exactly these rows (any order), nothing else
 * </pre>
 * A value is matched exactly; a value written as {@code /regex/} must match the whole cell. Expected rows must name
 * every column the grid shows, so an expectation cannot silently check only part of a row.
 */
public final class GridRows
{
    private GridRows() {}

    /**
     * @param actual   rows read with {@link Grid#rows()}
     * @param expected rows from the expectations file (tokens already resolved)
     * @param exact    the grid must hold exactly the expected rows
     * @param what     description used in failure messages, e.g. the view or scenario
     * @throws AssertionError describing every problem found
     */
    public static void assertRows(List<Map<String, String>> actual, List<Map<String, Object>> expected, boolean exact,
                                  String what)
    {
        List<String> problems = new ArrayList<>();
        Set<String> columns = new LinkedHashSet<>();
        actual.forEach(r -> columns.addAll(r.keySet()));

        for (int i = 0; i < expected.size(); i++)
        {
            Set<String> missing = new LinkedHashSet<>(columns);
            missing.removeAll(expected.get(i).keySet());
            if (!missing.isEmpty() && !actual.isEmpty())
            {
                problems.add("expected row " + (i + 1) + " does not give a value for column(s) " + missing
                    + " - expectations must cover every column");
            }
        }

        List<Map<String, String>> unmatched = new ArrayList<>(actual);
        for (Map<String, Object> row : expected)
        {
            Map<String, String> match = unmatched.stream().filter(a -> matches(a, row)).findFirst().orElse(null);
            if (match == null)
            {
                problems.add("no row matches " + row);
            }
            else
            {
                unmatched.remove(match);
            }
        }
        if (exact && !unmatched.isEmpty())
        {
            problems.add(unmatched.size() + " unexpected row(s): " + unmatched);
        }
        if (!problems.isEmpty())
        {
            throw new AssertionError(what + ":\n  " + String.join("\n  ", problems) + "\n\nactual rows:\n  "
                + String.join("\n  ", actual.stream().map(Object::toString).toList()));
        }
    }

    static boolean matches(Map<String, String> actual, Map<String, Object> expected)
    {
        for (Map.Entry<String, Object> e : expected.entrySet())
        {
            String cell = actual.get(e.getKey());
            if (cell == null || !valueMatches(cell, String.valueOf(e.getValue())))
            {
                return false;
            }
        }
        return true;
    }

    static boolean valueMatches(String cell, String expected)
    {
        if (expected.length() >= 2 && expected.startsWith("/") && expected.endsWith("/"))
        {
            return Pattern.compile(expected.substring(1, expected.length() - 1), Pattern.DOTALL).matcher(cell).matches();
        }
        return cell.equals(expected);
    }

    /** Convenience for tests: the 'rows' list of an expectation map, or null. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> expectedRows(Map<String, Object> expect)
    {
        Object rows = expect.get("rows");
        if (rows == null)
        {
            return null;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object r : (List<Object>) rows)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            ((Map<String, Object>) r).forEach((k, v) -> row.put(k, v));
            out.add(row);
        }
        return out;
    }
}
