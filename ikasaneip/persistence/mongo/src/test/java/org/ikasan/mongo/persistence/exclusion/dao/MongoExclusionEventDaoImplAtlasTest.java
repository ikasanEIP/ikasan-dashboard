package org.ikasan.mongo.persistence.exclusion.dao;

import org.bson.Document;
import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.exclusion.repository.MongoExclusionEventRepository;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.exclusion.ExclusionEvent;
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
 * {@link MongoExclusionEventDaoImpl#find} against Atlas Local with the shipped search index: every filter must be
 * answered by {@code $search} and return what the regex fallback would.
 */
public class MongoExclusionEventDaoImplAtlasTest {

    private static final String DB = "atlas_exclusion";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static AtlasSearchQueryHelper helper;
    private static MongoExclusionEventDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoExclusionEventDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoExclusionEventRepository.class), template, 7, helper);

        dao.save(exclusion("x1", "m1", "f1", "evt-7f3a-0001", "order 1 rejected", NOW - 3 * HOUR));
        dao.save(exclusion("x2", "m1", "f2", "evt-7f3a-0002", "order 2 rejected", NOW - 2 * HOUR));
        dao.save(exclusion("x3", "m2", "f1", "evt-9c1d-0003", "invoice 3 rejected", NOW - HOUR));
        dao.save(exclusion("x4", "m2", "f1", "evt-9c1d-0004", "order 4 rejected", NOW));
        for (int i = 0; i < 8; i++) {
            dao.save(exclusion("p" + i, "paging", "f1", "paging-" + i, "order " + i, NOW - i));
        }
        // another document type in the shared collection that matches every other filter: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "replay")
            .append("moduleName", "m1").append("flowName", "f1").append("payload", "order decoy")
            .append("event", "evt-7f3a-0001").append("errorUri", "decoy").append("timestamp", NOW - HOUR));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void search_term_matches_the_payload() {
        assertEquals(Set.of("x1", "x2", "x4"), uris(() -> dao.find(List.of("m1", "m2"), null, "ORDER", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_the_error_uri() {
        assertEquals(Set.of("x3"), uris(() -> dao.find(null, null, "x3", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_a_whole_event_identifier() {
        assertEquals(Set.of("x2"), uris(() -> dao.find(null, null, "evt-7f3a-0002", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_part_of_an_event_identifier() {
        assertEquals(Set.of("x3", "x4"), uris(() -> dao.find(null, null, "9C1D", 0, 0, -1, -1)));
    }

    @Test
    public void module_and_flow_filters() {
        assertEquals(Set.of("x1"), uris(() -> dao.find(List.of("m1"), List.of("f1"), "order", 0, 0, -1, -1)));
        assertEquals(Set.of("x2"), uris(() -> dao.find(null, List.of("f2"), "order", 0, 0, -1, -1)));
    }

    @Test
    public void time_range_filter() {
        assertEquals(Set.of("x2", "x4"),
            uris(() -> dao.find(List.of("m1", "m2"), null, "order", NOW - 150 * 60_000L, NOW, -1, -1)));
    }

    @Test
    public void no_match() {
        assertEquals(Set.of(), uris(() -> dao.find(null, null, "nonexistent", 0, 0, -1, -1)));
    }

    @Test
    public void paging_convention() {
        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.pagedListUsingSearch(DB, limit,
            () -> dao.find(List.of("paging"), null, "order", 0, 0, offset, limit))));
    }

    private static Set<String> uris(Supplier<List<ExclusionEvent>> call) {
        return AtlasLocalSupport.usingSearch(DB, call).stream()
            .map(ExclusionEvent::getErrorUri)
            .collect(Collectors.toSet());
    }

    private static MongoExclusionEvent exclusion(String errorUri, String module, String flow, String identifier,
                                                 String payload, long timestamp) {
        MongoExclusionEvent event = new MongoExclusionEvent(module, flow, identifier, payload, timestamp, errorUri);
        event.setHarvested(false);
        return event;
    }
}
