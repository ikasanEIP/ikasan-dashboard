package org.ikasan.testharness.junit;

import org.ikasan.testharness.support.Expectations;
import org.ikasan.testharness.support.Scenario;

import java.util.List;
import java.util.stream.Stream;

/**
 * Scenario source for {@code @MethodSource} factories:
 * <pre>
 * static Stream&lt;Scenario&gt; scenarios() { return Scenarios.load(List.of("search/wiretap"), "search"); }
 * </pre>
 * Scenarios come from the given expectation files, filtered by backend and the run's tag selection (class tags
 * plus scenario tags), with time tokens resolved by the clock the dataset was seeded with.
 */
public final class Scenarios
{
    private Scenarios() {}

    public static Stream<Scenario> load(List<String> files, String... classTags)
    {
        HarnessSession session = HarnessSession.current();
        return files.stream().flatMap(f -> Expectations.scenarios(session.config(), session.clock(), f, List.of(classTags)));
    }

    public static Stream<Scenario> load(String file, String... classTags)
    {
        return load(List.of(file), classTags);
    }

    /**
     * One named scenario of an expectations file, for tests that are not parameterized. Not filtered by tags:
     * the test method's own @Tag selection applies.
     */
    public static Scenario one(String file, String name)
    {
        HarnessSession session = HarnessSession.current();
        return Expectations.all(session.config(), session.clock(), file)
            .filter(s -> s.name().equals(name)).findFirst()
            .orElseGet(() -> {
                throw new IllegalArgumentException("No scenario '" + name + "' in expectations/" + file + ".yaml");
            });
    }
}
