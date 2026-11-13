package org.ikasan.mongo.persistence.general;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Verifies the shipped index definitions in {@code src/main/resources/mongo/indexes}:
 * <ul>
 *   <li>{@code apply-indexes.js} applies them with mongosh, and is idempotent;</li>
 *   <li>the query shapes issued by the DAOs are answered from an index (no collection scan and, where the DAO
 *   sorts, no in-memory sort).</li>
 * </ul>
 * The filters below are the stored-field-name form that Spring Data sends to MongoDB for each DAO query.
 */
public class MongoIndexDefinitionsTest {

    private static final String DB = "indextest";
    private static final String IKASAN = "ikasan";
    private static final String PROCESS_EVENTS = "contextualisedScheduledProcessEventImpl";

    /** A typical dashboard time window: covers all seeded data, so only the name filters are selective. */
    private static final Document ALL_TIME = new Document("$gte", 0L).append("$lte", 10_000_000_000L);

    private static MongoDBContainer mongo;
    private static MongoClient client;
    private static MongoDatabase database;

    @BeforeClass
    public static void applyIndexes() throws Exception {
        mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongo.start();
        mongo.copyFileToContainer(MountableFile.forClasspathResource("mongo/indexes"), "/indexes");

        client = MongoClients.create(mongo.getReplicaSetUrl(DB));
        database = client.getDatabase(DB);

        seedData();

        // run twice: the script must be idempotent
        for (int run = 1; run <= 2; run++) {
            Container.ExecResult result = runApplyScript();
            assertEquals("apply-indexes.js run " + run + " failed:\n" + result.getStdout() + result.getStderr(),
                0, result.getExitCode());
        }
    }

    @AfterClass
    public static void stop() {
        if (client != null) {
            client.close();
        }
        if (mongo != null) {
            mongo.stop();
        }
    }

    /**
     * The query planner picks between candidate indexes by trial execution, so the collections need a realistic
     * distribution: many types/modules/agents spread over a long time span (time ranges are unselective, names are
     * selective).
     */
    private static void seedData() {
        List<Document> ikasan = new ArrayList<>();
        List<Document> events = new ArrayList<>();
        String[] types = {"EXCLUSION", "SCHEDULED_CONTEXT_INSTANCE", "WIRETAP", "METRIC"};
        Random random = new Random(42);
        for (int i = 0; i < 4000; i++) {
            ikasan.add(new Document("type", types[random.nextInt(types.length)])
                .append("moduleName", "m" + random.nextInt(50))
                .append("flowName", "f" + random.nextInt(10))
                .append("componentName", "c" + random.nextInt(100))
                .append("status", "S" + random.nextInt(5))
                .append("name", "n" + i)
                .append("timestamp", i * 1000L)
                .append("expiry", i * 1000L)
                .append("harvestReceivedTimestamp", i * 1000L)
                .append("event", "e" + i)
                .append("errorUri", i % 2 == 0 ? "u" + i : null));
            events.add(new Document("_id", (long) i)
                .append("agentName", "a" + random.nextInt(50))
                .append("jobGroup", "g" + random.nextInt(10))
                .append("jobName", "j" + random.nextInt(20))
                .append("fireTime", i * 1000L));
        }
        database.getCollection(IKASAN).insertMany(ikasan);
        database.getCollection(PROCESS_EVENTS).insertMany(events);
    }

    private static Container.ExecResult runApplyScript() throws Exception {
        return mongo.execInContainer("sh", "-c",
            "ATLAS_SEARCH=false mongosh --quiet mongodb://localhost:27017/" + DB + " --file /indexes/apply-indexes.js");
    }

    @Test
    public void all_indexes_are_created() {
        assertEquals(List.of("_id_",
                "type_1_timestamp_-1",
                "type_1_moduleName_1_timestamp_-1",
                "type_1_moduleName_1_flowName_1_timestamp_-1",
                "type_1_componentName_1_flowName_1",
                "type_1_flowName_1",
                "type_1_status_1",
                "type_1_name_1",
                "type_1_expiry_1",
                "expiry_1",
                "type_1_harvestReceivedTimestamp_1",
                "errorUri_1",
                "event_1"),
            indexNames(IKASAN));

        assertEquals(List.of("_id_",
                "fireTime_-1",
                "agentName_1_fireTime_-1",
                "agentName_1_jobGroup_1_jobName_1_fireTime_-1"),
            indexNames(PROCESS_EVENTS));
    }

