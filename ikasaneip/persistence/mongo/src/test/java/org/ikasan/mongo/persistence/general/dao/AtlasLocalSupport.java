package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.Container;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Shared MongoDB Atlas Local container for the {@code *AtlasTest} classes, which run DAOs against a real Atlas Search
 * index.
 *
 * <ul>
 *   <li>One container is started lazily and shared by every Atlas test class in the JVM (Testcontainers removes it when
 *   the JVM exits). Starting Atlas Local and building a search index is the slow part, so classes do not start their
 *   own.</li>
 *   <li>Each test class uses its own database, so seed data never collides.</li>
 *   <li>{@link #applyShippedIndexes(String)} creates the indexes with the shipped {@code apply-indexes.js} and index
 *   definitions, exactly as an operator would, so a field missing from a shipped search index fails the tests.</li>
 *   <li>Search indexes are built asynchronously; {@link #awaitSearchable} waits until the index has caught up with the
 *   seed data instead of sleeping a fixed time (which is not long enough on slow CI hosts).</li>
 * </ul>
 */
public final class AtlasLocalSupport {

    public static final String IMAGE = "mongodb/mongodb-atlas-local:7.0.14";
    public static final String SEARCH_INDEX = "default";

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration SEARCHABLE_TIMEOUT = Duration.ofMinutes(3);

    private static MongoDBAtlasLocalContainer container;
    private static MongoClient client;
    private static boolean indexFilesCopied;

    /** Databases of the aggregations sent with a {@code $search} or {@code $searchMeta} stage, in order. */
    private static final Queue<String> SEARCHES = new ConcurrentLinkedQueue<>();

    private static final CommandListener SEARCH_RECORDER = new CommandListener() {
        @Override
        public void commandStarted(CommandStartedEvent event) {
            if ("aggregate".equals(event.getCommandName())) {
                String pipeline = String.valueOf(event.getCommand().get("pipeline"));
                if (pipeline.contains("$search")) {
                    SEARCHES.add(event.getDatabaseName());
                }
            }
        }
    };

    private AtlasLocalSupport() {
    }

    /** The shared container, started on first use. */
    public static synchronized MongoDBAtlasLocalContainer container() {
        if (container == null) {
            MongoDBAtlasLocalContainer started = new MongoDBAtlasLocalContainer(IMAGE).withStartupTimeout(STARTUP_TIMEOUT);
            started.start();
            container = started;
        }
        return container;
    }

    /** A client for the shared container. Do not close it: other test classes use it. */
    public static synchronized MongoClient client() {
        if (client == null) {
            client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(container().getConnectionString()))
                .addCommandListener(SEARCH_RECORDER)
                .build());
        }
        return client;
    }

    /** A template for a fresh (dropped) database in the shared container. */
    public static MongoTemplate freshDatabase(String database) {
        client().getDatabase(database).drop();
        return new MongoTemplate(client(), database);
    }

    /** A search helper for {@code template} using the shipped index name, with Atlas Search enabled. */
    public static AtlasSearchQueryHelper searchHelper(MongoTemplate template) {
        return new AtlasSearchQueryHelper(template, SEARCH_INDEX, true);
    }

    /**
     * Create all indexes (standard and Atlas Search) in {@code database} with the shipped {@code apply-indexes.js}.
     * The collections should already exist (seed the data first); the script creates indexes for every shipped
     * definition either way.
     */
    public static void applyShippedIndexes(String database) {
        try {
            synchronized (AtlasLocalSupport.class) {
                if (!indexFilesCopied) {
                    container().copyFileToContainer(MountableFile.forClasspathResource("mongo/indexes"), "/indexes");
                    indexFilesCopied = true;
                }
            }
            Container.ExecResult result = container().execInContainer("sh", "-c",
                "mongosh --quiet 'mongodb://localhost:27017/" + database + "?directConnection=true'"
                    + " --file /indexes/apply-indexes.js");
            assertEquals("apply-indexes.js failed:\n" + result.getStdout() + result.getStderr(), 0,
                result.getExitCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not apply the shipped indexes", e);
        }
    }

    /**
     * Run {@code call} and assert it was answered by Atlas Search: at least one {@code $search}/{@code $searchMeta}
     * aggregation was sent to {@code database}. This catches both a fallback to regex and the helper choosing a
     * standard query (for example because a sort field is not sortable in the search index).
     *
     * @return the call's result
     */
    public static <T> T usingSearch(String database, Supplier<T> call) {
        SEARCHES.clear();
        T result = call.get();
        assertTrue("expected the query to use Atlas Search ($search) on database " + database + ", searches sent: "
            + SEARCHES, SEARCHES.contains(database));
        return result;
    }

    /**
     * {@link #usingSearch} for a paged call to a DAO method that returns a plain list (no total). With
     * {@code limit = 0} such a call asks for nothing, so it must not query the database at all; that is asserted
     * instead.
     */
    public static <T> T pagedListUsingSearch(String database, int limit, Supplier<T> call) {
        if (!MongoPaging.isCountOnly(limit)) {
            return usingSearch(database, call);
        }
        SEARCHES.clear();
        T result = call.get();
        assertTrue("a count-only call without a total must not search, searches sent: " + SEARCHES,
            SEARCHES.isEmpty());
        return result;
    }

    /**
     * Wait until the search index on {@code collection} can be queried and has indexed {@code expectedDocuments}
     * documents that have {@code indexedPath} (a field mapped in the search index and present on every seeded
     * document).
     */
    public static void awaitSearchable(String database, String collection, String indexedPath, long expectedDocuments) {
        Document searchMeta = new Document("$searchMeta", new Document("index", SEARCH_INDEX)
            .append("exists", new Document("path", indexedPath))
            .append("count", new Document("type", "total")));

        long deadline = System.currentTimeMillis() + SEARCHABLE_TIMEOUT.toMillis();
        long count = -1;
        RuntimeException lastError = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                Document meta = client().getDatabase(database).getCollection(collection)
                    .aggregate(List.of(searchMeta)).first();
                count = meta == null ? 0 : meta.get("count", Document.class).get("total", Number.class).longValue();
                if (count == expectedDocuments) {
                    return;
                }
            } catch (RuntimeException e) {
                lastError = e; // index not created or not queryable yet
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        fail("Search index on " + database + "." + collection + " not ready after " + SEARCHABLE_TIMEOUT
            + ": indexed " + count + " of " + expectedDocuments + " documents with '" + indexedPath + "'"
            + (lastError == null ? "" : "; last error: " + lastError.getMessage()));
    }
}
