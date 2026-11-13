package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.ikasan.mongo.persistence.general.repository.MongoIkasanDocumentRepository;
import org.ikasan.mongo.persistence.systemevent.dao.MongoSystemEventDaoImpl;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventSearchFilter;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.search.SearchResults;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Integration tests running the DAOs against MongoDB Atlas Local with the shipped search index definition
 * ({@code mongo/indexes/ikasan.search.json}). They verify that filters, sort, count and pagination embedded in the
 * {@code $search} stage return the same results the regex fallback would, and that Atlas Search is actually used
 * (no silent fallback).
 */
public class AtlasSearchDaoAtlasTest {

    private static final String DB = "atlasdaotest";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static MongoDBAtlasLocalContainer atlasContainer;
    private static MongoClient mongoClient;
    private static MongoTemplate mongoTemplate;
    private static AtlasSearchQueryHelper helper;
    private static MongoGeneralDaoImpl generalDao;
    private static MongoSystemEventDaoImpl systemEventDao;

    @BeforeClass
    public static void startContainer() throws Exception {
        atlasContainer = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.14");
        atlasContainer.start();

        mongoClient = MongoClients.create(atlasContainer.getConnectionString());
        mongoTemplate = new MongoTemplate(mongoClient, DB);
        helper = new AtlasSearchQueryHelper(mongoTemplate);

        MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
        generalDao = new MongoGeneralDaoImpl(factory.getRepository(MongoIkasanDocumentRepository.class),
            mongoTemplate, helper);
        systemEventDao = new MongoSystemEventDaoImpl(factory.getRepository(MongoSystemEventRepository.class),
            mongoTemplate, 30, helper);

        MongoDatabase database = mongoClient.getDatabase(DB);
        applyShippedIndexes();

        insertTestData();
        awaitSearchable(database, 8);
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (atlasContainer != null) {
            atlasContainer.stop();
        }
    }

    @Before
    public void assertAtlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    // ---------------------------------------------------------------------------------------------------------
    // General DAO: text + type/module/flow/time/eventId filters, sort, count, pagination, negation
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void general_text_search_with_type_filter_sorted_by_timestamp_desc() {
        IkasanDocumentSearchResults results = generalDao.search("order", 0, 0, 0, 10,
            List.of("wiretap"), false, null, null);

        assertEquals(List.of("d2", "d1"), ids(results));
        assertEquals(2, results.getTotalNumberOfResults());
        assertTrue(helper.isAtlasSearchAvailable());
    }

    @Test
    public void general_text_search_with_module_and_flow_filters() {
        IkasanDocumentSearchResults results = generalDao.search(Set.of("m1"), Set.of("f1"), "order",
            0, 0, 10, false, null, null);

        assertEquals(List.of("d1"), ids(results));
        assertEquals(1, results.getTotalNumberOfResults());
    }

