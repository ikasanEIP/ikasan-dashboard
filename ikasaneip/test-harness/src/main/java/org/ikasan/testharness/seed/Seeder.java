package org.ikasan.testharness.seed;

import org.ikasan.testharness.config.HarnessConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads a baseline into a datastore. One implementation per backend, both taking the same backend-neutral
 * documents (see {@link Dataset}).
 */
public interface Seeder extends AutoCloseable
{
    /** Remove every document except those whose {@code type} or id is in {@code preserve}. */
    void wipe(List<String> preserve);

    /** Insert or replace the documents. */
    void load(List<Map<String, Object>> documents);

    /** Number of documents of a type. */
    long count(String type);

    /** All documents, or those of the given types (null/empty = all). Used to capture a dataset. */
    List<Map<String, Object>> dump(List<String> types);

    /** Backend specific step after loading, e.g. index creation. */
    default void afterLoad() {}

    @Override
    void close();

    static Seeder create(HarnessConfig config)
    {
        return config.backend() == HarnessConfig.Backend.mongo ? new MongoSeeder(config) : new SolrSeeder(config);
    }

    /**
     * Wipe, load and verify: every type in the dataset must have exactly the expected number of documents.
     *
     * @return the document count per type that was seeded
     */
    static Map<String, Integer> seedAndVerify(Seeder seeder, Dataset dataset, List<String> preserve)
    {
        seeder.wipe(preserve);
        seeder.load(dataset.documents());
        seeder.afterLoad();
        Map<String, Integer> expected = dataset.countByType();
        List<String> mismatches = new ArrayList<>();
        expected.forEach((type, n) -> {
            long actual = seeder.count(type);
            if (actual != n)
            {
                mismatches.add(type + ": expected " + n + ", found " + actual);
            }
        });
        if (!mismatches.isEmpty())
        {
            throw new IllegalStateException("Seed verification failed:\n  " + String.join("\n  ", mismatches));
        }
        return expected;
    }
}
