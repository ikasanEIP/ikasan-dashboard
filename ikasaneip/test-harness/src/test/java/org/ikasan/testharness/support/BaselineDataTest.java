package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.seed.Clock;
import org.ikasan.testharness.seed.Dataset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Sanity checks of the module's datasets and expectation files, so broken data fails fast without a browser. */
class BaselineDataTest
{
    private static final Clock CLOCK = new Clock(1_800_000_000_000L);

    @Test
    void everyDatasetLoadsWithResolvedTimestamps()
    {
        for (String name : Dataset.manifest().datasets().keySet())
        {
            Dataset dataset = Dataset.load(name, CLOCK);
            assertThat(dataset.documents()).as(name).isNotEmpty();
            for (Map<String, Object> d : dataset.documents())
            {
                assertThat(d.get("timestamp")).as(name + " " + d.get("id")).isNotInstanceOf(String.class);
            }
        }
    }

    @Test
    void coreDatasetHasTheDocumentsTheExpectationsAssume()
    {
        assertThat(Dataset.load("core", CLOCK).countByType()).containsEntry("wiretap", 12).containsEntry("error", 5);
    }

    @Test
    void everyExpectationFileParses() throws IOException
    {
        HarnessConfig config = ConfigLoader.load(Map.of(), new Properties());
        Path root = ConfigLoader.resolve("expectations");
        List<String> names;
        try (Stream<Path> files = Files.walk(root))
        {
            names = files.filter(p -> p.toString().endsWith(".yaml"))
                .filter(p -> !root.relativize(p).startsWith("env"))
                .map(p -> root.relativize(p).toString().replace('\\', '/').replaceAll("\\.yaml$", ""))
                .toList();
        }
        assertThat(names).isNotEmpty();
        for (String name : names)
        {
            List<Scenario> scenarios = Expectations.scenarios(config, CLOCK, name, List.of()).toList();
            assertThat(scenarios).as(name).isNotEmpty().allSatisfy(s -> assertThat(s.name()).isNotBlank());
        }
    }

    @Test
    void tagFilterSelection()
    {
        TagFilter none = new TagFilter(List.of(), List.of());
        assertThat(none.accepts(List.of("anything"))).isTrue();
        TagFilter smoke = new TagFilter(List.of("@Smoke"), List.of("slow"));
        assertThat(smoke.accepts(List.of("search", "smoke"))).isTrue();
        assertThat(smoke.accepts(List.of("search"))).isFalse();
        assertThat(smoke.accepts(List.of("smoke", "SLOW"))).isFalse();
    }
}
