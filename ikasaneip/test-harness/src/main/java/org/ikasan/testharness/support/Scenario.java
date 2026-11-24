package org.ikasan.testharness.support;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * One externalised test case from an expectations file (see {@link Expectations}).
 *
 * @param name     scenario name, shown as the test name
 * @param tags     scenario tags, used by the run's include/exclude selection
 * @param skip     null to run; otherwise the reason it is skipped
 * @param backends backends the scenario applies to; empty = all
 * @param input    free-form input, interpreted by the test
 * @param expect   free-form expectations, interpreted by the test
 */
public record Scenario(String name, List<String> tags, String skip, List<String> backends,
                       Map<String, Object> input, Map<String, Object> expect)
{
    private static final JsonMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    /** Bind the input to a typed record/class (unknown keys are ignored). */
    public <T> T input(Class<T> type)
    {
        return MAPPER.convertValue(input, type);
    }

    /** Bind the expectations to a typed record/class (unknown keys are ignored). */
    public <T> T expect(Class<T> type)
    {
        return MAPPER.convertValue(expect, type);
    }

    @Override
    public String toString()
    {
        return tags.isEmpty() ? name : name + " " + tags.stream().map(t -> "@" + TagFilter.normalise(t)).toList();
    }
}
