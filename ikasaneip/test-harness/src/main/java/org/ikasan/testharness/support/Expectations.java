package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.seed.Clock;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Externalised expectations: expectations/&lt;feature&gt;/&lt;name&gt;.yaml
 * <pre>
 * scenarios:
 *   - name: wiretap events for a module
 *     tags: [search, wiretap]       # selection via run.include / run.exclude
 *     skip: false                   # or a reason string
 *     backends: [mongo, solr]       # optional
 *     input:  { ... }               # interpreted by the test
 *     expect: { ... }               # interpreted by the test
 * </pre>
 * Values that genuinely vary can reference a named pattern of the file: a top-level
 * {@code patterns: { timestamp: '/regex/' }} map, used as {@code "@timestamp"} in any value.
 * <p>
 * The directories in {@code expectations.paths} are read in order. A later file's scenario with the same name is
 * deep-merged over the earlier one, so an environment can change individual values
 * (expectations/env/&lt;env&gt;/...). Time tokens are resolved with the clock the dataset was seeded with.
 */
public final class Expectations
{
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    private Expectations() {}

    /**
     * Scenarios of one expectations file for the active backend and tag selection, tokens resolved.
     *
     * @param name e.g. {@code search/wiretap}
     */
    public static Stream<Scenario> scenarios(HarnessConfig config, Clock clock, String name, List<String> classTags)
    {
        TagFilter filter = TagFilter.of(config);
        return load(config, name).stream()
            .filter(s -> s.get("backends") == null || stringList(s.get("backends")).contains(config.backend().name()))
            .map(s -> toScenario(s, clock))
            .filter(s -> {
                List<String> all = new ArrayList<>(classTags);
                all.addAll(s.tags());
                return filter.accepts(all);
            });
    }

    /** All scenarios of one expectations file for the active backend, tokens resolved, ignoring tag selection. */
    public static Stream<Scenario> all(HarnessConfig config, Clock clock, String name)
    {
        return load(config, name).stream()
            .filter(s -> s.get("backends") == null || stringList(s.get("backends")).contains(config.backend().name()))
            .map(s -> toScenario(s, clock));
    }

    /** Raw scenarios after layering, tokens not resolved. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> load(HarnessConfig config, String name)
    {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        Map<String, Object> patterns = new LinkedHashMap<>();
        boolean found = false;
        for (String dir : config.expectations().paths())
        {
            Path file = ConfigLoader.resolve(dir).resolve(name + ".yaml");
            if (!Files.exists(file))
            {
                continue;
            }
            found = true;
            try
            {
                Map<String, Object> doc = YAML.readValue(Files.readString(file), new TypeReference<Map<String, Object>>() {});
                if (doc.get("patterns") instanceof Map<?, ?> named)
                {
                    named.forEach((k, v) -> patterns.put(String.valueOf(k), v));
                }
                for (Object o : (List<Object>) doc.getOrDefault("scenarios", List.of()))
                {
                    Map<String, Object> s = (Map<String, Object>) o;
                    String scenarioName = String.valueOf(s.get("name"));
                    byName.put(scenarioName, (Map<String, Object>) merge(byName.get(scenarioName), s));
                }
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Cannot read " + file, e);
            }
        }
        if (!found)
        {
            throw new IllegalArgumentException("No expectations file '" + name + ".yaml' in " + config.expectations().paths());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byName.values().forEach(s -> out.add((Map<String, Object>) withPatterns(s, patterns, name)));
        return out;
    }

    /** Replace "@name" values with the file's named pattern. */
    @SuppressWarnings("unchecked")
    private static Object withPatterns(Object value, Map<String, Object> patterns, String file)
    {
        if (value instanceof String s && s.startsWith("@") && s.length() > 1)
        {
            Object pattern = patterns.get(s.substring(1));
            if (pattern == null)
            {
                throw new IllegalArgumentException("expectations/" + file + ".yaml: unknown pattern '" + s
                    + "' (define it under 'patterns:')");
            }
            return pattern;
        }
        if (value instanceof Map<?, ?> map)
        {
            Map<String, Object> out = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> out.put(k, withPatterns(v, patterns, file)));
            return out;
        }
        if (value instanceof List<?> list)
        {
            return list.stream().map(v -> withPatterns(v, patterns, file)).toList();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Object merge(Object base, Object override)
    {
        if (!(override instanceof Map<?, ?> o) || !(base instanceof Map<?, ?> b))
        {
            return override;
        }
        Map<String, Object> out = new LinkedHashMap<>((Map<String, Object>) b);
        ((Map<String, Object>) o).forEach((k, v) -> out.put(k, merge(out.get(k), v)));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Scenario toScenario(Map<String, Object> s, Clock clock)
    {
        Object skip = s.get("skip");
        String skipReason = skip == null || Boolean.FALSE.equals(skip) ? null
            : Boolean.TRUE.equals(skip) ? "skipped in expectations" : String.valueOf(skip);
        Map<String, Object> input = (Map<String, Object>) s.getOrDefault("input", Map.of());
        Map<String, Object> expect = (Map<String, Object>) s.getOrDefault("expect", Map.of());
        return new Scenario(String.valueOf(s.get("name")), stringList(s.get("tags")), skipReason,
            stringList(s.get("backends")), clock.resolve(input == null ? Map.of() : input),
            clock.resolve(expect == null ? Map.of() : expect));
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object o)
    {
        return o == null ? List.of() : ((List<Object>) o).stream().map(String::valueOf).toList();
    }
}
