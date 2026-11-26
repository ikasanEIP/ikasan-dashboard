package org.ikasan.testharness.config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the harness configuration. Layers, later wins:
 * <ol>
 *     <li>{@code config/harness.default.yaml}</li>
 *     <li>{@code config/environments/<env>.yaml} where env comes from {@code HARNESS_ENV} / {@code -Dharness.env}
 *     (default {@code compose-mongo})</li>
 *     <li>{@code config/local.yaml} (optional, git-ignored)</li>
 *     <li>system properties {@code -Dharness.<section>.<key>=value}</li>
 *     <li>environment variables {@code HARNESS_<SECTION>_<KEY>}</li>
 * </ol>
 * Keys are matched case-insensitively and underscores are ignored, so {@code HARNESS_RUN_EXPECTTIMEOUTSECONDS} and
 * {@code HARNESS_RUN_EXPECT_TIMEOUT_SECONDS} both set {@code run.expectTimeoutSeconds}. List values are
 * comma separated. All relative paths in the configuration resolve against {@link #harnessRoot()}.
 */
public final class ConfigLoader
{
    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static volatile HarnessConfig cached;
    private static final Map<String, HarnessConfig> BY_ENV = new java.util.concurrent.ConcurrentHashMap<>();

    /** The environment name that runs every end-to-end test on each backend (MongoBackendSuite, SolrBackendSuite). */
    public static final String ALL = "all";

    private ConfigLoader() {}

    /** The configuration for this JVM, loaded once from the process environment and system properties. */
    public static HarnessConfig get()
    {
        if (cached == null)
        {
            synchronized (ConfigLoader.class)
            {
                if (cached == null)
                {
                    cached = load(System.getenv(), System.getProperties());
                }
            }
        }
        return cached;
    }

    /**
     * The configuration of a named environment (e.g. {@code compose-solr}), loaded once per name: one run can use
     * several environments, as the per-backend suites do. A blank name means {@link #get()}.
     */
    public static HarnessConfig get(String envName)
    {
        if (envName == null || envName.isBlank())
        {
            return get();
        }
        return BY_ENV.computeIfAbsent(envName, name -> load(name, System.getenv(), System.getProperties()));
    }

    /** Root of the test-harness module: {@code -Dharness.root}, else the working directory. */
    public static Path harnessRoot()
    {
        return Path.of(System.getProperty("harness.root", System.getProperty("user.dir"))).toAbsolutePath().normalize();
    }

    /** Resolve a configured path against the harness root. */
    public static Path resolve(String path)
    {
        Path p = Path.of(path);
        return p.isAbsolute() ? p : harnessRoot().resolve(p).normalize();
    }

    public static HarnessConfig load(Map<String, String> env, Map<Object, Object> props)
    {
        return load(null, env, props);
    }

    /**
     * Load the configuration of the named environment, or (blank name) of {@code HARNESS_ENV}, else
     * {@code -Dharness.env}, else {@code compose-mongo}.
     */
    public static HarnessConfig load(String envName, Map<String, String> env, Map<Object, Object> props)
    {
        envName = firstNonBlank(envName, env.get("HARNESS_ENV"), (String) props.get("harness.env"), "compose-mongo");
        if (ALL.equals(envName))
        {
            // -Dharness.env=all: each per-backend suite names its own environment; anything asking for the run's
            // configuration (timeouts, say, which are the same for both) gets the first backend's
            envName = "compose-mongo";
        }
        envName = onStack(envName, firstNonBlank(env.get("HARNESS_STACK"), (String) props.get("harness.stack"), ""));
        Path configDir = harnessRoot().resolve("config");
        Path envFile = configDir.resolve("environments").resolve(envName + ".yaml");
        if (!Files.exists(envFile))
        {
            throw new IllegalArgumentException("Unknown harness environment '" + envName + "': " + envFile + " does not exist");
        }

        ObjectNode merged = readYaml(configDir.resolve("harness.default.yaml"));
        deepMerge(merged, readYaml(envFile));
        deepMerge(merged, readYaml(configDir.resolve("local.yaml")));
        for (Map.Entry<Object, Object> e : props.entrySet())
        {
            String key = String.valueOf(e.getKey());
            if (key.startsWith("harness.") && !key.equals("harness.env") && !key.equals("harness.root")
                && !key.equals("harness.stack"))
            {
                override(merged, key.substring("harness.".length()).split("\\.", 2), String.valueOf(e.getValue()));
            }
        }
        for (Map.Entry<String, String> e : env.entrySet())
        {
            if (e.getKey().startsWith("HARNESS_") && !e.getKey().equals("HARNESS_ENV") && !e.getKey().equals("HARNESS_STACK"))
            {
                overrideFromEnv(merged, e.getKey().substring("HARNESS_".length()), e.getValue());
            }
        }
        merged.put("env", envName);

        // ${env} in expectation paths layers per-environment overrides
        JsonNode paths = merged.path("expectations").path("paths");
        if (paths instanceof ArrayNode array)
        {
            for (int i = 0; i < array.size(); i++)
            {
                array.set(i, array.get(i).asString().replace("${env}", envName));
            }
        }
        return JSON.treeToValue(merged, HarnessConfig.class);
    }

    /**
     * The per-backend suites (-Dharness.env=all) name the compose environments; {@code harness.stack} (or
     * HARNESS_STACK) runs them on another stack instead: {@code local} turns compose-solr into local-solr, for
     * machines without docker.
     */
    static String onStack(String envName, String stack)
    {
        if (stack == null || stack.isBlank() || stack.equals("compose") || !envName.startsWith("compose-"))
        {
            return envName;
        }
        return stack + "-" + envName.substring("compose-".length());
    }

    private static ObjectNode readYaml(Path file)
    {
        if (!Files.exists(file))
        {
            return JsonNodeFactory.instance.objectNode();
        }
        try
        {
            JsonNode node = YAML.readTree(Files.readString(file));
            return node instanceof ObjectNode o ? o : JsonNodeFactory.instance.objectNode();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    static void deepMerge(ObjectNode base, ObjectNode override)
    {
        for (Map.Entry<String, JsonNode> e : override.properties())
        {
            JsonNode existing = base.get(e.getKey());
            if (existing instanceof ObjectNode existingObject && e.getValue() instanceof ObjectNode overrideObject)
            {
                deepMerge(existingObject, overrideObject);
            }
            else
            {
                base.set(e.getKey(), e.getValue());
            }
        }
    }

    private static String norm(String s)
    {
        return s.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String findKey(ObjectNode node, String wanted)
    {
        Iterator<String> names = node.propertyNames().iterator();
        while (names.hasNext())
        {
            String name = names.next();
            if (norm(name).equals(norm(wanted)))
            {
                return name;
            }
        }
        return null;
    }

    /** {@code HARNESS_DASHBOARD_URL}: the first segment that names a section selects it, the rest names the key. */
    private static void overrideFromEnv(ObjectNode root, String rest, String value)
    {
        Iterator<String> names = root.propertyNames().iterator();
        while (names.hasNext())
        {
            String section = names.next();
            String prefix = section.toUpperCase(Locale.ROOT);
            if (rest.equals(prefix) || rest.startsWith(prefix + "_"))
            {
                String remainder = rest.substring(prefix.length()).replaceFirst("^_", "");
                override(root, remainder.isEmpty() ? new String[]{section} : new String[]{section, remainder}, value);
                return;
            }
        }
    }

    private static void override(ObjectNode root, String[] path, String value)
    {
        String sectionKey = findKey(root, path[0]);
        if (sectionKey == null)
        {
            return;
        }
        if (path.length == 1)
        {
            root.set(sectionKey, coerce(root.get(sectionKey), value));
            return;
        }
        if (root.get(sectionKey) instanceof ObjectNode section)
        {
            String key = findKey(section, path[1]);
            if (key != null)
            {
                section.set(key, coerce(section.get(key), value));
            }
        }
    }

    /** Coerce a string override to the type of the value it replaces. */
    private static JsonNode coerce(JsonNode current, String raw)
    {
        JsonNodeFactory f = JsonNodeFactory.instance;
        if (current != null && current.isArray())
        {
            ArrayNode array = f.arrayNode();
            Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(array::add);
            return array;
        }
        if (current != null && current.isBoolean())
        {
            return f.booleanNode(raw.equalsIgnoreCase("true") || raw.equals("1"));
        }
        if (current != null && current.isNumber())
        {
            return f.numberNode(Long.parseLong(raw.trim()));
        }
        return f.stringNode(raw);
    }

    private static String firstNonBlank(String... values)
    {
        for (String v : values)
        {
            if (v != null && !v.isBlank())
            {
                return v;
            }
        }
        return null;
    }
}