    // ---- type + time (wiretap / error / exclusion / replay / system event / metrics lists) -------------------

    @Test
    public void type_and_time_range_sorted_by_newest_first() {
        assertIndexed(IKASAN, doc("type", "WIRETAP").append("timestamp", range(1, 2)), doc("timestamp", -1),
            "type_1_timestamp_-1");
    }

    @Test
    public void module_flow_and_time_range() {
        // errors / exclusions / hospital / replays / metrics by module(s) and flow(s)
        assertIndexed(IKASAN, doc("type", "EXCLUSION")
                .append("moduleName", doc("$in", List.of("m1", "m2")))
                .append("flowName", doc("$in", List.of("f1")))
                .append("timestamp", ALL_TIME), null,
            "type_1_moduleName_1_flowName_1_timestamp_-1");
        // metrics by module + time
        assertIndexed(IKASAN, doc("moduleName", "m1").append("timestamp", ALL_TIME).append("type", "METRIC"), null,
            "type_1_moduleName_1_timestamp_-1");
    }

    @Test
    public void context_and_instance_lookups_by_name() {
        // scheduled context / context instances by context name (moduleName), newest first
        assertIndexed(IKASAN, doc("type", "SCHEDULED_CONTEXT_INSTANCE").append("moduleName", "m1"),
            doc("timestamp", -1), "type_1_moduleName_1_timestamp_-1");
        // context views (moduleName = parent, flowName = context)
        assertIndexed(IKASAN, doc("type", "SCHEDULED_CONTEXT_VIEW").append("moduleName", "p").append("flowName", "c"),
            null, "type_1_moduleName_1_flowName_1_timestamp_-1");
    }

    // ---- scheduler jobs / job instances / notifications / profiles by context --------------------------------

    @Test
    public void jobs_and_notifications_by_context() {
        assertIndexed(IKASAN, doc("type", "QUARTZ_SCHEDULE_DRIVEN_JOB").append("componentName", "ctx"), null,
            "type_1_componentName_1_flowName_1");
        // findByContextIdAndJobName: type in [...], flowName = job, componentName in (ctx, GLOBAL_EVENT)
        assertIndexed(IKASAN, doc("type", doc("$in", List.of("A", "B")))
                .append("flowName", "job")
                .append("$or", List.of(doc("componentName", "ctx"), doc("componentName", "GLOBAL_EVENT"))), null,
            "type_1_componentName_1_flowName_1");
        // job instances of a context instance
        assertIndexed(IKASAN, doc("type", doc("$in", List.of("A", "B")))
                .append("componentName", doc("$in", List.of("i1", "i2"))), null,
            "type_1_componentName_1_flowName_1");
        // scheduler job list for a context sorted by job name
        assertIndexed(IKASAN, doc("type", "QUARTZ_SCHEDULE_DRIVEN_JOB").append("componentName", "ctx"),
            doc("flowName", -1), "type_1_componentName_1_flowName_1");
    }

    @Test
    public void audit_aggregates_by_context_instance() {
        assertIndexed(IKASAN, doc("flowName", "instance-1").append("isRepeatingJob", true)
            .append("type", "SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE"), null, "type_1_flowName_1");
    }

    @Test
    public void context_instances_by_status() {
        assertIndexed(IKASAN, doc("status", doc("$in", List.of("RUNNING", "WAITING")))
            .append("type", "SCHEDULED_CONTEXT_INSTANCE"), null, "type_1_status_1");
    }

    // ---- security ---------------------------------------------------------------------------------------------

    @Test
    public void security_entities_by_name() {
        assertIndexed(IKASAN, doc("name", "bob").append("type", "USER"), null, "type_1_name_1");
        assertIndexed(IKASAN, doc("name", doc("$in", List.of("a", "b"))).append("type", "PRINCIPAL"), null,
            "type_1_name_1");
        assertIndexed(IKASAN, doc("type", "PRINCIPAL"), doc("name", 1), "type_1_name_1");
    }

