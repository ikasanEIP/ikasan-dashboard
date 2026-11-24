package org.ikasan.testharness.seed;

import org.ikasan.testharness.config.ConfigLoader;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A baseline dataset: data/baseline/&lt;name&gt;/*.ndjson as listed in data/baseline/manifest.yaml.
 * <p>
 * Documents are backend-neutral. They use the shared Ikasan field names ({@code org.ikasan.spec.entity.EntityFields})
 * with the primary key in {@code id} (Solr shape); the Mongo seeder stores it as {@code _id}. Every document needs
 * string {@code id} and {@code type} fields. Time tokens ({@link Clock}) are resolved on load.
 *
 * @param name      dataset name
 * @param documents resolved documents; a later document with the same id replaces an earlier one
 */
public record Dataset(String name, List<Map<String, Object>> documents)
{
    public static final String BASELINE_DIR = "data/baseline";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final YAMLMapper YAML = YAMLMapper.builder().build();

    /** manifest.yaml: datasets: { name: { description, extends: [..], documents: [..] } } */
    public record Manifest(Map<String, Definition> datasets) {}

    public record Definition(String description, List<String> extends_, List<String> documents)
    {
        public Definition
        {
            extends_ = extends_ == null ? List.of() : extends_;
            documents = documents == null ? List.of() : documents;
        }
    }

    public static Manifest manifest()
    {
        Path file = ConfigLoader.resolve(BASELINE_DIR).resolve("manifest.yaml");
        try
        {
            Map<String, Map<String, Object>> raw = YAML.readValue(Files.readString(file),
                new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {}).get("datasets");
            Map<String, Definition> defs = new LinkedHashMap<>();
            raw.forEach((name, d) -> defs.put(name, new Definition((String) d.get("description"),
                stringList(d.get("extends")), stringList(d.get("documents")))));
            return new Manifest(defs);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object o)
    {
        return o == null ? List.of() : (List<String>) o;
    }

    public static Dataset load(String name, Clock clock)
    {
        List<Map<String, Object>> docs = new ArrayList<>();
        collect(name, clock, manifest(), new HashSet<>(), docs);
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        docs.forEach(d -> byId.put((String) d.get("id"), d));
        return new Dataset(name, List.copyOf(byId.values()));
    }

    private static void collect(String name, Clock clock, Manifest manifest, Set<String> seen, List<Map<String, Object>> out)
    {
        Definition def = manifest.datasets().get(name);
        if (def == null)
        {
            throw new IllegalArgumentException("Dataset '" + name + "' is not defined in " + BASELINE_DIR + "/manifest.yaml");
        }
        if (!seen.add(name))
        {
            throw new IllegalArgumentException("Dataset cycle via '" + name + "'");
        }
        for (String parent : def.extends_())
        {
            collect(parent, clock, manifest, seen, out);
        }
        Path dir = ConfigLoader.resolve(BASELINE_DIR).resolve(name);
        for (String rel : def.documents())
        {
            for (Map<String, Object> raw : readNdjson(dir.resolve(rel)))
            {
                Map<String, Object> doc = clock.resolve(raw);
                if (!(doc.get("id") instanceof String) || !(doc.get("type") instanceof String))
                {
                    throw new IllegalArgumentException(name + "/" + rel + ": every document needs string 'id' and 'type': " + raw);
                }
                out.add(doc);
            }
        }
    }

    public static List<Map<String, Object>> readNdjson(Path file)
    {
        try
        {
            List<Map<String, Object>> docs = new ArrayList<>();
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("//"))
                {
                    continue;
                }
                try
                {
                    docs.add(JSON.readValue(line, new TypeReference<LinkedHashMap<String, Object>>() {}));
                }
                catch (RuntimeException e)
                {
                    throw new IllegalArgumentException(file + ":" + (i + 1) + ": invalid JSON: " + e.getMessage(), e);
                }
            }
            return docs;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    /** Document count per type. */
    public Map<String, Integer> countByType()
    {
        Map<String, Integer> counts = new TreeMap<>();
        documents.forEach(d -> counts.merge((String) d.get("type"), 1, Integer::sum));
        return counts;
    }
}
