package org.ikasan.testharness.cli;

import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.seed.Seeder;
import org.ikasan.testharness.support.Baseline;
import org.ikasan.testharness.support.Compose;
import org.ikasan.testharness.support.DashboardApi;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Command line tools, run from the test-harness directory with:
 * <pre>
 *   mvn -q exec:java -Dexec.args="seed"                    # stack up, baseline loaded, dashboard up
 *   mvn -q exec:java -Dexec.args="stack up"                # stack up without seeding
 *   mvn -q exec:java -Dexec.args="stack down"              # remove the stack and its volumes
 *   mvn -q exec:java -Dexec.args="capture my-dataset [type,type...]"
 * </pre>
 * The environment is chosen as for the tests: {@code -Dharness.env=compose-solr} or {@code HARNESS_ENV}.
 */
public final class HarnessCli
{
    /** Epoch-millis fields rewritten as time tokens by capture. */
    private static final Set<String> TIME_FIELDS = Set.of("timestamp", "modifiedTimestamp", "expiry",
        "harvestReceivedTimestamp", "startTime", "endTime", "createdDateTime", "updatedDateTime", "executionTimestamp");

    /** The dashboard creates these on start-up, so they are never part of a baseline. */
    private static final Set<String> NEVER_CAPTURED = Set.of("securityUser", "securityRole", "securityPolicy",
        "securityPrincipal", "securityAuthenticationMethod");

    private HarnessCli() {}

    public static void main(String[] args) throws IOException
    {
        HarnessConfig config = ConfigLoader.get();
        String command = args.length == 0 ? "help" : args[0];
        switch (command)
        {
            case "seed" -> {
                Baseline.Result r = Baseline.establish(config);
                System.out.println("[harness] baseline ready at " + config.dashboard().url() + ", clock anchor "
                    + Instant.ofEpochMilli(r.clock().anchor()));
            }
            case "stack" -> stack(config, args.length > 1 ? args[1] : "");
            case "capture" -> {
                if (args.length < 2)
                {
                    throw new IllegalArgumentException("usage: capture <dataset> [type,type...]");
                }
                capture(config, args[1], args.length > 2 ? Arrays.asList(args[2].split(",")) : List.of());
            }
            default -> System.out.println("usage: seed | stack up|down | capture <dataset> [type,type...]");
        }
    }

    private static void stack(HarnessConfig config, String action)
    {
        Compose compose = new Compose(config);
        switch (action)
        {
            case "up" -> {
                compose.upStore();
                compose.restartDashboard();
                DashboardApi.waitForDashboard(config);
                System.out.println("[harness] dashboard up at " + config.dashboard().url() + " (" + config.backend() + ")");
            }
            case "down" -> compose.down();
            default -> throw new IllegalArgumentException("usage: stack up|down");
        }
    }

    /**
     * Write the current datastore contents as data/baseline/&lt;dataset&gt;/&lt;type&gt;.ndjson. Timestamps become
     * {@code {{now-…}}} tokens relative to now, so the captured data stays recent whenever it is replayed.
     */
    static void capture(HarnessConfig config, String dataset, List<String> types) throws IOException
    {
        List<Map<String, Object>> docs;
        try (Seeder seeder = Seeder.create(config))
        {
            docs = seeder.dump(types);
        }
        long anchor = System.currentTimeMillis();
        Map<String, List<Map<String, Object>>> byType = new TreeMap<>();
        for (Map<String, Object> d : docs)
        {
            String type = String.valueOf(d.get("type"));
            boolean wanted = types.contains(type) || (!NEVER_CAPTURED.contains(type) && !"dashboardPlatformSetup".equals(d.get("id")));
            if (wanted)
            {
                byType.computeIfAbsent(type, t -> new ArrayList<>()).add(tokenise(d, anchor));
            }
        }
        Path dir = ConfigLoader.resolve("data/baseline").resolve(dataset);
        Files.createDirectories(dir);
        JsonMapper json = JsonMapper.builder().build();
        StringBuilder manifest = new StringBuilder("  " + dataset + ":\n    documents:\n");
        for (Map.Entry<String, List<Map<String, Object>>> e : byType.entrySet())
        {
            e.getValue().sort((a, b) -> String.valueOf(a.get("id")).compareTo(String.valueOf(b.get("id"))));
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> d : e.getValue())
            {
                sb.append(json.writeValueAsString(d)).append('\n');
            }
            Path file = dir.resolve(e.getKey() + ".ndjson");
            Files.writeString(file, sb.toString());
            manifest.append("      - ").append(e.getKey()).append(".ndjson\n");
            System.out.println("[harness] " + e.getKey() + ": " + e.getValue().size() + " -> " + file);
        }
        System.out.println("\nAdd to data/baseline/manifest.yaml:\n\n" + manifest);
    }

    private static Map<String, Object> tokenise(Map<String, Object> doc, long anchor)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        doc.forEach((k, v) -> out.put(k, TIME_FIELDS.contains(k) && v instanceof Number n && n.longValue() > 1_000_000_000_000L
            ? token(n.longValue() - anchor) : v));
        return out;
    }

    static String token(long diff)
    {
        if (diff == 0)
        {
            return "{{now}}";
        }
        long[] sizes = {86_400_000L, 3_600_000L, 60_000L, 1000L};
        String[] units = {"d", "h", "m", "s"};
        String sign = diff > 0 ? "+" : "-";
        long abs = Math.abs(diff);
        for (int i = 0; i < sizes.length; i++)
        {
            if (abs % sizes[i] == 0)
            {
                return "{{now" + sign + abs / sizes[i] + units[i] + "}}";
            }
        }
        return "{{now" + sign + Math.round(abs / 1000.0) + "s}}";
    }
}