    // ---- lookups by id / uri ------------------------------------------------------------------------------------

    @Test
    public void lookups_by_error_uri_and_event_id() {
        assertIndexed(IKASAN, doc("errorUri", "uri").append("type", "ERROR"), null, "errorUri_1");
        assertIndexed(IKASAN, doc("event", "e1").append("type", "WIRETAP"), null, "event_1");
        // general search by event id: $or over event / errorUri
        assertIndexed(IKASAN, doc("$or", List.of(doc("event", "e1"), doc("errorUri", "e1"))), null,
            "event_1", "errorUri_1");
    }

    // ---- housekeeping / harvesting ------------------------------------------------------------------------------

    @Test
    public void housekeeping_and_harvesting() {
        assertIndexed(IKASAN, doc("expiry", doc("$lt", 5L)).append("type", "WIRETAP"), null, "type_1_expiry_1");
        assertIndexed(IKASAN, doc("expiry", doc("$gt", 0).append("$lt", 5L)), null, "expiry_1");
        assertIndexed(IKASAN, doc("type", "WIRETAP").append("harvestReceivedTimestamp", range(1, 2)), null,
            "type_1_harvestReceivedTimestamp_1");
    }

    // ---- scheduled process events (own collection) ------------------------------------------------------------

    @Test
    public void scheduled_process_events() {
        assertIndexed(PROCESS_EVENTS, doc("fireTime", ALL_TIME)
                .append("agentName", doc("$in", List.of("a1", "a2"))), doc("fireTime", -1),
            "agentName_1_fireTime_-1", "agentName_1_jobGroup_1_jobName_1_fireTime_-1");
        assertIndexed(PROCESS_EVENTS, doc("agentName", "a1").append("jobGroup", "g").append("jobName", "j")
            .append("fireTime", ALL_TIME), doc("fireTime", -1), "agentName_1_jobGroup_1_jobName_1_fireTime_-1");
        assertIndexed(PROCESS_EVENTS, doc("fireTime", doc("$lt", 5L)), null, "fireTime_-1");
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * Assert the winning plan reads one of the expected indexes, never scans the collection and, when a sort is
     * given, does not sort in memory.
     */
    private static void assertIndexed(String collection, Document filter, Document sort, String... expectedIndexes) {
        Document find = new Document("find", collection).append("filter", filter);
        if (sort != null) {
            find.append("sort", sort);
        }
        Document explain = database.runCommand(new Document("explain", find).append("verbosity", "queryPlanner"));
        Document winningPlan = ((Document) explain.get("queryPlanner")).get("winningPlan", Document.class);

        List<String> stages = new ArrayList<>();
        List<String> indexes = new ArrayList<>();
        collectPlan(winningPlan, stages, indexes);

        String plan = winningPlan.toJson();
        assertFalse("collection scan for " + filter.toJson() + ": " + plan, stages.contains("COLLSCAN"));
        assertTrue("expected one of " + List.of(expectedIndexes) + " for " + filter.toJson() + ": " + plan,
            indexes.stream().anyMatch(List.of(expectedIndexes)::contains));
        if (sort != null) {
            assertFalse("in-memory sort for " + filter.toJson() + " sort " + sort.toJson() + ": " + plan,
                stages.contains("SORT"));
        }
    }

    private static void collectPlan(Object node, List<String> stages, List<String> indexes) {
        if (node instanceof Document document) {
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                if ("stage".equals(entry.getKey()) && entry.getValue() instanceof String stage) {
                    stages.add(stage);
                } else if ("indexName".equals(entry.getKey()) && entry.getValue() instanceof String index) {
                    indexes.add(index);
                } else {
                    collectPlan(entry.getValue(), stages, indexes);
                }
            }
        } else if (node instanceof List<?> list) {
            list.forEach(item -> collectPlan(item, stages, indexes));
        }
    }

    private static List<String> indexNames(String collection) {
        List<String> names = new ArrayList<>();
        database.getCollection(collection).listIndexes().forEach(index -> names.add(index.getString("name")));
        return names;
    }

    private static Document doc(String key, Object value) {
        return new Document(key, value);
    }

    private static Document range(long gte, long lte) {
        return new Document("$gte", gte).append("$lte", lte);
    }
}