    @Test
    public void general_text_search_with_time_range_and_ascending_sort() {
        IkasanDocumentSearchResults results = generalDao.search("order", NOW - 150 * 60_000L, NOW, 0, 10,
            null, false, "timestamp", MongoGeneralDaoImpl.ASCENDING);

        assertEquals(List.of("d2", "d3"), ids(results));
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void general_text_search_with_event_id_matching_error_uri() {
        IkasanDocumentSearchResults results = generalDao.search(null, null, null, "uri-3", "order",
            0, 0, 0, 10, null, false, null, null);

        assertEquals(List.of("d3"), ids(results));
    }

    @Test
    public void general_negated_text_search() {
        IkasanDocumentSearchResults results = generalDao.search("order", 0, 0, 0, 10,
            List.of("wiretap"), true, null, null);

        assertEquals(List.of("d4"), ids(results));
        assertEquals(1, results.getTotalNumberOfResults());
    }

    @Test
    public void general_pagination_reports_total_from_search_meta() {
        IkasanDocumentSearchResults page = generalDao.search("order", 0, 0, 1, 1,
            List.of("wiretap"), false, null, null);
        assertEquals(List.of("d1"), ids(page));
        assertEquals(2, page.getTotalNumberOfResults());

        // offset past the end: total comes from $searchMeta
        IkasanDocumentSearchResults beyond = generalDao.search("order", 0, 0, 5, 1,
            List.of("wiretap"), false, null, null);
        assertTrue(ids(beyond).isEmpty());
        assertEquals(2, beyond.getTotalNumberOfResults());
    }

    // ---------------------------------------------------------------------------------------------------------
    // System event DAO: case-insensitive substring filters + Date range + sort + count
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void system_event_substring_filters_are_case_insensitive() {
        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setActor("ADMIN");

        SearchResults<SystemEvent> results = systemEventDao.findByFilter(filter, 10, 0, null, null);

        assertEquals(List.of("admin.user", "super-admin"), actors(results));
        assertEquals(2, results.getTotalNumberOfResults());
        assertTrue(helper.isAtlasSearchAvailable());
    }

    @Test
    public void system_event_substring_and_date_range() {
        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setAction("deploy");
        filter.setStartTime(NOW - 90 * 60_000L);
        filter.setEndTime(NOW + 1);

        SearchResults<SystemEvent> results = systemEventDao.findByFilter(filter, 10, 0, null, null);

        assertEquals(List.of("admin.user"), actors(results));
    }

    @Test
    public void system_event_search_term_matches_any_field() {
        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setSearchTerm("Flow-B");

        SearchResults<SystemEvent> results = systemEventDao.findByFilter(filter, 10, 0, "actor", "ASCENDING");

        assertEquals(List.of("operator", "super-admin"), actors(results));
        assertEquals(2, results.getTotalNumberOfResults());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Test data / setup helpers
    // ---------------------------------------------------------------------------------------------------------

    private static void insertTestData() {
        generalDao.saveOrUpdate(List.of(
            doc("d1", "wiretap", "m1", "f1", NOW - 3 * HOUR, "new order 12345 created", null, null),
            doc("d2", "wiretap", "m1", "f2", NOW - 2 * HOUR, "order shipped", null, null),
            doc("d3", "error", "m2", "f1", NOW - HOUR, null, "order failed", "uri-3"),
            doc("d4", "wiretap", "m2", "f1", NOW, "invoice raised", null, null)));

        systemEventDao.save(List.of(
            systemEvent(1L, "admin.user", "deploy", "module flow-A", NOW - HOUR),
            systemEvent(2L, "super-admin", "undeploy", "module FLOW-B", NOW - 2 * HOUR),
            systemEvent(3L, "operator", "restart", "flow-b component", NOW - 3 * HOUR),
            systemEvent(4L, "guest", "deploy", "other", NOW - 4 * HOUR)));
    }

    private static MongoIkasanDocument doc(String id, String type, String module, String flow, long timestamp,
                                           String payload, String errorMessage, String errorUri) {
        MongoIkasanDocument doc = new MongoIkasanDocument();
        doc.setId(id);
        doc.setType(type);
        doc.setModuleName(module);
        doc.setFlowName(flow);
        doc.setComponentName("component");
        doc.setTimeStamp(timestamp);
        doc.setEvent(payload);              // stored in the "payload" field
        doc.setEventId("event-" + id);      // stored in the "event" field
        doc.setErrorMessage(errorMessage);
        doc.setErrorUri(errorUri);
        return doc;
    }

    private static SystemEvent systemEvent(Long id, String actor, String action, String subject, long timestamp) {
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
        event.setSystemEventId(id);
        event.setModuleName("dashboard");
        event.setActor(actor);
        event.setAction(action);
        event.setSubject(subject);
        event.setTimestamp(new Date(timestamp));
        return event;
    }

    private static List<String> ids(IkasanDocumentSearchResults results) {
        return results.getResultList().stream().map(IkasanESBDocument::getIdentifier).toList();
    }

    private static List<String> actors(SearchResults<SystemEvent> results) {
        return results.getResultList().stream().map(SystemEvent::getActor).toList();
    }

    /**
     * Create all indexes (standard and Atlas Search) with the shipped {@code apply-indexes.js} script, exactly as an
     * operator would.
     */
    private static void applyShippedIndexes() throws Exception {
        atlasContainer.copyFileToContainer(MountableFile.forClasspathResource("mongo/indexes"), "/indexes");
        Container.ExecResult result = atlasContainer.execInContainer("sh", "-c",
            "mongosh --quiet 'mongodb://localhost:27017/" + DB + "?directConnection=true' --file /indexes/apply-indexes.js");
        assertEquals("apply-indexes.js failed:\n" + result.getStdout() + result.getStderr(), 0, result.getExitCode());
    }

    /**
     * Search indexes are eventually consistent: wait until the index is built and has caught up with the inserts.
     */
    private static void awaitSearchable(MongoDatabase database, long expectedDocuments) throws InterruptedException {
        Document searchMeta = new Document("$searchMeta", new Document("index", "default")
            .append("exists", new Document("path", "type"))
            .append("count", new Document("type", "total")));

        long deadline = System.currentTimeMillis() + 120_000;
        long count = -1;
        while (System.currentTimeMillis() < deadline) {
            try {
                Document meta = database.getCollection(MongoConstants.IKASAN_COLLECTION_NAME)
                    .aggregate(List.of(searchMeta)).first();
                count = meta == null ? 0 : ((Document) meta.get("count")).get("total", Number.class).longValue();
                if (count == expectedDocuments) {
                    return;
                }
            } catch (RuntimeException e) {
                // index not ready yet
            }
            Thread.sleep(500);
        }
        fail("Search index not ready, indexed " + count + " of " + expectedDocuments + " documents");
    }
}
