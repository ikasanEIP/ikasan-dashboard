package org.ikasan.testharness.seed;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.BulkWriteOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.support.Compose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Mongo seeder. Every Ikasan entity lives in the single {@code ikasan} collection
 * ({@code org.ikasan.mongo.persistence.general.model.MongoConstants.IKASAN_COLLECTION_NAME}), keyed by {@code _id}
 * and told apart by {@code type}. Field names are the same as Solr's, so the only mapping is {@code id <-> _id}.
 */
public class MongoSeeder implements Seeder
{
    private static final Logger logger = LoggerFactory.getLogger(MongoSeeder.class);

    private final HarnessConfig config;
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoSeeder(HarnessConfig config)
    {
        this.config = config;
        this.client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(config.mongo().uri()))
            .applyToClusterSettings(b -> b.serverSelectionTimeout(30, TimeUnit.SECONDS))
            .build());
        this.collection = client.getDatabase(config.mongo().database()).getCollection(config.mongo().collection());
    }

    @Override
    public void wipe(List<String> preserve)
    {
        Bson filter = preserve.isEmpty() ? new Document()
            : Filters.and(Filters.nin("type", preserve), Filters.nin("_id", preserve));
        collection.deleteMany(filter);
    }

    @Override
    public void load(List<Map<String, Object>> documents)
    {
        List<ReplaceOneModel<Document>> ops = new ArrayList<>();
        for (Map<String, Object> d : documents)
        {
            Document doc = new Document(d);
            Object id = doc.remove("id");
            ops.add(new ReplaceOneModel<>(Filters.eq("_id", id), doc, new ReplaceOptions().upsert(true)));
        }
        for (int i = 0; i < ops.size(); i += 1000)
        {
            collection.bulkWrite(ops.subList(i, Math.min(i + 1000, ops.size())), new BulkWriteOptions().ordered(false));
        }
    }

    @Override
    public long count(String type)
    {
        return collection.countDocuments(Filters.eq("type", type));
    }

    @Override
    public List<Map<String, Object>> dump(List<String> types)
    {
        List<Map<String, Object>> out = new ArrayList<>();
        Bson filter = types == null || types.isEmpty() ? new Document() : Filters.in("type", types);
        for (Document d : collection.find(filter))
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(d.get("_id")));
            d.forEach((k, v) -> {
                if (!k.equals("_id") && !k.equals("_class"))
                {
                    m.put(k, v);
                }
            });
            out.add(m);
        }
        return out;
    }

    /**
     * Applies the repo's shipped index definitions with {@code apply-indexes.js} inside the Atlas Local container
     * (as {@code AtlasLocalSupport.applyShippedIndexes} does in the mongo module tests), then waits for the Atlas
     * Search index to be queryable. Only possible when the harness runs the compose stack; for an external
     * environment the indexes are assumed to exist.
     */
    @Override
    public void afterLoad()
    {
        HarnessConfig.Mongo mongo = config.mongo();
        if (!mongo.applyIndexes() || !config.compose().enabled())
        {
            return;
        }
        new Compose(config).exec("mongo", List.of("sh", "-c",
            "INDEX_DIR=/indexes ATLAS_SEARCH=" + mongo.atlasSearch() + " mongosh --quiet "
                + "'mongodb://localhost:27017/" + mongo.database() + "?directConnection=true' --file /indexes/apply-indexes.js"));
        if (mongo.atlasSearch())
        {
            waitForSearchIndexes(Duration.ofMinutes(2));
        }
    }

    private void waitForSearchIndexes(Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline)
        {
            List<Document> indexes = collection.listSearchIndexes().into(new ArrayList<>());
            if (!indexes.isEmpty() && indexes.stream().allMatch(i -> Boolean.TRUE.equals(i.getBoolean("queryable"))))
            {
                logger.info("Atlas Search indexes queryable: {}", indexes.stream().map(i -> i.getString("name")).toList());
                return;
            }
            sleep(1000);
        }
        throw new IllegalStateException("Timed out waiting for the Atlas Search index to become queryable");
    }

    private static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close()
    {
        client.close();
    }
}
