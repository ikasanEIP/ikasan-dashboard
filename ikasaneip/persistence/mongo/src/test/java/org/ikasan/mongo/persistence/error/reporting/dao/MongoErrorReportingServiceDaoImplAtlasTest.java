package org.ikasan.mongo.persistence.error.reporting.dao;

import org.bson.Document;
import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.error.reporting.repository.MongoErrorOccurrenceRepository;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoErrorReportingServiceDaoImpl#find} against Atlas Local with the shipped search index: every filter must
 * be answered by {@code $search} and return what the regex fallback would.
 */
public class MongoErrorReportingServiceDaoImplAtlasTest {

    private static final String DB = "atlas_error";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static AtlasSearchQueryHelper helper;
    private static MongoErrorReportingServiceDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoErrorReportingServiceDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoErrorOccurrenceRepository.class), template, 7, helper);

        dao.save(error("e1", "m1", "f1", "c1", "evt-7f3a-0001", "Connection refused", "java.net.ConnectException: refused",
            NOW - 3 * HOUR));
        dao.save(error("e2", "m1", "f2", "c2", "evt-7f3a-0002", "Timeout waiting for broker", "timeout detail",
            NOW - 2 * HOUR));
        dao.save(error("e3", "m2", "f1", "c1", "evt-9c1d-0003", "Invalid payload", "schema validation failed",
            NOW - HOUR));
        dao.save(error("e4", "m2", "f1", "c2", "evt-9c1d-0004", "Connection reset", "connection reset by peer", NOW));
        for (int i = 0; i < 8; i++) {
            dao.save(error("p" + i, "paging", "f1", "c1", "paging-" + i, "Connection lost " + i, "detail", NOW - i));
        }
        // another document type in the shared collection that matches every other filter: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "exclusion")
            .append("moduleName", "m1").append("flowName", "f1").append("componentName", "c1")
            .append("errorMessage", "Connection decoy").append("errorUri", "decoy").append("timestamp", NOW - HOUR));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void search_term_matches_the_error_message() {
        assertEquals(Set.of("e1", "e4"), uris(() -> dao.find(List.of("m1", "m2"), null, null, "CONNECTION", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_the_error_detail() {
        assertEquals(Set.of("e3"), uris(() -> dao.find(null, null, null, "validation", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_the_error_uri() {
        assertEquals(Set.of("e2"), uris(() -> dao.find(null, null, null, "e2", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_a_whole_event_identifier() {
        assertEquals(Set.of("e3"), uris(() -> dao.find(null, null, null, "evt-9c1d-0003", 0, 0, -1, -1)));
    }

    @Test
    public void module_flow_and_component_filters() {
        assertEquals(Set.of("e1"), uris(() -> dao.find(List.of("m1"), List.of("f1"), null, "connection", 0, 0, -1, -1)));
        assertEquals(Set.of("e4"), uris(() -> dao.find(List.of("m1", "m2"), null, List.of("c2"), "connection", 0, 0,
            -1, -1)));
    }

    @Test
    public void time_range_filter() {
        assertEquals(Set.of("e4"),
            uris(() -> dao.find(List.of("m1", "m2"), null, null, "connection", NOW - 150 * 60_000L, NOW, -1, -1)));
    }

    @Test
    public void no_match() {
        assertEquals(Set.of(), uris(() -> dao.find(null, null, null, "nonexistent", 0, 0, -1, -1)));
    }

    @Test
    public void paging_convention() {
        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.usingSearch(DB,
            () -> dao.find(List.of("paging"), null, null, "connection", 0, 0, offset, limit))));
    }

    private static Set<String> uris(Supplier<List<ErrorOccurrence>> call) {
        return AtlasLocalSupport.usingSearch(DB, call).stream()
            .map(ErrorOccurrence::getUri)
            .collect(Collectors.toSet());
    }

    private static MongoErrorOccurrence error(String uri, String module, String flow, String component,
                                              String eventId, String errorMessage, String errorDetail,
                                              long timestamp) {
        return new MongoErrorOccurrence(uri, module, flow, component, "action", errorDetail, errorMessage,
            "java.lang.Exception", eventId, null, "payload", timestamp);
    }
}
